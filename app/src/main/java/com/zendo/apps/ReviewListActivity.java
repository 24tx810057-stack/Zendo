package com.zendo.apps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zendo.apps.databinding.ActivityReviewListBinding;
import com.zendo.apps.databinding.ItemReviewBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReviewListActivity extends AppCompatActivity {

    private ActivityReviewListBinding binding;
    private final List<Review> reviewList = new ArrayList<>();
    private ReviewAdapter adapter;
    private FirebaseFirestore db;
    private String productId, userRole;
    private final Map<String, User> userCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReviewListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        productId = getIntent().getStringExtra("product_id");
        
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userRole = sharedPref.getString("user_role", "user");

        binding.btnBackReviews.setOnClickListener(v -> finish());

        adapter = new ReviewAdapter();
        binding.lvReviews.setAdapter(adapter);

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
                            review.setId(doc.getId());
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
            ItemReviewBinding itemBinding;
            if (view == null) {
                itemBinding = ItemReviewBinding.inflate(LayoutInflater.from(ReviewListActivity.this), viewGroup, false);
                view = itemBinding.getRoot();
                view.setTag(itemBinding);
            } else {
                itemBinding = (ItemReviewBinding) view.getTag();
            }

            Review review = reviewList.get(i);
            
            // Reset UI state for recycled view
            itemBinding.ivReviewUserAvatar.setImageResource(R.drawable.ic_person);
            itemBinding.ivReviewUserAvatar.setTag(null);
            itemBinding.tvReviewUserName.setText(review.getUserName() != null ? review.getUserName() : "Người dùng Zendo");

            if (review.isAnonymous()) {
                String anonName = review.getUserName();
                if (anonName != null && anonName.length() > 2) {
                    anonName = anonName.charAt(0) + "***" + anonName.charAt(anonName.length() - 1);
                } else anonName = "Người dùng ẩn danh";
                itemBinding.tvReviewUserName.setText(anonName);
                itemBinding.ivReviewUserAvatar.setImageResource(R.drawable.ic_person);
            } else {
                String email = review.getUserEmail();
                if (email != null && !email.isEmpty()) {
                    if (userCache.containsKey(email)) {
                        updateUserUI(userCache.get(email), itemBinding.ivReviewUserAvatar, itemBinding.tvReviewUserName);
                    } else {
                        itemBinding.ivReviewUserAvatar.setTag(email);
                        db.collection("users").whereEqualTo("email", email).get()
                                .addOnSuccessListener(snapshots -> {
                                    if (!snapshots.isEmpty()) {
                                        User user = snapshots.getDocuments().get(0).toObject(User.class);
                                        userCache.put(email, user);
                                        // Ensure we're updating the correct view after async fetch
                                        if (email.equals(itemBinding.ivReviewUserAvatar.getTag())) {
                                            updateUserUI(user, itemBinding.ivReviewUserAvatar, itemBinding.tvReviewUserName);
                                        }
                                    }
                                });
                    }
                }
            }
            
            itemBinding.rbReviewStars.setRating(review.getQualityRating());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            itemBinding.tvReviewDate.setText(sdf.format(new Date(review.getTimestamp())));
            
            if (review.getComment() != null && !review.getComment().trim().isEmpty()) {
                itemBinding.tvReviewComment.setText(review.getComment());
                itemBinding.tvReviewComment.setVisibility(View.VISIBLE);
            } else {
                itemBinding.tvReviewComment.setVisibility(View.GONE);
            }

            if (review.getSellerReply() != null && !review.getSellerReply().isEmpty()) {
                itemBinding.layoutReviewReply.setVisibility(View.VISIBLE);
                itemBinding.tvReviewReplyContent.setText(review.getSellerReply());
            } else {
                itemBinding.layoutReviewReply.setVisibility(View.GONE);
            }

            if ("admin".equals(userRole)) {
                itemBinding.btnReplyReviewList.setVisibility(View.VISIBLE);
                itemBinding.btnReplyReviewList.setText(review.getSellerReply() == null ? "TRẢ LỜI" : "SỬA PHẢN HỒI");
                itemBinding.btnReplyReviewList.setOnClickListener(v -> showReplyDialog(review));
            } else {
                itemBinding.btnReplyReviewList.setVisibility(View.GONE);
            }

            // Fix tags trống
            itemBinding.cgReviewTags.removeAllViews();
            if (review.getTags() != null && !review.getTags().isEmpty()) {
                itemBinding.cgReviewTags.setVisibility(View.VISIBLE);
                for (String tag : review.getTags()) {
                    Chip chip = new Chip(ReviewListActivity.this);
                    chip.setText(tag);
                    chip.setChipMinHeight(20f);
                    chip.setTextSize(10f);
                    itemBinding.cgReviewTags.addView(chip);
                }
            } else {
                itemBinding.cgReviewTags.setVisibility(View.GONE);
            }

            return view;
        }

        private void updateUserUI(User user, ImageView ivAvatar, TextView tvName) {
            if (user == null) return;
            if (user.getFullName() != null) tvName.setText(user.getFullName());
            String avatarData = user.getAvatar();
            if (avatarData != null && !avatarData.isEmpty()) {
                if (avatarData.startsWith("http")) {
                    Glide.with(ReviewListActivity.this).load(avatarData).placeholder(R.drawable.ic_person).into(ivAvatar);
                } else {
                    try {
                        byte[] decodedString = Base64.decode(avatarData, Base64.DEFAULT);
                        Glide.with(ReviewListActivity.this).load(decodedString).into(ivAvatar);
                    } catch (Exception e) {
                        ivAvatar.setImageResource(R.drawable.ic_person);
                    }
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }
        }

        private void showReplyDialog(Review review) {
            View dialogView = LayoutInflater.from(ReviewListActivity.this).inflate(R.layout.dialog_reply_review, null);
            AlertDialog dialog = new AlertDialog.Builder(ReviewListActivity.this, R.style.CustomDialogTheme).setView(dialogView).create();

            TextView tvOriginal = dialogView.findViewById(R.id.tvOriginalReview);
            TextInputEditText etReply = dialogView.findViewById(R.id.etReplyContent);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnSend = dialogView.findViewById(R.id.btnSend);
            ChipGroup cgQuickReplies = dialogView.findViewById(R.id.cgQuickReplies);

            tvOriginal.setText(review.getComment());
            if (review.getSellerReply() != null) {
                etReply.setText(review.getSellerReply());
            }

            for (int i = 0; i < cgQuickReplies.getChildCount(); i++) {
                View child = cgQuickReplies.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    chip.setOnClickListener(v -> {
                        etReply.setText(chip.getText());
                        etReply.setSelection(etReply.getText().length());
                    });
                }
            }

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnSend.setOnClickListener(v -> {
                String reply = etReply.getText().toString().trim();
                if (reply.isEmpty()) {
                    Toast.makeText(ReviewListActivity.this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (review.getId() == null) {
                    Toast.makeText(ReviewListActivity.this, "Lỗi: Không tìm thấy ID đánh giá", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("sellerReply", reply);
                updates.put("replyTimestamp", System.currentTimeMillis());
                
                db.collection("reviews").document(review.getId()).update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ReviewListActivity.this, "Đã gửi phản hồi thành công", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(ReviewListActivity.this, "Lỗi khi gửi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });

            dialog.show();

            if (dialog.getWindow() != null) {
                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }
}
