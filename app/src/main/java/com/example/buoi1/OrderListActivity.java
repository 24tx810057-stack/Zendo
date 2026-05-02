package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class OrderListActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ListView lvOrders;
    private TextView tvTitle;
    private List<Order> allOrders = new ArrayList<>();
    private List<Order> filteredOrders = new ArrayList<>();
    private OrderAdapter adapter;
    private FirebaseFirestore db;
    private String userEmail, userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");
        userRole = sharedPref.getString("user_role", "user");

        initViews();
        setupTabs();
        
        // Đổi tiêu đề dựa trên Role
        if ("admin".equals(userRole)) {
            tvTitle.setText("Quản lý đơn hàng");
        } else {
            tvTitle.setText("Đơn mua của tôi");
        }
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayoutOrders);
        lvOrders = findViewById(R.id.lvOrderList);
        tvTitle = findViewById(R.id.tvOrderListTitle);
        findViewById(R.id.btnBackOrderList).setOnClickListener(v -> finish());

        adapter = new OrderAdapter(this, filteredOrders);
        lvOrders.setAdapter(adapter);

        lvOrders.setOnItemClickListener((parent, view, position, id) -> {
            try {
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra("order_data", filteredOrders.get(position));
                startActivity(intent);
            } catch (Exception e) {
                Log.e("OrderList", "Error opening detail: " + e.getMessage());
                Toast.makeText(this, "Không thể mở chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        tabLayout.addTab(tabLayout.newTab().setText("Chờ xác nhận"));
        tabLayout.addTab(tabLayout.newTab().setText("Chờ lấy hàng"));
        tabLayout.addTab(tabLayout.newTab().setText("Đang giao"));
        tabLayout.addTab(tabLayout.newTab().setText("Hoàn thành"));
        tabLayout.addTab(tabLayout.newTab().setText("Trả hàng"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã hủy"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterOrders(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        Query query = db.collection("orders");
        // USER thì lọc theo Email, ADMIN thì lấy
        if (!"admin".equals(userRole)) {
            query = query.whereEqualTo("userEmail", userEmail);
        }
        
        query.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allOrders.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                order.setId(doc.getId());
                                allOrders.add(order);
                            }
                        } catch (Exception e) {
                            Log.e("OrderList", "Lỗi đọc đơn hàng " + doc.getId() + ": " + e.getMessage());
                        }
                    }
                    filterOrders(tabLayout.getSelectedTabPosition());
                })
                .addOnFailureListener(e -> {
                    Log.e("OrderList", "Firebase error: " + e.getMessage());
                    Toast.makeText(this, "Lỗi tải đơn hàng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void filterOrders(int tabPosition) {
        filteredOrders.clear();
        
        if (tabPosition == 0) { // Tất cả
            filteredOrders.addAll(allOrders);
        } else {
            for (Order order : allOrders) {
                String status = order.getStatus();
                if (status == null) continue;

                boolean match = false;
                switch (tabPosition) {
                    case 1: match = status.equals("Chờ xác nhận"); break;
                    case 2: match = status.equals("Chờ lấy hàng"); break;
                    case 3: // Gom Đang giao và Đã giao vào 1 Tab
                        match = status.equals("Đang giao") || status.equals("Đã giao"); 
                        break;
                    case 4: match = status.equals("Hoàn thành"); break;
                    case 5: // Tab Trả hàng
                        match = status.equals("Yêu cầu trả hàng") || status.equals("Đã hoàn tiền");
                        break;
                    case 6: match = status.equals("Đã hủy"); break;
                }
                if (match) filteredOrders.add(order);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
