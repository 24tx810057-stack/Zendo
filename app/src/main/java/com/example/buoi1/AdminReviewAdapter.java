package com.example.buoi1;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Phản hồi đánh giá");

        final EditText input = new EditText(context);
        input.setHint("Nhập nội dung phản hồi...");
        if (review.getSellerReply() != null) {
            input.setText(review.getSellerReply());
        }
        
        // Dùng FrameLayout để thêm padding cho EditText trong Dialog (tránh dính lề)
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String reply = input.getText().toString().trim();
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
                        if (context != null) {
                            Toast.makeText(context, "Đã gửi phản hồi", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (context != null) {
                            Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
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
