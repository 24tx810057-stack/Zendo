package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private List<Product> productList = new ArrayList<>();
    private List<Product> fullProductList = new ArrayList<>();
    private ProductAdapter adapter;
    private GridView gridViewProducts;
    private ImageView btnAddProduct, ivCartIcon;
    private View btnCart, btnNotification;
    private TextView tvCartBadge, tvNotifBadge;
    private EditText etSearch;
    private BottomNavigationView bottomNav;
    private ListenerRegistration productListener, cartListener, notificationListener;
    
    private String userEmail, userRole;
    private String currentBrand = "Tất cả";
    private String currentSort = "default";
    private TextView[] brandFilterViews;
    private TextView tvBestSelling, tvNewArrival, tvSortPriceLabel;
    private ImageView ivSortPriceIcon;
    private LinearLayout llSortPrice;
    private HorizontalScrollView hsvBrandFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");
        userRole = sharedPref.getString("user_role", "user");

        initViews();
        
        firestore = FirebaseFirestore.getInstance();
        adapter = new ProductAdapter(this, productList);
        gridViewProducts.setAdapter(adapter);

        setupBrandFilters();
        setupSortFilters();
        setupSearch();
        listenToProductChanges();
        observeBadges();
        autoCompleteOrders();

        gridViewProducts.setOnItemClickListener((parent, view, position, id) -> {
            Product selectedProduct = productList.get(position);
            Intent intent = new Intent(ListActivity.this, DetailActivity.class);
            intent.putExtra("product_data", selectedProduct);
            startActivity(intent);
        });

        btnAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
        
        btnCart.setOnClickListener(v -> {
            if ("admin".equals(userRole)) {
                startActivity(new Intent(this, OrderListActivity.class));
            } else {
                startActivity(new Intent(this, CartActivity.class));
            }
        });

        btnNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationActivity.class));
        });

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if (gridViewProducts != null) {
                    gridViewProducts.smoothScrollToPosition(0);
                }
                return true;
            }
            if (id == R.id.nav_me) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void initViews() {
        gridViewProducts = findViewById(R.id.gridViewProducts);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        btnCart = findViewById(R.id.btnCartContainer);
        ivCartIcon = findViewById(R.id.btnCart);
        btnNotification = findViewById(R.id.btnNotificationContainer);
        tvCartBadge = findViewById(R.id.tvCartBadgeList);
        tvNotifBadge = findViewById(R.id.tvNotifBadgeList);
        etSearch = findViewById(R.id.etSearch);
        bottomNav = findViewById(R.id.bottomNav);
        hsvBrandFilter = findViewById(R.id.hsvBrandFilter);

        if ("admin".equals(userRole)) {
            btnAddProduct.setVisibility(View.VISIBLE);
            btnCart.setVisibility(View.VISIBLE);
            if (ivCartIcon != null) {
                ivCartIcon.setImageResource(R.drawable.ic_receipt_admin);
            }
        } else {
            btnAddProduct.setVisibility(View.GONE);
            btnCart.setVisibility(View.VISIBLE);
            if (ivCartIcon != null) {
                ivCartIcon.setImageResource(R.drawable.ic_cart_logo);
            }
        }
    }

    private void observeBadges() {
        if (userEmail.isEmpty()) return;

        if (cartListener != null) cartListener.remove();
        if ("admin".equals(userRole)) {
            cartListener = firestore.collection("orders")
                    .whereEqualTo("status", "Chờ xác nhận")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;
                        updateBadge(tvCartBadge, value != null ? value.size() : 0);
                    });
        } else {
            cartListener = firestore.collection("cart")
                    .whereEqualTo("userEmail", userEmail)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;
                        int count = 0;
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                Long qty = doc.getLong("quantity");
                                if (qty != null) count += qty.intValue();
                            }
                        }
                        updateBadge(tvCartBadge, count);
                    });
        }

        if (notificationListener != null) notificationListener.remove();
        String targetEmail = "admin".equals(userRole) ? "admin" : userEmail;
        
        notificationListener = firestore.collection("notifications")
                .whereEqualTo("userEmail", targetEmail)
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ListActivity", "Notif Error: " + error.getMessage());
                        return;
                    }
                    updateBadge(tvNotifBadge, value != null ? value.size() : 0);
                });
    }

    private void updateBadge(TextView badge, int count) {
        if (badge == null) return;
        if (count > 0) {
            badge.setText(count > 99 ? "99+" : String.valueOf(count));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterProducts(String query) {
        String lowerCaseQuery = query.toLowerCase().trim();
        productList.clear();
        if (lowerCaseQuery.isEmpty()) {
            productList.addAll(fullProductList);
        } else {
            for (Product product : fullProductList) {
                if (product.getName().toLowerCase().contains(lowerCaseQuery) ||
                    product.getBrand().toLowerCase().contains(lowerCaseQuery) ||
                    product.getDescription().toLowerCase().contains(lowerCaseQuery)) {
                    productList.add(product);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupBrandFilters() {
        brandFilterViews = new TextView[]{
                findViewById(R.id.tvFilterAll),
                findViewById(R.id.tvFilterIphone),
                findViewById(R.id.tvFilterSamsung),
                findViewById(R.id.tvFilterOppo),
                findViewById(R.id.tvFilterXiaomi)
        };

        for (TextView tv : brandFilterViews) {
            tv.setOnClickListener(v -> {
                updateBrandFilterUI(tv);
                currentBrand = tv.getText().toString();
                listenToProductChanges();
                scrollToSelectedBrand(tv);
            });
        }
    }

    private void scrollToSelectedBrand(View view) {
        if (hsvBrandFilter != null && view != null) {
            hsvBrandFilter.post(() -> {
                int screenWidth = hsvBrandFilter.getWidth();
                int viewWidth = view.getWidth();
                int viewLeft = view.getLeft();
                int scrollX = viewLeft - (screenWidth / 2) + (viewWidth / 2);
                hsvBrandFilter.smoothScrollTo(scrollX, 0);
            });
        }
    }

    private void setupSortFilters() {
        tvBestSelling = findViewById(R.id.tvBestSelling);
        tvNewArrival = findViewById(R.id.tvNewArrival);
        tvSortPriceLabel = findViewById(R.id.tvSortPriceLabel);
        ivSortPriceIcon = findViewById(R.id.ivSortPriceIcon);
        llSortPrice = findViewById(R.id.llSortPrice);

        tvBestSelling.setOnClickListener(v -> {
            currentSort = "best_selling";
            updateSortUI();
            listenToProductChanges();
        });

        tvNewArrival.setOnClickListener(v -> {
            currentSort = "new_arrival";
            updateSortUI();
            listenToProductChanges();
        });

        llSortPrice.setOnClickListener(v -> {
            currentSort = currentSort.equals("price_asc") ? "price_desc" : "price_asc";
            updateSortUI();
            listenToProductChanges();
        });
    }

    private void updateBrandFilterUI(TextView selectedTv) {
        for (TextView tv : brandFilterViews) {
            tv.setBackgroundResource(R.drawable.bg_filter_chip);
            tv.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
        selectedTv.setBackgroundResource(R.drawable.bg_filter_chip_selected);
        selectedTv.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void updateSortUI() {
        int inactiveColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        int activeColor = ContextCompat.getColor(this, android.R.color.holo_blue_dark);

        tvBestSelling.setTextColor(inactiveColor);
        tvNewArrival.setTextColor(inactiveColor);
        tvSortPriceLabel.setTextColor(inactiveColor);
        ivSortPriceIcon.setColorFilter(inactiveColor);

        if (currentSort.equals("best_selling")) tvBestSelling.setTextColor(activeColor);
        else if (currentSort.equals("new_arrival")) tvNewArrival.setTextColor(activeColor);
        else if (currentSort.equals("price_asc")) {
            tvSortPriceLabel.setTextColor(activeColor);
            ivSortPriceIcon.setColorFilter(activeColor);
            ivSortPriceIcon.setRotation(0);
        } else if (currentSort.equals("price_desc")) {
            tvSortPriceLabel.setTextColor(activeColor);
            ivSortPriceIcon.setColorFilter(activeColor);
            ivSortPriceIcon.setRotation(180);
        }
    }

    private void listenToProductChanges() {
        if (productListener != null) productListener.remove();

        Query query = firestore.collection("products");
        if (!currentBrand.equals("Tất cả")) query = query.whereEqualTo("brand", currentBrand);

        switch (currentSort) {
            case "best_selling": query = query.orderBy("soldCount", Query.Direction.DESCENDING); break;
            case "new_arrival": query = query.orderBy("createdAt", Query.Direction.DESCENDING); break;
            case "price_asc": query = query.orderBy("price", Query.Direction.ASCENDING); break;
            case "price_desc": query = query.orderBy("price", Query.Direction.DESCENDING); break;
            default: query = query.orderBy("createdAt", Query.Direction.DESCENDING); break;
        }

        productListener = query.addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value == null) return;
            fullProductList.clear();
            for (QueryDocumentSnapshot document : value) {
                Product prod = document.toObject(Product.class);
                prod.setId(document.getId());
                
                // ẨN HÀNG HẾT TRONG KHO CHO USER
                if (!"admin".equals(userRole)) {
                    if (prod.getStock() > 0) {
                        fullProductList.add(prod);
                    }
                } else {
                    // Admin thấy tất cả
                    fullProductList.add(prod);
                }
            }
            filterProducts(etSearch.getText().toString());
        });
    }

    private void autoCompleteOrders() {
        // Tìm các đơn hàng cần hoàn thành hoặc sửa lại ngày
        long threeDaysInMillis = 3L * 24 * 60 * 60 * 1000;
        long oneDayInMillis = 24L * 60 * 60 * 1000;
        long currentTime = System.currentTimeMillis();

        firestore.collection("orders").get().addOnSuccessListener(queryDocumentSnapshots -> {
            OrderManager orderManager = new OrderManager();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Order order = doc.toObject(Order.class);
                if (order.getTimestamp() == null) continue;
                
                String status = order.getStatus();
                long orderTime = order.getTimestamp().getTime();

                // 1. TỰ ĐỘNG HOÀN THÀNH: Đơn "Đã giao" quá 3 ngày
                if ("Đã giao".equals(status) && (currentTime - orderTime > threeDaysInMillis)) {
                    orderManager.updateOrderStatus(doc.getId(), "Hoàn thành", new OrderManager.OnActionCompleteListener() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(String error) {}
                    });
                }
                
                // 2. SỬA LẠI NGÀY (REPAIR): Nếu đơn cũ (> 3 ngày) mà ngày nhận lại là "mới đây" (do lỗi logic cũ)
                if ("Hoàn thành".equals(status) && (currentTime - orderTime > threeDaysInMillis)) {
                    long deliveryTime = order.getDeliveryDate() != null ? order.getDeliveryDate().getTime() : 0;
                    // Nếu ngày nhận nằm trong vòng 24h qua -> Chắc chắn là do bị ép sai ngày
                    if (deliveryTime > (currentTime - oneDayInMillis)) {
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTime(order.getTimestamp());
                        cal.add(java.util.Calendar.DAY_OF_YEAR, 3);
                        firestore.collection("orders").document(doc.getId()).update("deliveryDate", cal.getTime());
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        observeBadges();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productListener != null) productListener.remove();
        if (cartListener != null) cartListener.remove();
        if (notificationListener != null) notificationListener.remove();
    }
}
