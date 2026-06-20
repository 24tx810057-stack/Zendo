package com.zendo.apps;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zendo.apps.databinding.ActivityProfileBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userEmail, userRole;
    private List<Product> suggestionList = new ArrayList<>();
    private ProductAdapter suggestionAdapter;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        userEmail = prefManager.getUserEmail();
        userRole = prefManager.getUserRole();

        setupUIByRole();
        setupListeners();
        
        if (!"admin".equals(userRole)) {
            setupSuggestions();
        }

        binding.tvProfileRole.setText(userRole.equals("admin") ? "Chủ cửa hàng" : "Thành viên");
    }

    private void setupUIByRole() {
        boolean isAdmin = "admin".equals(userRole);
        
        binding.sectionFavorite.setVisibility(View.VISIBLE);
        
        if (isAdmin) {
            binding.layoutUserMenu.setVisibility(View.GONE);
            binding.layoutAdminMenu.setVisibility(View.VISIBLE);
            binding.sectionSuggestion.setVisibility(View.GONE);
            binding.layoutCartProfile.setVisibility(View.GONE);
        } else {
            binding.layoutUserMenu.setVisibility(View.VISIBLE);
            binding.layoutAdminMenu.setVisibility(View.GONE);
            binding.sectionSuggestion.setVisibility(View.VISIBLE);
            binding.layoutCartProfile.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        binding.ivAvatar.setOnClickListener(v -> startActivity(new Intent(this, AccountDetailActivity.class)));
        
        binding.layoutCartProfile.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, CartActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Không thể mở Giỏ hàng", Toast.LENGTH_SHORT).show();
            }
        });
        
        binding.btnNotificationProfile.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        binding.btnSetting.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        binding.sectionChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.sectionFavorite.setOnClickListener(v -> startActivity(new Intent(this, FavoriteActivity.class)));

        binding.btnAdminOrders.setOnClickListener(v -> startActivity(new Intent(this, OrderListActivity.class)));
        binding.btnAdminProducts.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
        binding.btnAdminVouchers.setOnClickListener(v -> startActivity(new Intent(this, VoucherManagementActivity.class)));
        binding.btnAdminReviews.setOnClickListener(v -> startActivity(new Intent(this, AdminReviewsActivity.class)));
        binding.btnAdminReturnRequests.setOnClickListener(v -> startActivity(new Intent(this, AdminReturnListActivity.class)));
        binding.btnAdminWarrantyRequests.setOnClickListener(v -> startActivity(new Intent(this, AdminWarrantyListActivity.class)));
        binding.btnAdminPaymentSettings.setOnClickListener(v -> startActivity(new Intent(this, PaymentSettingsActivity.class)));
        binding.btnRevenueStats.setOnClickListener(v -> startActivity(new Intent(this, RevenueActivity.class)));

        binding.btnOrderHistory.setOnClickListener(v -> startActivity(new Intent(this, OrderListActivity.class)));

        binding.btnStatusPending.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderListActivity.class);
            intent.putExtra("tab_index", 1);
            startActivity(intent);
        });
        binding.btnStatusPickup.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderListActivity.class);
            intent.putExtra("tab_index", 2);
            startActivity(intent);
        });
        binding.btnStatusShipping.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderListActivity.class);
            intent.putExtra("tab_index", 3);
            startActivity(intent);
        });
        binding.btnStatusReview.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderListActivity.class);
            intent.putExtra("tab_index", 4);
            startActivity(intent);
        });

        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            SharedPrefManager.getInstance(this).clear();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        binding.bottomNavProfile.setSelectedItemId(R.id.nav_me);
        binding.bottomNavProfile.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, ListActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return id == R.id.nav_me;
        });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        final EditText etOldPass = dialogView.findViewById(R.id.etOldPass);
        final EditText etNewPass = dialogView.findViewById(R.id.etNewPass);
        final EditText etConfirmPass = dialogView.findViewById(R.id.etConfirmPass);

        builder.setPositiveButton("Cập nhật", (dialog, which) -> {
            String oldPass = etOldPass.getText().toString().trim();
            String newPass = etNewPass.getText().toString().trim();
            String confirmPass = etConfirmPass.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update password in Firebase Auth as well
            if (mAuth.getCurrentUser() != null) {
                mAuth.getCurrentUser().updatePassword(newPass)
                        .addOnSuccessListener(aVoid -> {
                            db.collection("users").document(userEmail).update("password", newPass);
                            Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void setupSuggestions() {
        suggestionAdapter = new ProductAdapter(this, suggestionList);
        binding.gvSuggestions.setAdapter(suggestionAdapter);
        binding.gvSuggestions.setOnItemClickListener((parent, view, position, id) -> {
            Product selectedProduct = suggestionList.get(position);
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("product_data", selectedProduct);
            startActivity(intent);
        });
        loadSuggestions();
    }

    private void loadSuggestions() {
        if (userEmail == null || userEmail.isEmpty()) return;
        
        final Set<String> purchaseTags = new HashSet<>();
        final Set<String> cartTags = new HashSet<>();
        final Set<String> likedTags = new HashSet<>();
        
        db.collection("orders").whereEqualTo("userEmail", userEmail).get().addOnSuccessListener(orderSnaps -> {
            for (QueryDocumentSnapshot doc : orderSnaps) {
                Order order = doc.toObject(Order.class);
                if (order.getItems() != null) {
                    for (CartItem item : order.getItems()) {
                        String[] words = item.getProductName().split(" ");
                        for(String w : words) if(w.length() > 3) purchaseTags.add(w.toLowerCase());
                    }
                }
            }
            
            db.collection("cart").whereEqualTo("userEmail", userEmail).get().addOnSuccessListener(cartSnaps -> {
                for (QueryDocumentSnapshot doc : cartSnaps) {
                    CartItem item = doc.toObject(CartItem.class);
                    String[] words = item.getProductName().split(" ");
                    for(String w : words) if(w.length() > 3) cartTags.add(w.toLowerCase());
                }
                
                db.collection("products").whereArrayContains("likedBy", userEmail).get().addOnSuccessListener(likedSnaps -> {
                    for (QueryDocumentSnapshot doc : likedSnaps) {
                        Product p = doc.toObject(Product.class);
                        likedTags.addAll(p.getTags());
                    }
                    
                    // History from SharedPrefManager (if implemented) or fallback
                    android.content.SharedPreferences sharedPref = getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);
                    String searchHistoryStr = sharedPref.getString("search_history", "");
                    List<String> searchHistory = new ArrayList<>();
                    if (!searchHistoryStr.isEmpty()) {
                        searchHistory.addAll(java.util.Arrays.asList(searchHistoryStr.split(",")));
                    }
                    
                    fetchAndSortProducts(purchaseTags, cartTags, likedTags, searchHistory);
                });
            });
        });
    }

    private void fetchAndSortProducts(Set<String> purchaseTags, Set<String> cartTags, Set<String> likedTags, List<String> searchHistory) {
        db.collection("products").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Product> allProducts = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Product product = document.toObject(Product.class);
                product.setId(document.getId());
                if (product.getStock() > 0) allProducts.add(product);
            }
            
            List<Product> recommended = RecommendationEngine.recommendForUser(
                    purchaseTags, cartTags, likedTags, searchHistory, allProducts, 20);
            
            suggestionList.clear();
            suggestionList.addAll(recommended);
            if (suggestionAdapter != null) suggestionAdapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfoFromFirestore();
        observeNotificationBadge();
        if ("admin".equals(userRole)) {
            updateAdminOrderBadge();
            updateAdminReturnBadge();
            updateAdminWarrantyBadge();
            updateRevenueStats();
        } else {
            updateCartBadge();
            updateOrderBadges();
            loadSuggestions(); 
        }
    }

    private void observeNotificationBadge() {
        if (notificationListener != null) notificationListener.remove();
        
        String targetEmail = "admin".equals(userRole) ? "admin" : userEmail;
        
        notificationListener = db.collection("notifications")
                .whereEqualTo("userEmail", targetEmail)
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value == null) return;
                    updateBadge(binding.tvNotifBadgeProfile, value.size());
                });
    }

    private void updateRevenueStats() {
        db.collection("orders")
                .whereEqualTo("status", "Hoàn thành")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalRevenue = 0;
                    int totalProductsSold = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        totalRevenue += order.getTotalAmount();
                        
                        if (order.getItems() != null) {
                            for (CartItem item : order.getItems()) {
                                totalProductsSold += item.getQuantity();
                            }
                        }
                    }
                    binding.tvTotalRevenue.setText(formatter.format(totalRevenue) + "đ");
                    binding.tvCompletedOrders.setText(totalProductsSold + " sản phẩm");
                    TextView tvLabel = ((View) binding.tvCompletedOrders.getParent()).findViewById(R.id.tvCompletedOrdersLabel);
                    if (tvLabel != null) {
                        tvLabel.setText("Tổng SP đã bán");
                    }
                })
                .addOnFailureListener(e -> Log.e("ProfileActivity", "Error updating stats: ", e));
    }

    private void updateAdminOrderBadge() {
        db.collection("orders")
                .whereEqualTo("status", "Chờ xác nhận")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    updateBadge(binding.tvAdminOrderBadge, queryDocumentSnapshots.size());
                });
    }

    private void updateAdminReturnBadge() {
        db.collection("return_requests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    updateBadge(findViewById(R.id.tvReturnBadge), queryDocumentSnapshots.size());
                });
    }

    private void updateAdminWarrantyBadge() {
        db.collection("warranty_requests")
                .whereEqualTo("status", "pending_repair")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    updateBadge(findViewById(R.id.tvWarrantyBadge), queryDocumentSnapshots.size());
                });
    }

    private void updateCartBadge() {
        if (userEmail == null || userEmail.isEmpty()) return;
        db.collection("cart").whereEqualTo("userEmail", userEmail).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalCount = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long quantity = doc.getLong("quantity");
                        if (quantity != null) totalCount += quantity.intValue();
                    }
                    updateBadge(binding.tvCartBadge, totalCount);
                });
    }

    private void loadUserInfoFromFirestore() {
        if (userEmail == null || userEmail.isEmpty()) return;
        db.collection("users").document(userEmail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            String displayName = user.getNickname();
                            if (displayName == null || displayName.isEmpty()) {
                                displayName = user.getFullName();
                            }
                            
                            if (displayName != null && !displayName.isEmpty()) {
                                binding.tvProfileName.setText(displayName);
                            }

                            String base64Avatar = user.getAvatar();
                            if (base64Avatar != null && !base64Avatar.isEmpty()) {
                                try {
                                    byte[] decodedString = Base64.decode(base64Avatar, Base64.DEFAULT);
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    binding.ivAvatar.setImageBitmap(decodedByte);
                                } catch (Exception e) { binding.ivAvatar.setImageResource(R.drawable.ic_person); }
                            } else binding.ivAvatar.setImageResource(R.drawable.ic_person);
                        }
                    }
                });
    }

    private void updateOrderBadges() {
        db.collection("orders").whereEqualTo("userEmail", userEmail).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int pending = 0, pickup = 0, shipping = 0;
                List<String> deliveredOrderIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String status = doc.getString("status");
                    if (status == null) continue;
                    switch (status) {
                        case "Chờ xác nhận": pending++; break;
                        case "Chờ lấy hàng": pickup++; break;
                        case "Đang giao": shipping++; break;
                        case "Đã giao": deliveredOrderIds.add(doc.getId()); break;
                    }
                }
                updateBadge(findViewById(R.id.badgePending), pending);
                updateBadge(findViewById(R.id.badgePickup), pickup);
                updateBadge(findViewById(R.id.badgeShipping), shipping);
                checkUnreviewedOrders(deliveredOrderIds);
            });
    }

    private void checkUnreviewedOrders(List<String> orderIds) {
        if (orderIds.isEmpty()) { updateBadge(findViewById(R.id.badgeReview), 0); return; }
        AtomicInteger unreviewedCount = new AtomicInteger(0);
        AtomicInteger processedOrders = new AtomicInteger(0);
        for (String orderId : orderIds) {
            db.collection("reviews").whereEqualTo("orderId", orderId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) unreviewedCount.incrementAndGet();
                    }
                    if (processedOrders.incrementAndGet() == orderIds.size()) {
                        updateBadge(findViewById(R.id.badgeReview), unreviewedCount.get());
                    }
                });
        }
    }

    private void updateBadge(TextView badge, int count) {
        if (badge != null) {
            if (count > 0) {
                badge.setText(String.valueOf(count));
                badge.setVisibility(View.VISIBLE);
            } else { badge.setVisibility(View.GONE); }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notificationListener != null) notificationListener.remove();
    }
}
