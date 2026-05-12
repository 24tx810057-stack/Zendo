package com.example.buoi1;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

    private TextView tvTotalRevenue, tvTotalOrders, tvAvgValue, tvDateRangeDisplay;
    private NonScrollGridView gvTopProducts;
    private TopProductAdapter adapter;
    private List<Product> topProducts = new ArrayList<>();
    private FirebaseFirestore db;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private TabLayout tabLayoutTime, tabLayoutMain;
    private LinearLayout layoutOverview, layoutCharts;
    private LineChart lineChartRevenue;
    private BarChart barChartTopProducts;
    private PieChart pieChartCategory, pieChartOrderHealth;
    
    private double totalItemsValueForPercent = 0; 
    private OrderManager orderManager;
    private final SimpleDateFormat displayDayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
    private final SimpleDateFormat sortDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private Calendar customStartCal = Calendar.getInstance();
    private Calendar customEndCal = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue);

        db = FirebaseFirestore.getInstance();
        orderManager = new OrderManager();
        initViews();
        setupTabs();
        
        if (tabLayoutTime != null) {
            TabLayout.Tab allTab = tabLayoutTime.getTabAt(0);
            if (allTab != null) {
                allTab.select();
                updateDateRangeDisplay(0);
                loadRevenueData(null, null);
            }
        }
    }

    private void initViews() {
        tvTotalRevenue = findViewById(R.id.tvDetailTotalRevenue);
        tvTotalOrders = findViewById(R.id.tvDetailTotalOrders);
        tvAvgValue = findViewById(R.id.tvAvgOrderValue);
        tvDateRangeDisplay = findViewById(R.id.tvDateRangeDisplay);
        gvTopProducts = findViewById(R.id.gvTopProducts);
        tabLayoutTime = findViewById(R.id.tabLayoutTime);
        tabLayoutMain = findViewById(R.id.tabLayoutMain);
        layoutOverview = findViewById(R.id.layoutOverview);
        layoutCharts = findViewById(R.id.layoutCharts);
        lineChartRevenue = findViewById(R.id.lineChartRevenue);
        barChartTopProducts = findViewById(R.id.barChartTopProducts);
        pieChartCategory = findViewById(R.id.pieChartCategory);
        pieChartOrderHealth = findViewById(R.id.pieChartOrderHealth);

        ImageView btnBack = findViewById(R.id.btnBackRevenue);
        ImageView btnSync = findViewById(R.id.btnSyncSoldCount);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        
        if (btnSync != null) {
            btnSync.setOnClickListener(v -> {
                Toast.makeText(this, "Đang đồng bộ lại lượt bán...", Toast.LENGTH_SHORT).show();
                orderManager.recalculateAllProductSoldCounts(new OrderManager.OnActionCompleteListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(RevenueActivity.this, "Đồng bộ thành công! Vui lòng tải lại trang.", Toast.LENGTH_LONG).show();
                        loadRevenueData(getSelectedStartTime(), null);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(RevenueActivity.this, "Lỗi đồng bộ: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        adapter = new TopProductAdapter(this, topProducts, 0);
        if (gvTopProducts != null) gvTopProducts.setAdapter(adapter);

        setupChartConfigs();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTabs() {
        tabLayoutTime.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 4) {
                    showCompactDateRangePicker();
                } else {
                    updateDateRangeDisplay(tab.getPosition());
                    loadRevenueData(getSelectedStartTime(), null);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 4) {
                    showCompactDateRangePicker();
                }
            }
        });

        tabLayoutMain.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutOverview.setVisibility(View.VISIBLE);
                    layoutCharts.setVisibility(View.GONE);
                } else {
                    layoutOverview.setVisibility(View.GONE);
                    layoutCharts.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showCompactDateRangePicker() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_date_range_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        TextView tvStartDate = dialogView.findViewById(R.id.tvStartDate);
        TextView tvEndDate = dialogView.findViewById(R.id.tvEndDate);
        View btnSelectStart = dialogView.findViewById(R.id.btnSelectStartDate);
        View btnSelectEnd = dialogView.findViewById(R.id.btnSelectEndDate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        tvStartDate.setText(fullDateFormat.format(customStartCal.getTime()));
        tvEndDate.setText(fullDateFormat.format(customEndCal.getTime()));

        btnSelectStart.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                customStartCal.set(year, month, dayOfMonth);
                tvStartDate.setText(fullDateFormat.format(customStartCal.getTime()));
            }, customStartCal.get(Calendar.YEAR), customStartCal.get(Calendar.MONTH), customStartCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSelectEnd.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                customEndCal.set(year, month, dayOfMonth);
                tvEndDate.setText(fullDateFormat.format(customEndCal.getTime()));
            }, customEndCal.get(Calendar.YEAR), customEndCal.get(Calendar.MONTH), customEndCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            if (customStartCal.after(customEndCal)) {
                Toast.makeText(this, "Ngày bắt đầu phải trước ngày kết thúc", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Calendar start = (Calendar) customStartCal.clone();
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);

            Calendar end = (Calendar) customEndCal.clone();
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);

            tvDateRangeDisplay.setText("Đang hiển thị: " + fullDateFormat.format(start.getTime()) + " - " + fullDateFormat.format(end.getTime()));
            loadRevenueData(start.getTime(), end.getTime());
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateDateRangeDisplay(int position) {
        String text = "Đang hiển thị: ";
        switch (position) {
            case 0: text += "Tất cả thời gian"; break;
            case 1: text += "Hôm nay (" + fullDateFormat.format(new Date()) + ")"; break;
            case 2: text += "7 ngày qua"; break;
            case 3: text += "Tháng này"; break;
        }
        tvDateRangeDisplay.setText(text);
    }

    private Date getSelectedStartTime() {
        switch (tabLayoutTime.getSelectedTabPosition()) {
            case 1: return getStartTimeOfDay();
            case 2: return getStartTimeOfDaysAgo(7);
            case 3: return getStartTimeOfMonth();
            default: return null;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupChartConfigs() {
        configureBaseChart(lineChartRevenue);
        lineChartRevenue.setPinchZoom(true);

        configureBaseChart(barChartTopProducts);
        barChartTopProducts.getLegend().setEnabled(false);

        pieChartCategory.getDescription().setEnabled(false);
        pieChartCategory.setUsePercentValues(true);
        pieChartCategory.setDrawHoleEnabled(true);
        pieChartCategory.setHoleRadius(40f);
        pieChartCategory.getLegend().setEnabled(false);

        pieChartOrderHealth.getDescription().setEnabled(false);
        pieChartOrderHealth.setUsePercentValues(true);
        pieChartOrderHealth.setDrawHoleEnabled(true);
        pieChartOrderHealth.setHoleRadius(40f);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureBaseChart(Chart<?> chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setExtraOffsets(5, 5, 5, 15);
        
        if (chart instanceof BarChart || chart instanceof LineChart) {
            XAxis xAxis;
            if (chart instanceof BarChart) {
                BarChart bc = (BarChart) chart;
                bc.setDragEnabled(true);
                bc.setScaleXEnabled(true);
                bc.setScaleYEnabled(false);
                bc.setDoubleTapToZoomEnabled(false);
                xAxis = bc.getXAxis();
                bc.getAxisRight().setEnabled(false);
            } else {
                LineChart lc = (LineChart) chart;
                lc.setDragEnabled(true);
                lc.setScaleXEnabled(true);
                lc.setScaleYEnabled(false);
                lc.setDoubleTapToZoomEnabled(false);
                xAxis = lc.getXAxis();
                lc.getAxisRight().setEnabled(false);
            }
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(7);
        }

        // Khắc phục xung đột với NestedScrollView
        chart.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
    }

    private void loadRevenueData(Date startTime, Date endTime) {
        tvTotalRevenue.setText("0đ");
        tvTotalOrders.setText("0");
        tvAvgValue.setText("0đ");
        topProducts.clear();
        totalItemsValueForPercent = 0;
        if (adapter != null) {
            adapter.setTotalRevenue(0);
            adapter.notifyDataSetChanged();
        }

        db.collection("orders")
            .whereEqualTo("status", "Hoàn thành")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                double totalRevenueNet = 0;
                double totalGrossValue = 0;
                int totalOrders = 0;
                
                Map<String, Double> productRevenueMap = new HashMap<>();
                Map<String, Integer> productCountMap = new HashMap<>();
                Map<String, Product> productDataMap = new HashMap<>();
                Map<String, Double> revenueBySortDay = new TreeMap<>();
                Map<String, Double> revenueByCategory = new HashMap<>();

                // TỰ ĐỘNG ĐIỀN CÁC NGÀY TRỐNG TRONG KHOẢNG ĐÃ CHỌN (Để biểu đồ không bị "cụt")
                if (startTime != null && endTime != null) {
                    Calendar tempCal = Calendar.getInstance();
                    tempCal.setTime(startTime);
                    while (!tempCal.getTime().after(endTime)) {
                        revenueBySortDay.put(sortDateFormat.format(tempCal.getTime()), 0.0);
                        tempCal.add(Calendar.DAY_OF_YEAR, 1);
                    }
                } else if (startTime != null) {
                    // Cho trường hợp 7 ngày hoặc Tháng này
                    Calendar tempCal = Calendar.getInstance();
                    tempCal.setTime(startTime);
                    Date now = new Date();
                    while (!tempCal.getTime().after(now)) {
                        revenueBySortDay.put(sortDateFormat.format(tempCal.getTime()), 0.0);
                        tempCal.add(Calendar.DAY_OF_YEAR, 1);
                    }
                }

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Order order = doc.toObject(Order.class);
                    Date orderDate = order.getTimestamp();

                    if (startTime != null && (orderDate == null || orderDate.before(startTime))) {
                        continue;
                    }
                    if (endTime != null && (orderDate == null || orderDate.after(endTime))) {
                        continue;
                    }

                    totalRevenueNet += order.getTotalAmount();
                    totalOrders++;

                    if (orderDate != null) {
                        String sortDay = sortDateFormat.format(orderDate);
                        Double current = revenueBySortDay.get(sortDay);
                        revenueBySortDay.put(sortDay, (current != null ? current : 0.0) + order.getTotalAmount());
                    }

                    if (order.getItems() != null) {
                        for (CartItem item : order.getItems()) {
                            String pid = item.getProductId();
                            if (pid == null) continue;
                            
                            double itemRevenue = item.getProductPrice() * item.getQuantity();
                            totalGrossValue += itemRevenue;

                            productRevenueMap.put(pid, productRevenueMap.getOrDefault(pid, 0.0) + itemRevenue);
                            productCountMap.put(pid, productCountMap.getOrDefault(pid, 0) + item.getQuantity());
                            
                            Product existingProductData = productDataMap.get(pid);
                            if (existingProductData == null) {
                                Product p = new Product();
                                p.setId(pid);
                                p.setName(item.getProductName());
                                p.setImageUrl(item.getProductImageUrl());
                                productDataMap.put(pid, p);
                                
                                fetchProductCategory(p, revenueByCategory, itemRevenue);
                            } else {
                                String category = existingProductData.getCategory();
                                if (category != null) {
                                    Double currentCatRevenue = revenueByCategory.get(category);
                                    revenueByCategory.put(category, (currentCatRevenue != null ? currentCatRevenue : 0.0) + itemRevenue);
                                }
                            }
                        }
                    }
                }

                totalItemsValueForPercent = totalGrossValue;
                tvTotalRevenue.setText(formatter.format(totalRevenueNet) + "đ");
                tvTotalOrders.setText(String.valueOf(totalOrders));
                tvAvgValue.setText(totalOrders > 0 ? formatter.format(totalRevenueNet / totalOrders) + "đ" : "0đ");

                topProducts.clear();
                for (String pid : productRevenueMap.keySet()) {
                    Product p = productDataMap.get(pid);
                    if (p != null) {
                        int soldCount = productCountMap.getOrDefault(pid, 0);
                        p.setSoldCount(soldCount);
                        // Revenue thật sự của sản phẩm này
                        double totalProductRevenue = productRevenueMap.getOrDefault(pid, 0.0);
                        p.setPrice(soldCount > 0 ? totalProductRevenue / soldCount : 0); 
                        topProducts.add(p);
                    }
                }

                // Sắp xếp theo TỔNG DOANH THU của từng sản phẩm (Giảm dần)
                Collections.sort(topProducts, (p1, p2) -> {
                    double r1 = productRevenueMap.getOrDefault(p1.getId(), 0.0);
                    double r2 = productRevenueMap.getOrDefault(p2.getId(), 0.0);
                    return Double.compare(r2, r1);
                });

                if (topProducts.size() > 10) {
                    topProducts.subList(10, topProducts.size()).clear();
                }

                if (adapter != null) {
                    adapter.setTotalRevenue(totalItemsValueForPercent);
                    adapter.notifyDataSetChanged();
                }

                updateLineChart(revenueBySortDay);
                updateBarChart(topProducts);
                updatePieChartCategory(revenueByCategory);
                updateOrderHealthChart(totalOrders, startTime, endTime);
            });
    }

    private void fetchProductCategory(Product p, Map<String, Double> revenueByCategory, double itemRevenue) {
        db.collection("products").document(p.getId()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String category = documentSnapshot.getString("category");
                if (category == null) category = "Khác";
                p.setCategory(category);
                Double currentCatRevenue = revenueByCategory.get(category);
                revenueByCategory.put(category, (currentCatRevenue != null ? currentCatRevenue : 0.0) + itemRevenue);
                updatePieChartCategory(revenueByCategory);
            }
        });
    }

    private void updateLineChart(Map<String, Double> revenueBySortDay) {
        if (lineChartRevenue == null) return;
        
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        
        if (revenueBySortDay.isEmpty()) {
            lineChartRevenue.clear();
            lineChartRevenue.setNoDataText("Không có dữ liệu trong khoảng thời gian này");
            lineChartRevenue.invalidate();
            return;
        }

        try {
            for (Map.Entry<String, Double> entry : revenueBySortDay.entrySet()) {
                entries.add(new Entry(i, entry.getValue().floatValue()));
                Date date = sortDateFormat.parse(entry.getKey());
                labels.add(date != null ? displayDayFormat.format(date) : entry.getKey());
                i++;
            }
        } catch (Exception e) {
            Log.e("RevenueActivity", "Error parsing date: " + e.getMessage());
        }

        LineDataSet dataSet = new LineDataSet(entries, "Số tiền (đ)");
        dataSet.setColor(Color.parseColor("#01579B"));
        dataSet.setCircleColor(Color.parseColor("#01579B"));
        dataSet.setLineWidth(2.5f); // Tăng độ dày đường kẻ
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(8f); 
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(40);
        dataSet.setFillColor(Color.parseColor("#01579B"));
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER); // Làm mượt đường cong
        dataSet.setDrawValues(true); 
        
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000000) return String.format("%.1fM", value / 1000000);
                if (value >= 1000) return String.format("%.0fK", value / 1000);
                return String.format("%.0f", value);
            }
        });

        LineData lineData = new LineData(dataSet);
        lineChartRevenue.setData(lineData);
        lineChartRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        
        if (entries.size() > 7) {
            lineChartRevenue.setVisibleXRangeMaximum(7);
            lineChartRevenue.moveViewToX(entries.size() - 7);
        } else {
            lineChartRevenue.setVisibleXRangeMaximum(entries.size());
        }
        
        lineChartRevenue.invalidate();
    }

    private void updateBarChart(List<Product> topProducts) {
        if (barChartTopProducts == null) return;

        if (topProducts.isEmpty()) {
            barChartTopProducts.clear();
            barChartTopProducts.setNoDataText("Không có dữ liệu sản phẩm");
            barChartTopProducts.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (int i = 0; i < Math.min(5, topProducts.size()); i++) {
            Product p = topProducts.get(i);
            entries.add(new BarEntry(i, (float) (p.getPrice() * p.getSoldCount())));
            String shortName = p.getName();
            if (shortName.length() > 10) shortName = shortName.substring(0, 10) + "...";
            labels.add(shortName);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu (đ)");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(10f);
        dataSet.setBarShadowColor(Color.parseColor("#F5F5F5"));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.4f); // Giảm thêm độ rộng cột cho gọn

        barChartTopProducts.setData(barData);
        barChartTopProducts.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartTopProducts.getXAxis().setLabelRotationAngle(-45);
        barChartTopProducts.getXAxis().setGranularity(1f); // Ép hiển thị từng nhãn một
        barChartTopProducts.getXAxis().setLabelCount(labels.size()); // Hiển thị đủ số nhãn sản phẩm
        
        if (entries.size() > 5) {
            barChartTopProducts.setVisibleXRangeMaximum(5);
        } else {
            barChartTopProducts.setVisibleXRangeMaximum(5); // Luôn ép khung hình tối thiểu 5 cột để không bị nở to
        }

        barChartTopProducts.invalidate();
    }

    private void updatePieChartCategory(Map<String, Double> revenueByCategory) {
        if (pieChartCategory == null) return;

        if (revenueByCategory.isEmpty()) {
            pieChartCategory.clear();
            pieChartCategory.setNoDataText("Không có dữ liệu danh mục");
            pieChartCategory.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : revenueByCategory.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChartCategory));
        pieChartCategory.setData(pieData);
        pieChartCategory.invalidate();
    }

    private void updateOrderHealthChart(int completedCount, Date startTime, Date endTime) {
        db.collection("orders")
            .whereEqualTo("status", "Đã hủy")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int cancelledCount = 0;
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Order order = doc.toObject(Order.class);
                    if (startTime != null && (order.getTimestamp() == null || order.getTimestamp().before(startTime))) {
                        continue;
                    }
                    if (endTime != null && order.getTimestamp() != null && order.getTimestamp().after(endTime)) {
                        continue;
                    }
                    cancelledCount++;
                }

                List<PieEntry> finalEntries = new ArrayList<>();
                if (completedCount > 0) finalEntries.add(new PieEntry(completedCount, "Thành công"));
                if (cancelledCount > 0) finalEntries.add(new PieEntry(cancelledCount, "Đã hủy"));

                PieDataSet dataSet = new PieDataSet(finalEntries, "");
                dataSet.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#F44336"));
                dataSet.setValueTextColor(Color.WHITE);
                dataSet.setValueTextSize(12f);

                PieData pieData = new PieData(dataSet);
                pieData.setValueFormatter(new PercentFormatter(pieChartOrderHealth));
                pieChartOrderHealth.setData(pieData);
                pieChartOrderHealth.invalidate();
            });
    }

    private Date getStartTimeOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getStartTimeOfDaysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getStartTimeOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
