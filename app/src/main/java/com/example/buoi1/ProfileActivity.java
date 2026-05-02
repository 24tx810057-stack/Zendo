package com.example.buoi1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvRole, tvCartBadge, tvAdminOrderBadge, tvNotifBadge;
    private TextView tvTotalRevenue, tvCompletedOrders;
    private View btnRevenueStats;
    private ImageView btnSetting;
    private View btnCart, layoutUserMenu, layoutAdminMenu, sectionSuggestion, btnChangePassword, btnNotification, sectionFavorite;
    private ShapeableImageView ivAvatar;
    private View btnAdminOrders, btnAdminProducts, btnAdminVouchers, btnAdminReviews, btnAdminReturnRequests;
    private Button btnLogout;
    private BottomNavigationView bottomNav;
    private NonScrollGridView gvSuggestions;
    
    private FirebaseFirestore db;
    private String userEmail, userRole;
    private List<Product> suggestionList = new ArrayList<>();
    private ProductAdapter suggestionAdapter;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");
        userRole = sharedPref.getString("user_role", "user");

        initViews();
        setupUIByRole();
        setupListeners();
        
        if (!"admin".equals(userRole)) {
            setupSuggestions();
        }
    }

    private void initViews() {
        tvName = findViewById(R.id.tvProfileName);
        tvRole = findViewById(R.id.tvProfileRole);
        ivAvatar = findViewById(R.id.ivAvatar);
        btnCart = findViewById(R.id.layoutCartProfile);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        tvAdminOrderBadge = findViewById(R.id.tvAdminOrderBadge);
        tvNotifBadge = findViewById(R.id.tvNotifBadgeProfile);
        btnNotification = findViewById(R.id.btnNotificationProfile);
        btnSetting = findViewById(R.id.btnSetting);
        
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        btnRevenueStats = findViewById(R.id.btnRevenueStats);
        
        layoutUserMenu = findViewById(R.id.layoutUserMenu);
        layoutAdminMenu = findViewById(R.id.layoutAdminMenu);
        sectionSuggestion = findViewById(R.id.sectionSuggestion);
        
        btnAdminOrders = findViewById(R.id.btnAdminOrders);
        btnAdminProducts = findViewById(R.id.btnAdminProducts);
        btnAdminVouchers = findViewById(R.id.btnAdminVouchers);
        btnAdminReviews = findViewById(R.id.btnAdminReviews);
        btnAdminReturnRequests = findViewById(R.id.btnAdminReturnRequests);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        sectionFavorite = findViewById(R.id.sectionFavorite);
        
        gvSuggestions = findViewById(R.id.gvSuggestions);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNav = findViewById(R.id.bottomNavProfile);

        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String savedName = sharedPref.getString("user_name", "");
        if (tvName != null) {
            if (!savedName.isEmpty()) tvName.setText(savedName);
            else tvName.setText(userEmail);
        }
        if (tvRole != null) tvRole.setText(userRole.equals("admin") ? "Chủ cửa hàng" : "Thành viên");
    }

    private void setupUIByRole() {
        boolean isAdmin = "admin".equals(userRole);
        
        if (sectionFavorite != null) {
            sectionFavorite.setVisibility(View.VISIBLE);
        }
        
        if (isAdmin) {
            if (layoutUserMenu != null) layoutUserMenu.setVisibility(View.GONE);
            if (layoutAdminMenu != null) layoutAdminMenu.setVisibility(View.VISIBLE);
            if (sectionSuggestion != null) sectionSuggestion.setVisibility(View.GONE);
            if (btnCart != null) btnCart.setVisibility(View.GONE);
        } else {
            if (layoutUserMenu != null) layoutUserMenu.setVisibility(View.VISIBLE);
            if (layoutAdminMenu != null) layoutAdminMenu.setVisibility(View.GONE);
            if (sectionSuggestion != null) sectionSuggestion.setVisibility(View.VISIBLE);
            if (btnCart != null) btnCart.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        if (ivAvatar != null) ivAvatar.setOnClickListener(v -> startActivity(new Intent(this, AccountDetailActivity.class)));
        
        if (btnCart != null) {
            btnCart.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, CartActivity.class));
                } catch (Exception e) {
                    Toast.makeText(this, "Không thể mở Giỏ hàng", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        }
        
        if (btnSetting != null) btnSetting.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        
        findViewById(R.id.sectionChat).setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        if (sectionFavorite != null) {
            sectionFavorite.setOnClickListener(v -> startActivity(new Intent(this, FavoriteActivity.class)));
        }

        if (btnAdminOrders != null) btnAdminOrders.setOnClickListener(v -> startActivity(new Intent(this, OrderListActivity.class)));

        if (btnAdminProducts != null) btnAdminProducts.setOnClickListener(v -> {
            startActivity(new Intent(this, AddProductActivity.class));
        });

        if (btnAdminVouchers != null) btnAdminVouchers.setOnClickListener(v -> startActivity(new Intent(this, VoucherManagementActivity.class)));
        if (btnAdminReviews != null) btnAdminReviews.setOnClickListener(v -> startActivity(new Intent(this, AdminReviewsActivity.class)));
        if (btnAdminReturnRequests != null) {
            btnAdminReturnRequests.setOnClickListener(v -> {
                // Sẽ tạo activity này ở bước sau
                startActivity(new Intent(this, AdminReturnListActivity.class));
            });
        }

        if (btnRevenueStats != null) {
            btnRevenueStats.setOnClickListener(v -> {
                startActivity(new Intent(this, RevenueActivity.class));
            });
        }

        View btnOrderHistory = findViewById(R.id.btnOrderHistory);
        if (btnOrderHistory != null) btnOrderHistory.setOnClickListener(v -> startActivity(new Intent(this, OrderListActivity.class)));

        View.OnClickListener statusClick = v -> startActivity(new Intent(this, OrderListActivity.class));
        if (findViewById(R.id.btnStatusPending) != null) findViewById(R.id.btnStatusPending).setOnClickListener(statusClick);
        if (findViewById(R.id.btnStatusPickup) != null) findViewById(R.id.btnStatusPickup).setOnClickListener(statusClick);
        if (findViewById(R.id.btnStatusShipping) != null) findViewById(R.id.btnStatusShipping).setOnClickListener(statusClick);
        if (findViewById(R.id.btnStatusReview) != null) findViewById(R.id.btnStatusReview).setOnClickListener(statusClick);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().clear().apply();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_me);
            bottomNav.setOnItemSelectedListener(item -> {
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
            db.collection("users").whereEqualTo("email", userEmail).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                            String currentPassOnDb = doc.getString("password");
                            if (oldPass.equals(currentPassOnDb)) {
                                db.collection("users").document(doc.getId())
                                        .update("password", newPass)
                                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show());
                            } else { Toast.makeText(this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show(); }
                        }
                    });
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void setupSuggestions() {
        if (gvSuggestions == null) return;
        suggestionAdapter = new ProductAdapter(this, suggestionList);
        gvSuggestions.setAdapter(suggestionAdapter);
        gvSuggestions.setOnItemClickListener((parent, view, position, id) -> {
            Product selectedProduct = suggestionList.get(position);
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("product_data", selectedProduct);
            startActivity(intent);
        });
        loadSuggestions();
    }

    private void loadSuggestions() {
        if (userEmail == null || userEmail.isEmpty()) return;
        db.collection("orders")
                .whereEqualTo("userEmail", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> favoriteBrands = new HashSet<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        if (order.getItems() != null) {
                            for (CartItem item : order.getItems()) {
                                String[] words = item.getProductName().split(" ");
                                if (words.length > 0) favoriteBrands.add(words[0].toLowerCase());
                            }
                        }
                    }
                    fetchAndSortProducts(favoriteBrands);
                });
    }

    private void fetchAndSortProducts(Set<String> favoriteBrands) {
        db.collection("products")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> allProducts = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        allProducts.add(product);
                    }
                    Collections.sort(allProducts, (p1, p2) -> {
                        int score1 = calculateScore(p1, favoriteBrands);
                        int score2 = calculateScore(p2, favoriteBrands);
                        return Integer.compare(score2, score1);
                    });
                    suggestionList.clear();
                    suggestionList.addAll(allProducts);
                    if (suggestionAdapter != null) suggestionAdapter.notifyDataSetChanged();
                });
    }

    private int calculateScore(Product product, Set<String> favoriteBrands) {
        int score = 0;
        String name = product.getName().toLowerCase();
        String brand = product.getBrand() != null ? product.getBrand().toLowerCase() : "";
        for (String fav : favoriteBrands) {
            if (name.contains(fav)) score += 10;
            if (brand.equals(fav)) score += 20;
        }
        score += product.getSoldCount();
        return score;
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 1. CẬP NHẬT NGAY TỪ CACHE (SharedPreferences) để thấy thay đổi tức thì
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String savedName = sharedPref.getString("user_name", "");
        if (tvName != null && !savedName.isEmpty()) {
            tvName.setText(savedName);
        }

        loadUserInfoFromFirestore();
        observeNotificationBadge();
        if ("admin".equals(userRole)) {
            updateAdminOrderBadge();
            updateAdminReturnBadge();
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
                    updateBadge(tvNotifBadge, value.size());
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
                        
                        // Đếm tổng số lượng sản phẩm trong từng đơn
                        if (order.getItems() != null) {
                            for (CartItem item : order.getItems()) {
                                totalProductsSold += item.getQuantity();
                            }
                        }
                    }
                    if (tvTotalRevenue != null) tvTotalRevenue.setText(formatter.format(totalRevenue) + "đ");
                    if (tvCompletedOrders != null) {
                        tvCompletedOrders.setText(totalProductsSold + " sản phẩm");
                        TextView tvLabel = ((View) tvCompletedOrders.getParent()).findViewById(R.id.tvCompletedOrdersLabel);
                        if (tvLabel != null) {
                            tvLabel.setText("Tổng SP đã bán");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ProfileActivity", "Error updating stats: ", e));
    }

    private void updateAdminOrderBadge() {
        db.collection("orders")
                .whereEqualTo("status", "Chờ xác nhận")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (tvAdminOrderBadge != null) {
                        if (count > 0) {
                            tvAdminOrderBadge.setText(String.valueOf(count));
                            tvAdminOrderBadge.setVisibility(View.VISIBLE);
                        } else { tvAdminOrderBadge.setVisibility(View.GONE); }
                    }
                });
    }

    private void updateAdminReturnBadge() {
        db.collection("return_requests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    TextView tvReturnBadge = findViewById(R.id.tvReturnBadge);
                    if (tvReturnBadge != null) {
                        if (count > 0) {
                            tvReturnBadge.setText(String.valueOf(count));
                            tvReturnBadge.setVisibility(View.VISIBLE);
                        } else { tvReturnBadge.setVisibility(View.GONE); }
                    }
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
                    updateBadge(tvCartBadge, totalCount);
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
                            
                            if (tvName != null && displayName != null && !displayName.isEmpty()) {
                                tvName.setText(displayName);
                                // Cập nhật lại cache để đồng bộ
                                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                                    .edit().putString("user_name", displayName).apply();
                            }

                            String base64Avatar = user.getAvatar();
                            if (ivAvatar != null && base64Avatar != null && !base64Avatar.isEmpty()) {
                                try {
                                    byte[] decodedString = Base64.decode(base64Avatar, Base64.DEFAULT);
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    ivAvatar.setImageBitmap(decodedByte);
                                } catch (Exception e) { ivAvatar.setImageResource(R.drawable.ic_person); }
                            } else if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_person);
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
