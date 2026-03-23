package com.example.buoi1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReviewListActivity extends AppCompatActivity {

    private ListView lvReviews;
    private List<Review> reviewList = new ArrayList<>();
    private ReviewAdapter adapter;
    private FirebaseFirestore db;
    private String productId, userRole;
    private Map<String, User> userCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_list);

        db = FirebaseFirestore.getInstance();
        productId = getIntent().getStringExtra("product_id");
        
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userRole = sharedPref.getString("user_role", "user");

        lvReviews = findViewById(R.id.lvReviews);
        findViewById(R.id.btnBackReviews).setOnClickListener(v -> finish());

        adapter = new ReviewAdapter();
        lvReviews.setAdapter(adapter);

        loadReviews();
    }

    private void loadReviews() {
        if (productId == null || productId.isEmpty()) return;

        db.collection("reviews")
                .whereEqualTo("productId", productId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        reviewList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Review review = doc.toObject(Review.class);
                            review.setId(doc.getId()); // GÁN ID Ở ĐÂY
                            reviewList.add(review);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private class ReviewAdapter extends BaseAdapter {
        @Override
        public int getCount() { return reviewList.size(); }
        @Override
        public Object getItem(int i) { return reviewList.get(i); }
        @Override
        public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(ReviewListActivity.this).inflate(R.layout.item_review, viewGroup, false);
            }

            Review review = reviewList.get(i);
            
            ImageView ivAvatar = view.findViewById(R.id.ivReviewUserAvatar);
            TextView tvName = view.findViewById(R.id.tvReviewUserName);
            RatingBar rbStars = view.findViewById(R.id.rbReviewStars);
            TextView tvDate = view.findViewById(R.id.tvReviewDate);
            TextView tvComment = view.findViewById(R.id.tvReviewComment);
            ChipGroup cgTags = view.findViewById(R.id.cgReviewTags);
            
            LinearLayout layoutReply = view.findViewById(R.id.layoutReviewReply);
            TextView tvReplyContent = view.findViewById(R.id.tvReviewReplyContent);
            TextView btnReply = view.findViewById(R.id.btnReplyReviewList);

            // 1. Hiển thị thông tin User
            ivAvatar.setImageResource(R.drawable.ic_launcher_background);
            tvName.setText(review.getUserName() != null ? review.getUserName() : "Người dùng Zendo");

            if (review.isAnonymous()) {
                String anonName = review.getUserName();
                if (anonName != null && anonName.length() > 2) {
                    anonName = anonName.charAt(0) + "***" + anonName.charAt(anonName.length() - 1);
                } else anonName = "Người dùng ẩn danh";
                tvName.setText(anonName);
            } else {
                String email = review.getUserEmail();
                if (email != null && !email.isEmpty()) {
                    if (userCache.containsKey(email)) {
                        updateUserUI(userCache.get(email), ivAvatar, tvName);
                    } else {
                        db.collection("users").whereEqualTo("email", email).get()
                                .addOnSuccessListener(snapshots -> {
                                    if (!snapshots.isEmpty()) {
                                        User user = snapshots.getDocuments().get(0).toObject(User.class);
                                        userCache.put(email, user);
                                        updateUserUI(user, ivAvatar, tvName);
                                    }
                                });
                    }
                }
            }
            
            rbStars.setRating(review.getQualityRating());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(review.getTimestamp())));
            tvComment.setText(review.getComment());

            // 2. Hiển thị Phản hồi của Shop
            if (review.getSellerReply() != null && !review.getSellerReply().isEmpty()) {
                layoutReply.setVisibility(View.VISIBLE);
                tvReplyContent.setText(review.getSellerReply());
            } else {
                layoutReply.setVisibility(View.GONE);
            }

            // 3. Xử lý nút Trả lời cho Admin
            if ("admin".equals(userRole)) {
                btnReply.setVisibility(View.VISIBLE);
                btnReply.setText(review.getSellerReply() == null ? "TRẢ LỜI" : "SỬA PHẢN HỒI");
                btnReply.setOnClickListener(v -> showReplyDialog(review));
            } else {
                btnReply.setVisibility(View.GONE);
            }

            cgTags.removeAllViews();
            if (review.getTags() != null) {
                for (String tag : review.getTags()) {
                    Chip chip = new Chip(ReviewListActivity.this);
                    chip.setText(tag);
                    chip.setChipMinHeight(20f);
                    chip.setTextSize(10f);
                    cgTags.addView(chip);
                }
            }

            return view;
        }

        private void updateUserUI(User user, ImageView ivAvatar, TextView tvName) {
            if (user == null) return;
            if (user.getFullName() != null) tvName.setText(user.getFullName());
            String avatarData = user.getAvatar();
            if (avatarData != null && !avatarData.isEmpty()) {
                if (avatarData.startsWith("http")) Glide.with(ReviewListActivity.this).load(avatarData).into(ivAvatar);
                else {
                    try {
                        byte[] decodedString = Base64.decode(avatarData, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivAvatar.setImageBitmap(bitmap);
                    } catch (Exception e) {}
                }
            }
        }

        private void showReplyDialog(Review review) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ReviewListActivity.this);
            builder.setTitle("Phản hồi đánh giá");
            final EditText input = new EditText(ReviewListActivity.this);
            input.setHint("Nhập nội dung...");
            if (review.getSellerReply() != null) input.setText(review.getSellerReply());
            builder.setView(input);

            builder.setPositiveButton("Gửi", (dialog, which) -> {
                String reply = input.getText().toString().trim();
                if (reply.isEmpty()) return;
                
                if (review.getId() == null) {
                    Toast.makeText(ReviewListActivity.this, "Lỗi: Không tìm thấy ID đánh giá", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("sellerReply", reply);
                updates.put("replyTimestamp", System.currentTimeMillis());
                
                db.collection("reviews").document(review.getId()).update(updates)
                        .addOnSuccessListener(aVoid -> Toast.makeText(ReviewListActivity.this, "Đã gửi phản hồi thành công", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(ReviewListActivity.this, "Lỗi khi gửi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
            builder.setNegativeButton("Hủy", null);
            builder.show();
        }
    }
}
