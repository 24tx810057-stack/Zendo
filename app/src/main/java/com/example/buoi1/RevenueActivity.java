package com.example.buoi1;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RevenueActivity extends AppCompatActivity {

    private TextView tvTotalRevenue, tvTotalOrders, tvAvgValue;
    private NonScrollGridView gvTopProducts;
    private TopProductAdapter adapter;
    private List<Product> topProducts = new ArrayList<>();
    private FirebaseFirestore db;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private TabLayout tabLayout;
    private double totalItemsValueForPercent = 0; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupTabs();
        
        if (tabLayout != null) {
            TabLayout.Tab monthTab = tabLayout.getTabAt(0); // Mặc định về Tất cả để test dữ liệu
            if (monthTab != null) {
                monthTab.select();
                loadRevenueData(null);
            }
        }
    }

    private void initViews() {
        tvTotalRevenue = findViewById(R.id.tvDetailTotalRevenue);
        tvTotalOrders = findViewById(R.id.tvDetailTotalOrders);
        tvAvgValue = findViewById(R.id.tvAvgOrderValue);
        gvTopProducts = findViewById(R.id.gvTopProducts);
        tabLayout = findViewById(R.id.tabLayoutTime);
        ImageView btnBack = findViewById(R.id.btnBackRevenue);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        adapter = new TopProductAdapter(this, topProducts, 0);
        if (gvTopProducts != null) gvTopProducts.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: loadRevenueData(null); break;
                    case 1: loadRevenueData(getStartTimeOfDay()); break;
                    case 2: loadRevenueData(getStartTimeOfDaysAgo(7)); break;
                    case 3: loadRevenueData(getStartTimeOfMonth()); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadRevenueData(Date startTime) {
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
            .whereEqualTo("status", "Đã giao")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                double totalRevenueNet = 0; // Tiền thực nhận
                double totalGrossValue = 0; // Tổng giá trị hàng (để chia %)
                int totalOrders = 0;
                
                // Map lưu Doanh thu và Số lượng của từng sản phẩm
                Map<String, Double> productRevenueMap = new HashMap<>();
                Map<String, Integer> productCountMap = new HashMap<>();
                Map<String, Product> productDataMap = new HashMap<>();

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Order order = doc.toObject(Order.class);
                    
                    if (startTime != null && (order.getTimestamp() == null || order.getTimestamp().before(startTime))) {
                        continue;
                    }

                    totalRevenueNet += order.getTotalAmount();
                    totalOrders++;

                    if (order.getItems() != null) {
                        for (CartItem item : order.getItems()) {
                            String pid = item.getProductId();
                            if (pid == null) continue;
                            
                            double itemRevenue = item.getProductPrice() * item.getQuantity();
                            totalGrossValue += itemRevenue;

                            productRevenueMap.put(pid, productRevenueMap.getOrDefault(pid, 0.0) + itemRevenue);
                            productCountMap.put(pid, productCountMap.getOrDefault(pid, 0) + item.getQuantity());
                            
                            if (!productDataMap.containsKey(pid)) {
                                Product p = new Product();
                                p.setId(pid);
                                p.setName(item.getProductName());
                                p.setImageUrl(item.getProductImageUrl());
                                productDataMap.put(pid, p);
                            }
                        }
                    }
                }

                totalItemsValueForPercent = totalGrossValue;
                tvTotalRevenue.setText(formatter.format(totalRevenueNet) + "đ");
                tvTotalOrders.setText(String.valueOf(totalOrders));
                tvAvgValue.setText(totalOrders > 0 ? formatter.format(totalRevenueNet / totalOrders) + "đ" : "0đ");

                // Chuyển dữ liệu vào List để hiển thị
                topProducts.clear();
                for (String pid : productRevenueMap.keySet()) {
                    Product p = productDataMap.get(pid);
                    p.setSoldCount(productCountMap.get(pid));
                    // Lưu doanh thu vào price để adapter lấy hiển thị (hoặc dùng field chuyên dụng)
                    p.setPrice(productRevenueMap.get(pid) / p.getSoldCount()); 
                    topProducts.add(p);
                }

                // Sắp xếp theo doanh thu giảm dần
                Collections.sort(topProducts, (p1, p2) -> {
                    double r1 = p1.getPrice() * p1.getSoldCount();
                    double r2 = p2.getPrice() * p2.getSoldCount();
                    return Double.compare(r2, r1);
                });

                if (topProducts.size() > 10) {
                    topProducts.subList(10, topProducts.size()).clear();
                }

                if (adapter != null) {
                    adapter.setTotalRevenue(totalItemsValueForPercent);
                    adapter.notifyDataSetChanged();
                }
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
