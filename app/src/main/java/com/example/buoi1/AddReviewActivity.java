package com.example.buoi1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class AddReviewActivity extends AppCompatActivity {

    private ImageView ivProduct;
    private TextView tvProductName;
    private RatingBar rbQuality, rbSeller, rbShipping;
    private EditText etComment;
    private ChipGroup cgTags;
    private CheckBox cbAnon;
    private Button btnSubmit, btnCancel;

    private FirebaseFirestore db;
    private String userEmail, userName, productId, orderId;
    private CartItem cartItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_review); // Dùng lại layout dialog_review nhưng trong Activity

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
        ivProduct = findViewById(R.id.ivReviewProduct);
        tvProductName = findViewById(R.id.tvReviewProductName);
        rbQuality = findViewById(R.id.rbQuality);
        rbSeller = findViewById(R.id.rbSeller);
        rbShipping = findViewById(R.id.rbShipping);
        etComment = findViewById(R.id.etReviewComment);
        cgTags = findViewById(R.id.chipGroupTags);
        cbAnon = findViewById(R.id.cbAnonymous);
        btnSubmit = findViewById(R.id.btnSubmitReview);
        btnCancel = findViewById(R.id.btnCancelReview);

        btnCancel.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void displayInfo() {
        if (cartItem != null) {
            tvProductName.setText(cartItem.getProductName());
            String imgData = cartItem.getProductImageUrl();
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
        }
    }

    private void submitReview() {
        float rating = rbQuality.getRating();
        String comment = etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (comment.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung đánh giá!", Toast.LENGTH_SHORT).show();
            etComment.setError("Nội dung không được để trống");
            return;
        }

        List<String> selectedTags = new ArrayList<>();
        for (int i = 0; i < cgTags.getChildCount(); i++) {
            Chip chip = (Chip) cgTags.getChildAt(i);
            if (chip.isChecked()) selectedTags.add(chip.getText().toString());
        }

        Review review = new Review();
        review.setProductId(productId);
        review.setOrderId(orderId);
        review.setUserEmail(userEmail);
        review.setUserName(userName);
        review.setQualityRating(rating);
        review.setSellerRating(rbSeller.getRating());
        review.setShippingRating(rbShipping.getRating());
        review.setComment(comment);
        review.setTags(selectedTags);
        review.setAnonymous(cbAnon.isChecked());
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
