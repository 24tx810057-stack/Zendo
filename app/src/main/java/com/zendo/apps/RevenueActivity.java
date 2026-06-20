package com.zendo.apps;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zendo.apps.databinding.ActivityRevenueBinding;
import com.zendo.apps.databinding.DialogDateRangePickerBinding;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class RevenueActivity extends AppCompatActivity {

    private ActivityRevenueBinding binding;
    private OrderViewModel orderViewModel;
    private TopProductAdapter adapter;
    private List<Product> topProducts = new ArrayList<>();
    private FirebaseFirestore db;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private final SimpleDateFormat displayDayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
    private final SimpleDateFormat sortDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private Calendar customStartCal = Calendar.getInstance();
    private Calendar customEndCal = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRevenueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        
        initViews();
        setupTabs();
        observeViewModel();
        
        TabLayout.Tab allTab = binding.tabLayoutTime.getTabAt(0);
        if (allTab != null) {
            allTab.select();
            updateDateRangeDisplay(0);
            loadData(null, null);
        }
    }

    private void initViews() {
        binding.btnBackRevenue.setOnClickListener(v -> finish());
        binding.btnSyncSoldCount.setOnClickListener(v -> syncSoldCount());

        adapter = new TopProductAdapter(this, topProducts, 0);
        binding.gvTopProducts.setAdapter(adapter);

        setupChartConfigs();
    }

    private void syncSoldCount() {
        Toast.makeText(this, "Đang đồng bộ...", Toast.LENGTH_SHORT).show();
        new OrderManager().recalculateAllProductSoldCounts(new OrderManager.OnActionCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(RevenueActivity.this, "Xong! Tải lại dữ liệu...", Toast.LENGTH_SHORT).show();
                loadData(getSelectedStartTime(), null);
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(RevenueActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTabs() {
        binding.tabLayoutTime.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 4) showCompactDateRangePicker();
                else {
                    updateDateRangeDisplay(tab.getPosition());
                    loadData(getSelectedStartTime(), null);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 4) showCompactDateRangePicker();
            }
        });

        binding.tabLayoutMain.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    binding.layoutOverview.setVisibility(View.VISIBLE);
                    binding.layoutCharts.setVisibility(View.GONE);
                } else {
                    binding.layoutOverview.setVisibility(View.GONE);
                    binding.layoutCharts.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observeViewModel() {
        orderViewModel.ordersByStatus.observe(this, orders -> {
            if (orders != null) processOrders(orders);
        });
    }

    private void loadData(Date start, Date end) {
        orderViewModel.setStatusFilter("Hoàn thành", start, end);
    }

    private void processOrders(List<Order> orders) {
        double totalRevenueNet = 0;
        double totalGrossValue = 0;
        int totalOrders = orders.size();
        
        Map<String, Double> productRevenueMap = new HashMap<>();
        Map<String, Integer> productCountMap = new HashMap<>();
        Map<String, Product> productDataMap = new HashMap<>();
        Map<String, Double> revenueBySortDay = new TreeMap<>();
        Map<String, Double> revenueByCategory = new HashMap<>();

        for (Order order : orders) {
            Date orderDate = order.getTimestamp();
            totalRevenueNet += order.getTotalAmount();

            if (orderDate != null) {
                String sortDay = sortDateFormat.format(orderDate);
                revenueBySortDay.put(sortDay, revenueBySortDay.getOrDefault(sortDay, 0.0) + order.getTotalAmount());
            }

            if (order.getItems() != null) {
                for (CartItem item : order.getItems()) {
                    String pid = item.getProductId();
                    if (pid == null) continue;
                    
                    double itemRevenue = item.getProductPrice() * item.getQuantity();
                    totalGrossValue += itemRevenue;

                    productRevenueMap.put(pid, productRevenueMap.getOrDefault(pid, 0.0) + itemRevenue);
                    productCountMap.put(pid, productCountMap.getOrDefault(pid, 0) + item.getQuantity());
                    
                    Product p = productDataMap.get(pid);
                    if (p == null) {
                        p = new Product();
                        p.setId(pid); p.setName(item.getProductName()); p.setImageUrl(item.getProductImageUrl());
                        productDataMap.put(pid, p);
                        fetchProductCategory(p, revenueByCategory, itemRevenue);
                    } else if (p.getCategory() != null) {
                        revenueByCategory.put(p.getCategory(), revenueByCategory.getOrDefault(p.getCategory(), 0.0) + itemRevenue);
                    }
                }
            }
        }

        binding.tvDetailTotalRevenue.setText(formatter.format(totalRevenueNet) + "đ");
        binding.tvDetailTotalOrders.setText(String.valueOf(totalOrders));
        binding.tvAvgOrderValue.setText(totalOrders > 0 ? formatter.format(totalRevenueNet / totalOrders) + "đ" : "0đ");

        topProducts.clear();
        for (String pid : productRevenueMap.keySet()) {
            Product p = productDataMap.get(pid);
            if (p != null) {
                p.setSoldCount(productCountMap.getOrDefault(pid, 0));
                topProducts.add(p);
            }
        }

        Collections.sort(topProducts, (p1, p2) -> Double.compare(productRevenueMap.get(p2.getId()), productRevenueMap.get(p1.getId())));
        if (topProducts.size() > 10) topProducts.subList(10, topProducts.size()).clear();

        adapter.setTotalRevenue(totalGrossValue);
        adapter.notifyDataSetChanged();

        updateLineChart(revenueBySortDay);
        updateBarChart(topProducts, productRevenueMap);
        updatePieChartCategory(revenueByCategory);
        // Health chart requires "Cancelled" orders too, skipping for ViewModel brevity or could add another observer
    }

    private void fetchProductCategory(Product p, Map<String, Double> revenueByCategory, double itemRevenue) {
        db.collection("products").document(p.getId()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String cat = doc.getString("category"); if (cat == null) cat = "Khác";
                p.setCategory(cat);
                revenueByCategory.put(cat, revenueByCategory.getOrDefault(cat, 0.0) + itemRevenue);
                updatePieChartCategory(revenueByCategory);
            }
        });
    }

    private void showCompactDateRangePicker() {
        DialogDateRangePickerBinding dialogBinding = DialogDateRangePickerBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogBinding.getRoot()).create();

        dialogBinding.tvStartDate.setText(fullDateFormat.format(customStartCal.getTime()));
        dialogBinding.tvEndDate.setText(fullDateFormat.format(customEndCal.getTime()));

        dialogBinding.btnSelectStartDate.setOnClickListener(v -> new DatePickerDialog(this, (view, y, m, d) -> {
            customStartCal.set(y, m, d); dialogBinding.tvStartDate.setText(fullDateFormat.format(customStartCal.getTime()));
        }, customStartCal.get(Calendar.YEAR), customStartCal.get(Calendar.MONTH), customStartCal.get(Calendar.DAY_OF_MONTH)).show());

        dialogBinding.btnSelectEndDate.setOnClickListener(v -> new DatePickerDialog(this, (view, y, m, d) -> {
            customEndCal.set(y, m, d); dialogBinding.tvEndDate.setText(fullDateFormat.format(customEndCal.getTime()));
        }, customEndCal.get(Calendar.YEAR), customEndCal.get(Calendar.MONTH), customEndCal.get(Calendar.DAY_OF_MONTH)).show());

        dialogBinding.btnConfirm.setOnClickListener(v -> {
            if (customStartCal.after(customEndCal)) {
                Toast.makeText(this, "Ngày không hợp lệ", Toast.LENGTH_SHORT).show(); return;
            }
            Calendar start = (Calendar) customStartCal.clone(); start.set(Calendar.HOUR_OF_DAY, 0);
            Calendar end = (Calendar) customEndCal.clone(); end.set(Calendar.HOUR_OF_DAY, 23);
            binding.tvDateRangeDisplay.setText("Từ: " + fullDateFormat.format(start.getTime()) + " - " + fullDateFormat.format(end.getTime()));
            loadData(start.getTime(), end.getTime());
            dialog.dismiss();
        });
        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateDateRangeDisplay(int position) {
        String text = "Đang hiển thị: ";
        switch (position) {
            case 0: text += "Tất cả"; break;
            case 1: text += "Hôm nay"; break;
            case 2: text += "7 ngày qua"; break;
            case 3: text += "Tháng này"; break;
        }
        binding.tvDateRangeDisplay.setText(text);
    }

    private Date getSelectedStartTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        switch (binding.tabLayoutTime.getSelectedTabPosition()) {
            case 1: return cal.getTime();
            case 2: cal.add(Calendar.DAY_OF_YEAR, -7); return cal.getTime();
            case 3: cal.set(Calendar.DAY_OF_MONTH, 1); return cal.getTime();
            default: return null;
        }
    }

    private void setupChartConfigs() {
        configureBaseChart(binding.lineChartRevenue);
        configureBaseChart(binding.barChartTopProducts);
        binding.pieChartCategory.setUsePercentValues(true);
        binding.pieChartCategory.setHoleRadius(40f);
        binding.pieChartOrderHealth.setUsePercentValues(true);
        binding.pieChartOrderHealth.setHoleRadius(40f);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureBaseChart(Chart<?> chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        if (chart instanceof BarChart || chart instanceof LineChart) {
            XAxis xAxis = chart instanceof BarChart ? ((BarChart) chart).getXAxis() : ((LineChart) chart).getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
        }
        chart.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true); return false;
        });
    }

    private void updateLineChart(Map<String, Double> revenueBySortDay) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Double> entry : revenueBySortDay.entrySet()) {
            entries.add(new Entry(i++, entry.getValue().floatValue()));
            labels.add(entry.getKey().substring(5)); // Show MM-DD
        }
        LineDataSet dataSet = new LineDataSet(entries, "Doanh thu");
        dataSet.setColor(Color.BLUE); dataSet.setCircleColor(Color.BLUE);
        binding.lineChartRevenue.setData(new LineData(dataSet));
        binding.lineChartRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.lineChartRevenue.invalidate();
    }

    private void updateBarChart(List<Product> topProducts, Map<String, Double> productRevenueMap) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < Math.min(5, topProducts.size()); i++) {
            Product p = topProducts.get(i);
            entries.add(new BarEntry(i, productRevenueMap.get(p.getId()).floatValue()));
            labels.add(p.getName().substring(0, Math.min(10, p.getName().length())));
        }
        BarDataSet dataSet = new BarDataSet(entries, "Top Sản phẩm");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        binding.barChartTopProducts.setData(new BarData(dataSet));
        binding.barChartTopProducts.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChartTopProducts.invalidate();
    }

    private void updatePieChartCategory(Map<String, Double> revenueByCategory) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : revenueByCategory.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(binding.pieChartCategory));
        binding.pieChartCategory.setData(data);
        binding.pieChartCategory.invalidate();
    }
}
