package com.example.buoi1;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView tvStatus, tvOrderDate, tvNamePhone, tvAddress;
    private TextView tvSubtotal, tvShipping, tvTotal, tvPaymentMethod;
    private LinearLayout layoutProducts;
    private Button btnMainAction, btnContact, btnDeleteOrder;
    
    private Order order;
    private OrderManager orderManager;
    private String userEmail, userRole;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");
        userRole = sharedPref.getString("user_role", "user");
        
        order = (Order) getIntent().getSerializableExtra("order_data");
        orderManager = new OrderManager();

        initViews();
        displayOrderDetails();

        // Kiểm tra nếu được yêu cầu mở form đánh giá ngay lập tức
        boolean triggerReview = getIntent().getBooleanExtra("trigger_review", false);
        if (triggerReview && order != null && "Đã giao".equals(order.getStatus())) {
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                showReviewDialog(order.getItems().get(0));
            }
        }
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvOrderStatus);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvNamePhone = findViewById(R.id.tvReceiverNamePhone);
        tvAddress = findViewById(R.id.tvReceiverAddress);
        tvSubtotal = findViewById(R.id.tvOrderSubtotal);
        tvShipping = findViewById(R.id.tvOrderShipping);
        tvTotal = findViewById(R.id.tvOrderTotal);
        tvPaymentMethod = findViewById(R.id.tvOrderPaymentMethod);
        layoutProducts = findViewById(R.id.layoutOrderProducts);
        btnMainAction = findViewById(R.id.btnMainAction);
        btnContact = findViewById(R.id.btnContactSeller);
        
        findViewById(R.id.btnBackOrder).setOnClickListener(v -> finish());
    }

    private void displayOrderDetails() {
        if (order == null) return;

        tvStatus.setText(order.getStatus());
        tvNamePhone.setText(order.getUserName() + " | " + order.getPhone());
        tvAddress.setText(order.getAddress());
        
        if (order.getTimestamp() != null) {
            tvOrderDate.setText("Ngày đặt hàng: " + dateFormat.format(order.getTimestamp()));
        } else {
            tvOrderDate.setText("Ngày đặt hàng: N/A");
        }
        
        tvSubtotal.setText(formatter.format(order.getTotalAmount() - order.getShippingFee()) + "đ");
        tvShipping.setText(formatter.format(order.getShippingFee()) + "đ");
        tvTotal.setText(formatter.format(order.getTotalAmount()) + "đ");
        tvPaymentMethod.setText(order.getPaymentMethod());

        displayProducts(order.getItems());
        setupFooterActions();
    }

    private void displayProducts(List<CartItem> items) {
        if (items == null) return;
        layoutProducts.removeAllViews();
        for (CartItem item : items) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_cart, layoutProducts, false);
            if(view.findViewById(R.id.cbSelectItem) != null) view.findViewById(R.id.cbSelectItem).setVisibility(View.GONE);
            if(view.findViewById(R.id.btnMinus) != null) view.findViewById(R.id.btnMinus).setVisibility(View.GONE);
            if(view.findViewById(R.id.btnPlus) != null) view.findViewById(R.id.btnPlus).setVisibility(View.GONE);
            
            TextView tvName = view.findViewById(R.id.tvCartProductName);
            TextView tvPrice = view.findViewById(R.id.tvCartProductPrice);
            TextView tvQuantity = view.findViewById(R.id.tvQuantity);
            ImageView ivProduct = view.findViewById(R.id.ivCartProduct);

            tvName.setText(item.getProductName());
            tvPrice.setText(formatter.format(item.getProductPrice()) + "đ");
            tvQuantity.setText("x" + item.getQuantity());

            String imgData = item.getProductImageUrl();
            if (imgData != null && !imgData.isEmpty()) {
                if (imgData.startsWith("http")) Glide.with(this).load(imgData).into(ivProduct);
                else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivProduct.setImageBitmap(bitmap);
                    } catch (Exception e) {}
                }
            }

            // Click vào sản phẩm để xem chi tiết
            view.setOnClickListener(v -> {
                db.collection("products").document(item.getProductId()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            Product p = documentSnapshot.toObject(Product.class);
                            if (p != null) {
                                Intent intent = new Intent(this, DetailActivity.class);
                                intent.putExtra("product_data", p);
                                startActivity(intent);
                            }
                        });
            });

            layoutProducts.addView(view);
        }
    }

    private void setupFooterActions() {
        String status = order.getStatus();
        if (status == null) status = "";
        
        // LOGIC CHO ADMIN
        if ("admin".equals(userRole)) {
            // Nếu đã giao thì ẩn nút xóa đơn
            if (status.equals("Đã giao")) {
                btnContact.setVisibility(View.GONE);
            } else {
                btnContact.setVisibility(View.VISIBLE);
                btnContact.setText("XÓA ĐƠN");
                btnContact.setBackgroundTintList(getColorStateList(android.R.color.holo_red_dark));
                btnContact.setTextColor(getColor(android.R.color.white));
                btnContact.setOnClickListener(v -> confirmDeleteOrder());
            }

            if (btnMainAction != null) {
                btnMainAction.setVisibility(View.VISIBLE);
                
                if (status.equals("Yêu cầu hủy")) {
                    btnMainAction.setText("Đồng ý hủy đơn");
                    btnMainAction.setOnClickListener(v -> updateStatusByAdmin("Đã hủy"));
                    
                    btnContact.setVisibility(View.VISIBLE); // Phải hiện lại nếu là Yêu cầu hủy
                    btnContact.setText("Từ chối hủy");
                    btnContact.setBackgroundTintList(getColorStateList(android.R.color.holo_orange_dark));
                    btnContact.setOnClickListener(v -> updateStatusByAdmin("Chờ xác nhận"));
                } else if (status.equals("Chờ xác nhận")) {
                    btnMainAction.setText("Xác nhận đơn hàng");
                    btnMainAction.setOnClickListener(v -> updateStatusByAdmin("Chờ lấy hàng"));
                } else if (status.equals("Chờ lấy hàng")) {
                    btnMainAction.setText("Giao cho ĐVVC");
                    btnMainAction.setOnClickListener(v -> updateStatusByAdmin("Đang giao"));
                } else if (status.equals("Đang giao")) {
                    btnMainAction.setText("Xác nhận đã giao");
                    btnMainAction.setOnClickListener(v -> updateStatusByAdmin("Đã giao"));
                } else {
                    btnMainAction.setVisibility(View.GONE);
                }
            }
            return;
        }

        // LOGIC CHO NGƯỜI DÙNG
        if (btnMainAction != null) {
            btnMainAction.setVisibility(View.VISIBLE);
            btnMainAction.setEnabled(true);
            btnMainAction.setAlpha(1.0f);
            
            // Mặc định cho nút Liên hệ của User
            if (btnContact != null) {
                btnContact.setVisibility(View.VISIBLE);
                btnContact.setText("LIÊN HỆ");
                btnContact.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
                btnContact.setOnClickListener(v -> Toast.makeText(this, "Tính năng chat đang phát triển", Toast.LENGTH_SHORT).show());
            }

            if (status.equals("Chờ xác nhận") || status.equals("Chờ lấy hàng")) {
                btnMainAction.setText("Yêu cầu hủy đơn");
                btnMainAction.setOnClickListener(v -> requestCancelOrder());
            } else if (status.equals("Yêu cầu hủy")) {
                btnMainAction.setText("Đang chờ duyệt hủy...");
                btnMainAction.setEnabled(false);
                btnMainAction.setAlpha(0.5f);
                
                // Cho phép người dùng hủy yêu cầu hủy
                if (btnContact != null) {
                    btnContact.setText("Rút lại yêu cầu hủy");
                    btnContact.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_dark));
                    btnContact.setOnClickListener(v -> undoCancelRequest());
                }
            } else if (status.equals("Đã giao")) {
                checkIfAlreadyReviewed();
            } else {
                btnMainAction.setText("Mua lại");
                btnMainAction.setOnClickListener(v -> Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void checkIfAlreadyReviewed() {
        db.collection("reviews")
                .whereEqualTo("userEmail", order.getUserEmail())
                .whereEqualTo("orderId", order.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        btnMainAction.setText("Đã đánh giá");
                        btnMainAction.setEnabled(false);
                        btnMainAction.setAlpha(0.5f);
                    } else {
                        btnMainAction.setText("Đánh giá sản phẩm");
                        btnMainAction.setOnClickListener(v -> {
                            if (order.getItems() != null && !order.getItems().isEmpty()) {
                                showReviewDialog(order.getItems().get(0));
                            }
                        });
                    }
                });
    }

    private void requestCancelOrder() {
        new AlertDialog.Builder(this)
                .setTitle("Gửi yêu cầu hủy")
                .setMessage("Yêu cầu hủy của bạn sẽ được gửi tới Shop để duyệt. Bạn có chắc chắn không?")
                .setPositiveButton("Gửi yêu cầu", (d, w) -> updateStatusByAdmin("Yêu cầu hủy"))
                .setNegativeButton("Quay lại", null)
                .show();
    }

    private void undoCancelRequest() {
        new AlertDialog.Builder(this)
                .setTitle("Rút lại yêu cầu")
                .setMessage("Bạn muốn hủy yêu cầu hủy đơn và tiếp tục mua hàng chứ?")
                .setPositiveButton("Đồng ý", (d, w) -> updateStatusByAdmin("Chờ xác nhận"))
                .setNegativeButton("Bỏ qua", null)
                .show();
    }

    private void confirmDeleteOrder() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn đơn hàng này không?")
                .setPositiveButton("Xóa", (d, w) -> {
                    orderManager.deleteOrder(order.getId(), new OrderManager.OnActionCompleteListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(OrderDetailActivity.this, "Đã xóa đơn hàng", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(OrderDetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateStatusByAdmin(String newStatus) {
        orderManager.updateOrderStatus(order.getId(), newStatus, new OrderManager.OnActionCompleteListener() {
            @Override
            public void onSuccess() {
                // Tạo thông báo cho người dùng
                createStatusNotification(newStatus);
                Toast.makeText(OrderDetailActivity.this, "Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show();
                
                // Cập nhật lại dữ liệu local để UI thay đổi kịp thời
                order.setStatus(newStatus);
                displayOrderDetails();
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createStatusNotification(String status) {
        String title = "Cập nhật đơn hàng";
        String message = "";
        
        switch (status) {
            case "Chờ lấy hàng":
                message = "Đơn hàng của bạn đã được Shop xác nhận và đang chờ lấy hàng.";
                break;
            case "Đang giao":
                message = "Đơn hàng đang được giao đến bạn. Vui lòng chú ý điện thoại.";
                break;
            case "Đã giao":
                title = "Giao hàng thành công";
                message = "Đơn hàng đã được giao thành công. Hãy để lại đánh giá cho sản phẩm nhé!";
                break;
            case "Đã hủy":
                title = "Đanh hàng đã hủy";
                message = "Đơn hàng của bạn đã bị hủy. Hẹn gặp lại bạn lần sau.";
                break;
            case "Yêu cầu hủy":
                title = "Yêu cầu hủy đơn";
                message = "Bạn đã gửi yêu cầu hủy đơn hàng. Vui lòng chờ Shop phê duyệt.";
                break;
            case "Chờ xác nhận":
                message = "Đơn hàng của bạn đang được tiếp tục xử lý.";
                break;
        }

        if (message.isEmpty()) return;

        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        Map<String, Object> notification = new HashMap<>();
        notification.put("userEmail", order.getUserEmail());
        notification.put("title", title);
        notification.put("message", message);
        notification.put("date", dateStr);
        // Quan trọng: Field timestamp để sắp xếp
        notification.put("timestamp", FieldValue.serverTimestamp());

        db.collection("notifications").add(notification);
    }

    private void showReviewDialog(CartItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null);
        ImageView ivProduct = dialogView.findViewById(R.id.ivReviewProduct);
        TextView tvProductName = dialogView.findViewById(R.id.tvReviewProductName);
        RatingBar rbQuality = dialogView.findViewById(R.id.rbQuality);
        RatingBar rbSeller = dialogView.findViewById(R.id.rbSeller);
        RatingBar rbShipping = dialogView.findViewById(R.id.rbShipping);
        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroupTags);
        EditText etComment = dialogView.findViewById(R.id.etReviewComment);
        CheckBox cbAnonymous = dialogView.findViewById(R.id.cbAnonymous);

        if (tvProductName != null) tvProductName.setText(item.getProductName());
        String imgData = item.getProductImageUrl();
        if (imgData != null && !imgData.isEmpty() && ivProduct != null) {
            if (imgData.startsWith("http")) Glide.with(this).load(imgData).into(ivProduct);
            else {
                try {
                    byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ivProduct.setImageBitmap(bitmap);
                } catch (Exception e) {}
            }
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen)
                .setView(dialogView)
                .setPositiveButton("GỬI ĐÁNH GIÁ", (d, w) -> {
                    Review review = new Review();
                    review.setProductId(item.getProductId());
                    review.setOrderId(order.getId());
                    review.setUserEmail(order.getUserEmail());
                    review.setUserName(order.getUserName());
                    if (rbQuality != null) review.setQualityRating(rbQuality.getRating());
                    if (rbSeller != null) review.setSellerRating(rbSeller.getRating());
                    if (rbShipping != null) review.setShippingRating(rbShipping.getRating());
                    if (etComment != null) review.setComment(etComment.getText().toString());
                    if (cbAnonymous != null) review.setAnonymous(cbAnonymous.isChecked());
                    review.setTimestamp(System.currentTimeMillis());
                    
                    List<String> selectedTags = new ArrayList<>();
                    if (chipGroup != null) {
                        for (int i = 0; i < chipGroup.getChildCount(); i++) {
                            Chip chip = (Chip) chipGroup.getChildAt(i);
                            if (chip.isChecked()) selectedTags.add(chip.getText().toString());
                        }
                    }
                    review.setTags(selectedTags);

                    db.collection("reviews").add(review)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(OrderDetailActivity.this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                            displayOrderDetails();
                        });
                })
                .setNegativeButton("HỦY", null)
                .show();
    }
}
