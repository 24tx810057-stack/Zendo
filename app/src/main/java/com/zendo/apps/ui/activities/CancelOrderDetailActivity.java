package com.zendo.apps.ui.activities;

import com.zendo.apps.R;

import com.zendo.apps.utils.SharedPrefManager;

import com.zendo.apps.data.models.CartItem;

import com.zendo.apps.data.models.Order;

import com.zendo.apps.data.models.User;

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
import com.zendo.apps.data.models.AuthResultState;
import com.zendo.apps.viewmodels.CartViewModel;

import androidx.lifecycle.ViewModelProvider;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class CancelOrderDetailActivity extends AppCompatActivity {

    private TextView tvCancelTime, tvCancelReason, tvTotal, tvPaymentMethod;
    private LinearLayout layoutProducts;
    private Button btnReorder;
    private Order order;
    private CartViewModel cartViewModel;
    private String userEmail;
    private final DecimalFormat formatter = new DecimalFormat("###,###,###");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancel_order_detail);

        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        userEmail = prefManager.getUserEmail();

        order = (Order) getIntent().getSerializableExtra("order_data");
        if (order == null) {
            finish();
            return;
        }

        initViews();
        displayDetails();
    }

    private void initViews() {
        tvCancelTime = findViewById(R.id.tvCancelTime);
        tvCancelReason = findViewById(R.id.tvCancelReason);
        tvTotal = findViewById(R.id.tvCancelTotal);
        tvPaymentMethod = findViewById(R.id.tvCancelPaymentMethod);
        layoutProducts = findViewById(R.id.layoutCancelProducts);
        btnReorder = findViewById(R.id.btnReorder);

        findViewById(R.id.btnBackCancel).setOnClickListener(v -> finish());
        
        btnReorder.setOnClickListener(v -> handleReorder());
    }

    private void displayDetails() {
        if (order.getCancelTimestamp() != null) {
            tvCancelTime.setText("vào " + dateFormat.format(order.getCancelTimestamp()));
        } else {
            tvCancelTime.setText("Thời gian không xác định");
        }

        String reason = order.getCancelReason();
        tvCancelReason.setText((reason == null || reason.isEmpty()) ? "Không có lý do cụ thể" : reason);

        tvTotal.setText(formatter.format(order.getTotalAmount()) + "đ");
        tvPaymentMethod.setText("Phương thức thanh toán: " + order.getPaymentMethod());

        displayProducts(order.getItems());
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

    private void handleReorder() {
        if (order.getItems() == null || order.getItems().isEmpty()) return;
        
        btnReorder.setEnabled(false);
        btnReorder.setText("ĐANG THÊM VÀO GIỎ...");

        int totalItems = order.getItems().size();
        final int[] completedCount = {0};
        final boolean[] hasError = {false};

        for (CartItem item : order.getItems()) {
            item.setUserEmail(userEmail);
            cartViewModel.addToCart(item).observe(this, state -> {
                if (state.getStatus() == AuthResultState.Status.SUCCESS) {
                    completedCount[0]++;
                } else if (state.getStatus() == AuthResultState.Status.ERROR) {
                    completedCount[0]++;
                    hasError[0] = true;
                }

                if (completedCount[0] == totalItems) {
                    if (hasError[0]) {
                        Toast.makeText(this, "Có lỗi xảy ra khi thêm một số sản phẩm", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Đã thêm tất cả sản phẩm vào giỏ hàng!", Toast.LENGTH_LONG).show();
                    }
                    Intent intent = new Intent(this, CartActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }
}



