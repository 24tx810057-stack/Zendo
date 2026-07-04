package com.zendo.apps.ui.activities;

import com.zendo.apps.data.OrderManager;

import com.zendo.apps.viewmodels.ProductViewModel;

import com.zendo.apps.R;

import com.zendo.apps.utils.SharedPrefManager;

import com.zendo.apps.ui.adapters.ProductAdapter;

import com.zendo.apps.data.models.CartItem;

import com.zendo.apps.data.models.Product;

import com.zendo.apps.data.models.Order;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zendo.apps.databinding.ActivityListBinding;

import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    private ActivityListBinding binding;
    private ProductViewModel viewModel;
    private List<Product> productList = new ArrayList<>();
    private List<Product> fullProductList = new ArrayList<>();
    private ProductAdapter adapter;
    
    private String userEmail, userRole;
    private String currentBrand = "Tất cả";
    private String currentSort = "default";
    private TextView[] brandFilterViews;
    private ListenerRegistration cartListener, notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        userEmail = prefManager.getUserEmail();
        userRole = prefManager.getUserRole();

        initViews();
        
        adapter = new ProductAdapter(this, productList);
        binding.gridViewProducts.setAdapter(adapter);

        currentSort = "new_arrival";
        updateSortUI();
        viewModel.setFilter(currentBrand, currentSort);
        setupBrandFilters();
        setupSortFilters();
        setupSearch();
        observeViewModel();
        observeBadges();
        autoCompleteOrders();

        binding.gridViewProducts.setOnItemClickListener((parent, view, position, id) -> {
            Product selectedProduct = productList.get(position);
            Intent intent = new Intent(ListActivity.this, DetailActivity.class);
            intent.putExtra("product_data", selectedProduct);
            startActivity(intent);
        });

        binding.btnAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
        
        binding.btnCartContainer.setOnClickListener(v -> {
            if ("admin".equals(userRole)) {
                startActivity(new Intent(this, OrderListActivity.class));
            } else {
                startActivity(new Intent(this, CartActivity.class));
            }
        });

        binding.btnNotificationContainer.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationActivity.class));
        });

        binding.fabHomeChat.setOnClickListener(v -> {
            ChatBottomSheetFragment chatSheet = ChatBottomSheetFragment.newInstance(
                    "general",
                    "Phone Store",
                    true
            );
            chatSheet.show(getSupportFragmentManager(), "ChatBottomSheet");
        });

        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                binding.gridViewProducts.smoothScrollToPosition(0);
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
        if ("admin".equals(userRole)) {
            binding.btnAddProduct.setVisibility(View.VISIBLE);
            binding.btnCartContainer.setVisibility(View.VISIBLE);
            binding.btnCart.setImageResource(R.drawable.ic_receipt_admin);
        } else {
            binding.btnAddProduct.setVisibility(View.GONE);
            binding.btnCartContainer.setVisibility(View.VISIBLE);
            binding.btnCart.setImageResource(R.drawable.ic_cart_logo);
        }
    }

    private void observeViewModel() {
        binding.shimmerViewContainer.startShimmer();
        binding.shimmerViewContainer.setVisibility(View.VISIBLE);
        binding.gridViewProducts.setVisibility(View.GONE);

        viewModel.products.observe(this, products -> {
            if (products != null) {
                fullProductList.clear();
                for (Product prod : products) {
                    if (!"admin".equals(userRole)) {
                        if (prod.getStock() > 0) fullProductList.add(prod);
                    } else fullProductList.add(prod);
                }
                filterProducts(binding.etSearch.getText().toString());
                
                binding.shimmerViewContainer.stopShimmer();
                binding.shimmerViewContainer.setVisibility(View.GONE);
                binding.gridViewProducts.setVisibility(View.VISIBLE);
            }
        });
    }

    private void observeBadges() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        if (userEmail.isEmpty()) return;

        if (cartListener != null) cartListener.remove();
        if ("admin".equals(userRole)) {
            cartListener = firestore.collection("orders")
                    .whereEqualTo("status", "Chờ xác nhận")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;
                        updateBadge(binding.tvCartBadgeList, value != null ? value.size() : 0);
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
                        updateBadge(binding.tvCartBadgeList, count);
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
                    int count = value != null ? value.size() : 0;
                    updateBadge(binding.tvNotifBadgeList, count);
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
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {
                saveSearchQuery(s.toString());
            }
        });
    }

    private void saveSearchQuery(String query) {
        String q = query.trim().toLowerCase();
        if (q.length() < 2) return;

        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String history = prefManager.getSearchHistory();
        
        List<String> queries = new ArrayList<>();
        if (!history.isEmpty()) {
            queries.addAll(java.util.Arrays.asList(history.split(",")));
        }
        
        if (!queries.contains(q)) {
            queries.add(0, q);
            if (queries.size() > 5) {
                queries = queries.subList(0, 5);
            }
            prefManager.saveSearchHistory(android.text.TextUtils.join(",", queries));
        }
    }

    private void filterProducts(String query) {
        String lowerCaseQuery = query.toLowerCase().trim();
        productList.clear();
        if (lowerCaseQuery.isEmpty()) {
            productList.addAll(fullProductList);
        } else {
            for (Product product : fullProductList) {
                boolean matchesBasic = product.getName().toLowerCase().contains(lowerCaseQuery) ||
                                      product.getBrand().toLowerCase().contains(lowerCaseQuery) ||
                                      product.getDescription().toLowerCase().contains(lowerCaseQuery);

                boolean matchesTag = false;
                if (product.getTags() != null) {
                    for (String tag : product.getTags()) {
                        if (tag.toLowerCase().contains(lowerCaseQuery)) {
                            matchesTag = true;
                            break;
                        }
                    }
                }

                if (matchesBasic || matchesTag) {
                    productList.add(product);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupBrandFilters() {
        brandFilterViews = new TextView[]{
                binding.tvFilterAll,
                binding.tvFilterIphone,
                binding.tvFilterSamsung,
                binding.tvFilterOppo,
                binding.tvFilterXiaomi
        };

        for (TextView tv : brandFilterViews) {
            tv.setOnClickListener(v -> {
                updateBrandFilterUI(tv);
                currentBrand = tv.getText().toString();
                viewModel.setFilter(currentBrand, currentSort);
                scrollToSelectedBrand(tv);
            });
        }
    }

    private void scrollToSelectedBrand(View view) {
        binding.hsvBrandFilter.post(() -> {
            int screenWidth = binding.hsvBrandFilter.getWidth();
            int viewWidth = view.getWidth();
            int viewLeft = view.getLeft();
            int scrollX = viewLeft - (screenWidth / 2) + (viewWidth / 2);
            binding.hsvBrandFilter.smoothScrollTo(scrollX, 0);
        });
    }

    private void setupSortFilters() {
        binding.tvBestSelling.setOnClickListener(v -> {
            currentSort = "best_selling";
            updateSortUI();
            viewModel.setFilter(currentBrand, currentSort);
        });

        binding.tvNewArrival.setOnClickListener(v -> {
            currentSort = "new_arrival";
            updateSortUI();
            viewModel.setFilter(currentBrand, currentSort);
        });

        binding.tvDiscountSort.setOnClickListener(v -> {
            currentSort = "discount";
            updateSortUI();
            viewModel.setFilter(currentBrand, currentSort);
        });

        binding.llSortPrice.setOnClickListener(v -> {
            currentSort = currentSort.equals("price_asc") ? "price_desc" : "price_asc";
            updateSortUI();
            viewModel.setFilter(currentBrand, currentSort);
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

        binding.tvBestSelling.setTextColor(inactiveColor);
        binding.tvNewArrival.setTextColor(inactiveColor);
        binding.tvDiscountSort.setTextColor(inactiveColor);
        binding.tvSortPriceLabel.setTextColor(inactiveColor);
        binding.ivSortPriceIcon.setColorFilter(inactiveColor);

        if (currentSort.equals("best_selling")) binding.tvBestSelling.setTextColor(activeColor);
        else if (currentSort.equals("new_arrival")) binding.tvNewArrival.setTextColor(activeColor);
        else if (currentSort.equals("discount")) binding.tvDiscountSort.setTextColor(activeColor);
        else if (currentSort.equals("price_asc")) {
            binding.tvSortPriceLabel.setTextColor(activeColor);
            binding.ivSortPriceIcon.setColorFilter(activeColor);
            binding.ivSortPriceIcon.setRotation(0);
        } else if (currentSort.equals("price_desc")) {
            binding.tvSortPriceLabel.setTextColor(activeColor);
            binding.ivSortPriceIcon.setColorFilter(activeColor);
            binding.ivSortPriceIcon.setRotation(180);
        }
    }

    private void autoCompleteOrders() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
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

                if ("Đã giao".equals(status) && (currentTime - orderTime > threeDaysInMillis)) {
                    orderManager.updateOrderStatus(doc.getId(), "Hoàn thành", new OrderManager.OnActionCompleteListener() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(String error) {}
                    });
                }
                
                if ("Hoàn thành".equals(status) && (currentTime - orderTime > threeDaysInMillis)) {
                    long deliveryTime = order.getDeliveryDate() != null ? order.getDeliveryDate().getTime() : 0;
                    if (deliveryTime > (currentTime - oneDayInMillis)) {
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTime(order.getTimestamp());
                        cal.add(java.util.Calendar.DAY_OF_YEAR, 3);
                        firestore.collection("orders").document(doc.getId()).update("deliveryDate", cal.getTime());
                    }
                }

                if ("Yêu cầu hủy".equals(status) && order.getCancelTimestamp() != null) {
                    if (currentTime - order.getCancelTimestamp().getTime() > oneDayInMillis) {
                        java.util.Map<String, Object> autoCancelUpdates = new java.util.HashMap<>();
                        autoCancelUpdates.put("status", "Đã hủy");
                        autoCancelUpdates.put("cancelReason", "Đơn hàng đã được tự động hủy bởi hệ thống do quá 24h Shop không phản hồi");
                        autoCancelUpdates.put("cancelledBy", "system");
                        
                        firestore.collection("orders").document(doc.getId()).update(autoCancelUpdates)
                                .addOnSuccessListener(aVoid -> {
                                    if (order.getItems() != null) {
                                        for (CartItem item : order.getItems()) {
                                            if (item.getProductId() != null) {
                                                firestore.collection("products").document(item.getProductId())
                                                        .update("stock", com.google.firebase.firestore.FieldValue.increment(item.getQuantity()));
                                            }
                                        }
                                    }
                                });
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        observeBadges();
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartListener != null) cartListener.remove();
        if (notificationListener != null) notificationListener.remove();
    }
}





