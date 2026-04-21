package com.example.buoi1;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> reviewList;
    private FirebaseFirestore db;

    public AdminReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        holder.tvUser.setText("Người mua: " + (review.isAnonymous() ? "Ẩn danh" : review.getUserName()));
        holder.tvContent.setText(review.getComment());
        holder.rbStars.setRating(review.getQualityRating());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(review.getTimestamp())));

        // Hiển thị phản hồi nếu có
        if (review.getSellerReply() != null && !review.getSellerReply().isEmpty()) {
            holder.layoutReply.setVisibility(View.VISIBLE);
            holder.tvReplyContent.setText(review.getSellerReply());
            holder.btnReply.setText("SỬA PHẢN HỒI");
        } else {
            holder.layoutReply.setVisibility(View.GONE);
            holder.btnReply.setText("TRẢ LỜI");
        }

        holder.btnReply.setOnClickListener(v -> showReplyDialog(review));

        // Load Product Info
        db.collection("products").document(review.getProductId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Product product = documentSnapshot.toObject(Product.class);
                    if (product != null) {
                        holder.tvProductName.setText(product.getName());
                        String imgData = product.getImageUrl();
                        if (imgData != null && !imgData.isEmpty()) {
                            if (imgData.startsWith("http")) Glide.with(context).load(imgData).into(holder.ivProductImg);
                            else {
                                try {
                                    byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    holder.ivProductImg.setImageBitmap(bitmap);
                                } catch (Exception e) {}
                            }
                        }
                    }
                });
    }

    private void showReplyDialog(Review review) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reply_review, null);
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.CustomDialogTheme).setView(dialogView).create();

        TextView tvOriginal = dialogView.findViewById(R.id.tvOriginalReview);
        TextInputEditText etReply = dialogView.findViewById(R.id.etReplyContent);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSend = dialogView.findViewById(R.id.btnSend);
        ChipGroup cgQuickReplies = dialogView.findViewById(R.id.cgQuickReplies);

        tvOriginal.setText(review.getComment());
        if (review.getSellerReply() != null) {
            etReply.setText(review.getSellerReply());
        }

        // Xử lý khi bấm vào các mẫu trả lời nhanh
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
                Toast.makeText(context, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("sellerReply", reply);
            updates.put("replyTimestamp", System.currentTimeMillis());

            db.collection("reviews").document(review.getId())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Đã gửi phản hồi", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
        
        // Ép Dialog rộng ra 90% màn hình
        if (dialog.getWindow() != null) {
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivProductImg;
        TextView tvProductName, tvUser, tvContent, tvDate, tvReplyContent, btnReply;
        RatingBar rbStars;
        LinearLayout layoutReply;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImg = itemView.findViewById(R.id.ivReviewProductImg);
            tvProductName = itemView.findViewById(R.id.tvReviewProductName);
            tvUser = itemView.findViewById(R.id.tvReviewUser);
            tvContent = itemView.findViewById(R.id.tvReviewContent);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            rbStars = itemView.findViewById(R.id.rbReviewStars);
            
            layoutReply = itemView.findViewById(R.id.layoutSellerReply);
            tvReplyContent = itemView.findViewById(R.id.tvSellerReplyContent);
            btnReply = itemView.findViewById(R.id.btnReplyReview);
        }
    }
}
