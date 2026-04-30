package com.example.buoi1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView tvStatus, tvOrderDate, tvNamePhone, tvAddress, tvNote;
    private TextView tvSubtotal, tvShipping, tvVoucher, tvTotal, tvPaymentMethod;
    private View layoutVoucherContainer;
    private LinearLayout layoutProducts;
    private Button btnMainAction, btnCancelAction;
    
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

        // Nếu đi từ nút Đánh giá ở danh sách đơn hàng
        if (getIntent().getBooleanExtra("trigger_review", false)) {
            startAddReviewActivity();
        }
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvOrderStatus);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvNamePhone = findViewById(R.id.tvReceiverNamePhone);
        tvAddress = findViewById(R.id.tvReceiverAddress);
        tvNote = findViewById(R.id.tvOrderNote);
        tvSubtotal = findViewById(R.id.tvOrderSubtotal);
        tvShipping = findViewById(R.id.tvOrderShipping);
        tvVoucher = findViewById(R.id.tvOrderVoucher);
        layoutVoucherContainer = findViewById(R.id.layoutOrderVoucher);
        tvTotal = findViewById(R.id.tvOrderTotal);
        tvPaymentMethod = findViewById(R.id.tvOrderPaymentMethod);
        layoutProducts = findViewById(R.id.layoutOrderProducts);
        btnMainAction = findViewById(R.id.btnMainAction);
        btnCancelAction = findViewById(R.id.btnCancelOrder);
        
        findViewById(R.id.btnBackOrder).setOnClickListener(v -> finish());
    }

    private void startAddReviewActivity() {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) return;
        Intent intent = new Intent(this, AddReviewActivity.class);
        intent.putExtra("product_id", order.getItems().get(0).getProductId());
        intent.putExtra("order_id", order.getId());
        intent.putExtra("cart_item", order.getItems().get(0));
        startActivity(intent);
    }

    private void displayOrderDetails() {
        if (order == null) return;

        tvStatus.setText(order.getStatus());
        tvNamePhone.setText(order.getUserName() + " | " + order.getPhone());
        tvAddress.setText(order.getAddress());
        
        if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
            tvNote.setText(order.getNote());
            tvNote.setTextColor(0xFF212121);
        } else {
            tvNote.setText("Không có ghi chú từ khách hàng");
            tvNote.setTextColor(0xFF9E9E9E);
        }
        
        if (order.getTimestamp() != null) {
            tvOrderDate.setText("Ngày đặt hàng: " + dateFormat.format(order.getTimestamp()));
        }

        tvSubtotal.setText(formatter.format(order.getSubtotal()) + "đ");
        tvShipping.setText(formatter.format(order.getShippingFee()) + "đ");
        
        if (order.getVoucherDiscount() > 0) {
            layoutVoucherContainer.setVisibility(View.VISIBLE);
            tvVoucher.setText("-" + formatter.format(order.getVoucherDiscount()) + "đ");
        } else {
            layoutVoucherContainer.setVisibility(View.GONE);
        }

        tvTotal.setText(formatter.format(order.getTotalAmount()) + "đ");
        tvPaymentMethod.setText("Phương thức thanh toán: " + order.getPaymentMethod());

        displayProducts(order.getItems());
        setupFooterActions();
    }

    private void displayProducts(List<CartItem> items) {
        if (items == null) return;
        layoutProducts.removeAllViews();
        for (CartItem item : items) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_checkout_product, layoutProducts, false);
            TextView tvName = view.findViewById(R.id.tvName);
            TextView tvPrice = view.findViewById(R.id.tvPrice);
            TextView tvQuantity = view.findViewById(R.id.tvQuantity);
            ImageView ivProduct = view.findViewById(R.id.ivProduct);

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
            layoutProducts.addView(view);
        }
    }

    private void setupFooterActions() {
        String status = order.getStatus();
        if (status == null) status = "";
        btnCancelAction.setVisibility(View.GONE);

        if ("admin".equals(userRole)) {
            btnMainAction.setVisibility(View.VISIBLE);
            if (status.equals("Yêu cầu hủy")) {
                btnMainAction.setText("Đồng ý hủy đơn");
                btnMainAction.setOnClickListener(v -> updateStatus("Đã hủy"));
                btnCancelAction.setVisibility(View.VISIBLE);
                btnCancelAction.setText("Từ chối hủy");
                btnCancelAction.setOnClickListener(v -> updateStatus("Chờ xác nhận"));
            } else if (status.equals("Chờ xác nhận")) {
                btnMainAction.setText("Xác nhận đơn hàng");
                btnMainAction.setOnClickListener(v -> updateStatus("Chờ lấy hàng"));
                btnCancelAction.setVisibility(View.VISIBLE);
                btnCancelAction.setText("XÓA ĐƠN");
                btnCancelAction.setOnClickListener(v -> confirmDelete());
            } else if (status.equals("Chờ lấy hàng")) {
                btnMainAction.setText("Giao cho ĐVVC");
                btnMainAction.setOnClickListener(v -> updateStatus("Đang giao"));
            } else if (status.equals("Đang giao")) {
                btnMainAction.setText("Xác nhận đã giao");
                btnMainAction.setOnClickListener(v -> updateStatus("Đã giao"));
            } else {
                btnMainAction.setVisibility(View.GONE);
                btnCancelAction.setVisibility(View.VISIBLE);
                btnCancelAction.setText("XÓA ĐƠN");
                btnCancelAction.setOnClickListener(v -> confirmDelete());
            }
        } else {
            btnMainAction.setVisibility(View.VISIBLE);
            if (status.equals("Chờ xác nhận") || status.equals("Chờ lấy hàng")) {
                btnMainAction.setText("Gửi yêu cầu hủy");
                btnMainAction.setOnClickListener(v -> updateStatus("Yêu cầu hủy"));
            } else if (status.equals("Yêu cầu hủy")) {
                btnMainAction.setText("Đang chờ duyệt hủy...");
                btnMainAction.setEnabled(false);
                btnMainAction.setAlpha(0.6f);
            } else if (status.equals("Đã giao")) {
                db.collection("reviews").whereEqualTo("orderId", order.getId()).get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (queryDocumentSnapshots.isEmpty()) {
                                btnMainAction.setText("Đánh giá sản phẩm");
                                btnMainAction.setOnClickListener(v -> startAddReviewActivity());
                            } else {
                                btnMainAction.setText("Tiếp tục mua sắm");
                                btnMainAction.setOnClickListener(v -> finish());
                            }
                        });
            } else {
                btnMainAction.setText("Tiếp tục mua sắm");
                btnMainAction.setOnClickListener(v -> finish());
            }
        }
    }

    private void updateStatus(String newStatus) {
        orderManager.updateOrderStatus(order.getId(), newStatus, new OrderManager.OnActionCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(OrderDetailActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                order.setStatus(newStatus);
                displayOrderDetails();
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Hành động này không thể hoàn tác. Xóa đơn hàng?")
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
}
