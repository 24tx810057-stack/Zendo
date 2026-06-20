package com.zendo.apps;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zendo.apps.databinding.DialogReviewBinding;
import java.util.ArrayList;
import java.util.List;

public class AddReviewActivity extends AppCompatActivity {

    private DialogReviewBinding binding;
    private FirebaseFirestore db;
    private String userEmail, userName, productId, orderId;
    private CartItem cartItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DialogReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");
        userName = sharedPref.getString("user_name", "Người dùng Zendo");

        productId = getIntent().getStringExtra("product_id");
        orderId = getIntent().getStringExtra("order_id");
        cartItem = (CartItem) getIntent().getSerializableExtra("cart_item");

        initViews();
        displayInfo();
    }

    private void initViews() {
        binding.btnCancelReview.setOnClickListener(v -> finish());
        binding.btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void displayInfo() {
        if (cartItem != null) {
            binding.tvReviewProductName.setText(cartItem.getProductName());
            String imgData = cartItem.getProductImageUrl();
            if (imgData != null && !imgData.isEmpty()) {
                if (imgData.startsWith("http")) Glide.with(this).load(imgData).into(binding.ivReviewProduct);
                else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        Glide.with(this).load(decodedString).into(binding.ivReviewProduct);
                    } catch (Exception e) {}
                }
            }
        }
    }

    private void submitReview() {
        float rating = binding.rbQuality.getRating();
        String comment = binding.etReviewComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (comment.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung đánh giá!", Toast.LENGTH_SHORT).show();
            binding.etReviewComment.setError("Nội dung không được để trống");
            return;
        }

        List<String> selectedTags = new ArrayList<>();
        for (int i = 0; i < binding.chipGroupTags.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipGroupTags.getChildAt(i);
            if (chip.isChecked()) selectedTags.add(chip.getText().toString());
        }

        Review review = new Review();
        review.setProductId(productId);
        review.setOrderId(orderId);
        review.setUserEmail(userEmail);
        review.setUserName(userName);
        review.setQualityRating(rating);
        review.setSellerRating(binding.rbSeller.getRating());
        review.setShippingRating(binding.rbShipping.getRating());
        review.setComment(comment);
        review.setTags(selectedTags);
        review.setAnonymous(binding.cbAnonymous.isChecked());
        review.setTimestamp(System.currentTimeMillis());

        db.collection("reviews").add(review)
                .addOnSuccessListener(documentReference -> {
                    // CẬP NHẬT ĐƠN HÀNG LÀ ĐÃ ĐÁNH GIÁ
                    db.collection("orders").document(orderId).update("reviewed", true);

                    Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
