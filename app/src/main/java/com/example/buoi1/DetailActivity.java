package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private FirebaseFirestore db;
    private String productId;
    private Product product;
    private String userEmail;

    private TextView tvSpecChip, tvSpecScreen, tvSpecRam, tvSpecRom, tvSpecPin, tvSpecCamera, tvSpecOs;
    private ImageView ivArrowSpecs, ivArrowDesc;
    private TextView tvPrice, tvOldPrice, tvName, tvDesc, tvRating, tvSold, tvStock;
    private LinearLayout layoutContentSpecs, layoutContentDesc;
    
    private TextView tvBigRating, tvReviewCountSubtitle, tvTotalReviewsCount, tvViewAllReviews, tvImageIndicator;
    private RatingBar rbSmallSummary;
    private ViewPager2 vpImages;
    private LinearLayout llTopReviewsContainer;
    private ProgressBar pb5, pb4, pb3, pb2, pb1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");

        vpImages = findViewById(R.id.vpProductDetailImages);
        tvImageIndicator = findViewById(R.id.tvImageIndicator);
        
        tvPrice = findViewById(R.id.tvProductDetailPrice);
        tvOldPrice = findViewById(R.id.tvProductDetailOldPrice);
        tvName = findViewById(R.id.tvProductDetailName);
        tvDesc = findViewById(R.id.tvProductDetailDesc);
        tvRating = findViewById(R.id.tvProductDetailRating);
        tvSold = findViewById(R.id.tvProductDetailSold);
        tvStock = findViewById(R.id.tvProductDetailStock);
        ImageButton btnBack = findViewById(R.id.btnBackDetail);
        
        tvBigRating = findViewById(R.id.tvBigRating);
        tvReviewCountSubtitle = findViewById(R.id.tvReviewCountSubtitle);
        tvTotalReviewsCount = findViewById(R.id.tvTotalReviewsCount);
        rbSmallSummary = findViewById(R.id.rbSmallSummary);
        tvViewAllReviews = findViewById(R.id.tvViewAllReviews);
        llTopReviewsContainer = findViewById(R.id.llTopReviewsContainer);
        
        pb5 = findViewById(R.id.pb5Star);
        pb4 = findViewById(R.id.pb4Star);
        pb3 = findViewById(R.id.pb3Star);
        pb2 = findViewById(R.id.pb2Star);
        pb1 = findViewById(R.id.pb1Star);
        
        tvSpecChip = findViewById(R.id.tvSpecChip);
        tvSpecScreen = findViewById(R.id.tvSpecScreen);
        tvSpecRam = findViewById(R.id.tvSpecRam);
        tvSpecRom = findViewById(R.id.tvSpecRom);
        tvSpecPin = findViewById(R.id.tvSpecPin);
        tvSpecCamera = findViewById(R.id.tvSpecCamera);
        tvSpecOs = findViewById(R.id.tvSpecOs);

        layoutContentSpecs = findViewById(R.id.layoutContentSpecs);
        layoutContentDesc = findViewById(R.id.layoutContentDesc);
        ivArrowSpecs = findViewById(R.id.ivArrowSpecs);
        ivArrowDesc = findViewById(R.id.ivArrowDesc);
        LinearLayout layoutHeaderSpecs = findViewById(R.id.layoutHeaderSpecs);
        LinearLayout layoutHeaderDesc = findViewById(R.id.layoutHeaderDesc);
        
        layoutHeaderSpecs.setOnClickListener(v -> toggleCollapse(layoutContentSpecs, ivArrowSpecs));
        layoutHeaderDesc.setOnClickListener(v -> toggleCollapse(layoutContentDesc, ivArrowDesc));

        LinearLayout layoutUser = findViewById(R.id.layoutUserActions);
        LinearLayout layoutAdmin = findViewById(R.id.layoutAdminActions);
        
        Button btnBuyNow = findViewById(R.id.btnBuyNow);
        Button btnAddToCart = findViewById(R.id.btnAddToCart);
        Button btnEdit = findViewById(R.id.btnEditProduct);
        Button btnDelete = findViewById(R.id.btnDeleteProduct);

        String role = sharedPref.getString("user_role", "user");

        if (role != null && role.equals("admin")) {
            layoutAdmin.setVisibility(View.VISIBLE);
            layoutUser.setVisibility(View.GONE);
        } else {
            layoutAdmin.setVisibility(View.GONE);
            layoutUser.setVisibility(View.VISIBLE);
        }

        product = (Product) getIntent().getSerializableExtra("product_data");
        if (product != null) {
            productId = product.getId();
        }

        btnBack.setOnClickListener(v -> finish());
        btnBuyNow.setOnClickListener(v -> handleBuyNow());
        btnAddToCart.setOnClickListener(v -> addToCart());
        btnDelete.setOnClickListener(v -> showDeleteDialog());
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProductActivity.class);
            intent.putExtra("edit_product", product);
            startActivity(intent);
        });

        if (tvViewAllReviews != null) {
            tvViewAllReviews.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReviewListActivity.class);
                intent.putExtra("product_id", productId);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProductDetails();
        loadProductReviews(); 
    }

    private void loadProductDetails() {
        if (productId == null) return;
        db.collection("products").document(productId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    product = documentSnapshot.toObject(Product.class);
                    if (product != null) {
                        displayData();
                    }
                });
    }

    private void loadProductReviews() {
        if (productId == null) return;
        db.collection("reviews")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    llTopReviewsContainer.removeAllViews();
                    
                    int s5 = 0, s4 = 0, s3 = 0, s2 = 0, s1 = 0;
                    
                    if (count > 0) {
                        float totalRating = 0;
                        List<Review> allReviews = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Review review = doc.toObject(Review.class);
                            float rating = review.getQualityRating();
                            totalRating += rating;
                            allReviews.add(review);
                            
                            if (rating >= 5) s5++;
                            else if (rating >= 4) s4++;
                            else if (rating >= 3) s3++;
                            else if (rating >= 2) s2++;
                            else s1++;
                        }
                        
                        if (pb5 != null) { pb5.setMax(count); pb5.setProgress(s5); }
                        if (pb4 != null) { pb4.setMax(count); pb4.setProgress(s4); }
                        if (pb3 != null) { pb3.setMax(count); pb3.setProgress(s3); }
                        if (pb2 != null) { pb2.setMax(count); pb2.setProgress(s2); }
                        if (pb1 != null) { pb1.setMax(count); pb1.setProgress(s1); }
                        
                        allReviews.sort((r1, r2) -> Float.compare(r2.getQualityRating(), r1.getQualityRating()));
                        
                        int topDisplayCount = Math.min(count, 3);
                        for (int i = 0; i < topDisplayCount; i++) {
                            addReviewToLayout(allReviews.get(i));
                        }

                        float average = totalRating / count;
                        String ratingText = String.format(Locale.getDefault(), "%.1f", average);
                        
                        if (tvRating != null) tvRating.setText(ratingText);
                        if (tvBigRating != null) tvBigRating.setText(ratingText);
                        if (tvReviewCountSubtitle != null) tvReviewCountSubtitle.setText(count + " đánh giá");
                        if (tvTotalReviewsCount != null) tvTotalReviewsCount.setText(count + " đánh giá");
                        if (rbSmallSummary != null) rbSmallSummary.setRating(average);
                    } else {
                        // ... code logic khi không có đánh giá
                    }
                });
    }

    private void addReviewToLayout(Review review) {
        View reviewView = LayoutInflater.from(this).inflate(R.layout.item_review, llTopReviewsContainer, false);
        
        ShapeableImageView ivAvatar = reviewView.findViewById(R.id.ivReviewUserAvatar);
        TextView tvName = reviewView.findViewById(R.id.tvReviewUserName);
        RatingBar rbStars = reviewView.findViewById(R.id.rbReviewStars);
        TextView tvDate = reviewView.findViewById(R.id.tvReviewDate);
        TextView tvComment = reviewView.findViewById(R.id.tvReviewComment);
        ChipGroup cgTags = reviewView.findViewById(R.id.cgReviewTags);

        // LOGIC MỚI: Truy vấn thông tin user mới nhất thay vì dùng dữ liệu cũ trong review
        if (review.getUserEmail() != null) {
            db.collection("users").whereEqualTo("email", review.getUserEmail()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        if (user != null) {
                            // Hiển thị tên mới nhất
                            String displayName = user.getFullName();
                            if (review.isAnonymous() && displayName != null && displayName.length() > 2) {
                                displayName = displayName.charAt(0) + "***" + displayName.charAt(displayName.length() - 1);
                            }
                            tvName.setText(displayName != null ? displayName : "Người dùng Zendo");

                            // Hiển thị avatar mới nhất
                            String avatarData = user.getAvatar();
                            if (avatarData != null && !avatarData.isEmpty()) {
                                if (avatarData.startsWith("http")) {
                                    Glide.with(this).load(avatarData).placeholder(R.drawable.ic_person).into(ivAvatar);
                                } else {
                                    try {
                                        byte[] decodedString = Base64.decode(avatarData, Base64.DEFAULT);
                                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        ivAvatar.setImageBitmap(decodedByte);
                                    } catch (Exception e) {
                                        ivAvatar.setImageResource(R.drawable.ic_person);
                                    }
                                }
                            } else {
                                ivAvatar.setImageResource(R.drawable.ic_person);
                            }
                        }
                    }
                });
        }

        if (rbStars != null) rbStars.setRating(review.getQualityRating());
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (tvDate != null) tvDate.setText(sdf.format(new Date(review.getTimestamp())));
        } catch (Exception e) {
            if (tvDate != null) tvDate.setText("N/A");
        }
        
        if (tvComment != null) tvComment.setText(review.getComment());
        if (cgTags != null) {
            cgTags.removeAllViews();
            if (review.getTags() != null) {
                for (String tag : review.getTags()) {
                    Chip chip = new Chip(this);
                    chip.setText(tag);
                    chip.setChipMinHeight(20f);
                    chip.setTextSize(10f);
                    cgTags.addView(chip);
                }
            }
        }
        
        llTopReviewsContainer.addView(reviewView);
    }

    private void displayData() {
        tvName.setText(product.getName());
        tvPrice.setText(formatter.format(product.getPrice()) + "đ");
        
        if (product.getOldPrice() > 0 && product.getOldPrice() > product.getPrice()) {
            tvOldPrice.setVisibility(View.VISIBLE);
            tvOldPrice.setText(formatter.format(product.getOldPrice()) + "đ");
            tvOldPrice.setPaintFlags(tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tvOldPrice.setVisibility(View.GONE);
        }

        parseDescription(product.getDescription(), tvDesc);
        tvSold.setText("Đã bán " + (product.getSoldCount() > 0 ? product.getSoldCount() : "0"));
        tvStock.setText("Kho: " + product.getStock());
        
        List<String> imageList = new ArrayList<>();
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageList.addAll(product.getImages());
        } else if (product.getImageUrl() != null) {
            imageList.add(product.getImageUrl());
        }

        ImageSliderAdapter adapter = new ImageSliderAdapter(imageList);
        vpImages.setAdapter(adapter);
        
        if (tvImageIndicator != null) tvImageIndicator.setText("1/" + imageList.size());
        vpImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (tvImageIndicator != null) tvImageIndicator.setText((position + 1) + "/" + imageList.size());
            }
        });
    }

    private void toggleCollapse(LinearLayout content, ImageView arrow) {
        if (content.getVisibility() == View.VISIBLE) {
            content.setVisibility(View.GONE);
            arrow.setImageResource(android.R.drawable.arrow_down_float);
        } else {
            content.setVisibility(View.VISIBLE);
            arrow.setImageResource(android.R.drawable.arrow_up_float);
        }
    }

    private void parseDescription(String desc, TextView tvGeneralDesc) {
        if (desc == null || desc.isEmpty()) return;
        
        tvSpecChip.setText("Đang cập nhật");
        tvSpecScreen.setText("Đang cập nhật");
        tvSpecRam.setText("Đang cập nhật");
        tvSpecRom.setText("Đang cập nhật");
        tvSpecPin.setText("Đang cập nhật");
        tvSpecCamera.setText("Đang cập nhật");
        tvSpecOs.setText("Đang cập nhật");

        String[] lines = desc.split("\\r?\\n");
        StringBuilder generalDesc = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            String lowerLine = trimmedLine.toLowerCase();

            if (lowerLine.startsWith("chip:")) {
                tvSpecChip.setText(trimmedLine.substring(5).trim());
            } else if (lowerLine.startsWith("màn hình:")) {
                tvSpecScreen.setText(trimmedLine.substring(9).trim());
            } else if (lowerLine.startsWith("ram:")) {
                tvSpecRam.setText(trimmedLine.substring(4).trim());
            } else if (lowerLine.startsWith("bộ nhớ trong:")) {
                tvSpecRom.setText(trimmedLine.substring(13).trim());
            } else if (lowerLine.startsWith("pin:")) {
                tvSpecPin.setText(trimmedLine.substring(4).trim());
            } else if (lowerLine.startsWith("camera:")) {
                tvSpecCamera.setText(trimmedLine.substring(7).trim());
            } else if (lowerLine.startsWith("hệ điều hành:")) {
                tvSpecOs.setText(trimmedLine.substring(13).trim());
            } else if (!trimmedLine.isEmpty()) {
                generalDesc.append(trimmedLine).append("\n");
            }
        }
        tvGeneralDesc.setText(generalDesc.length() > 0 ? generalDesc.toString().trim() : "Không có mô tả.");
    }

    private void handleBuyNow() {
        if (product == null) return;
        ArrayList<CartItem> checkoutList = new ArrayList<>();
        CartItem tempItem = new CartItem(null, productId, product.getName(), 
                product.getPrice(), product.getImageUrl(), 1, userEmail);
        checkoutList.add(tempItem);
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("checkout_items", checkoutList);
        startActivity(intent);
    }

    private void addToCart() {
        if (product == null || userEmail.isEmpty()) return;
        db.collection("cart")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("cart").document(docId).update("quantity", FieldValue.increment(1));
                    } else {
                        CartItem newItem = new CartItem(null, productId, product.getName(), 
                                product.getPrice(), product.getImageUrl(), 1, userEmail);
                        db.collection("cart").add(newItem);
                    }
                    Toast.makeText(this, "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteProduct())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteProduct() {
        if (productId == null) return;
        db.collection("products").document(productId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
