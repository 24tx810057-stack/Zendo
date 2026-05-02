package com.example.buoi1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.List;

public class ProductAdapter extends BaseAdapter {
    private Context context;
    private List<Product> productList;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private DecimalFormat ratingFormatter = new DecimalFormat("0.0");
    private String userEmail, userRole;
    private FirebaseFirestore db;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        this.userEmail = sharedPref.getString("user_email", "");
        this.userRole = sharedPref.getString("user_role", "user");
    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public Object getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        }

        Product product = productList.get(position);

        ImageView ivProductImage = convertView.findViewById(R.id.ivProductImage);
        TextView tvProductName = convertView.findViewById(R.id.tvProductName);
        TextView tvProductPrice = convertView.findViewById(R.id.tvProductPrice);
        TextView tvProductOldPrice = convertView.findViewById(R.id.tvProductOldPrice);
        TextView tvProductDiscount = convertView.findViewById(R.id.tvProductDiscount);
        TextView tvRating = convertView.findViewById(R.id.tvProductRating);
        TextView tvSold = convertView.findViewById(R.id.tvProductSold);
        ImageView btnLike = convertView.findViewById(R.id.btnLike);
        
        ImageView ivProductStar = convertView.findViewById(R.id.ivProductStar);
        View vRatingDivider = convertView.findViewById(R.id.vProductRatingDivider);

        tvProductName.setText(product.getName());
        
        // Hiển thị giá và giảm giá dựa trên sự chênh lệch Giá Gốc và Giá Hiện Tại của Sản phẩm
        tvProductPrice.setText(formatter.format(product.getPrice()) + "đ");
        
        if (product.getOldPrice() > 0 && product.getOldPrice() > product.getPrice()) {
            tvProductOldPrice.setVisibility(View.VISIBLE);
            tvProductOldPrice.setText(formatter.format(product.getOldPrice()) + "đ");
            tvProductOldPrice.setPaintFlags(tvProductOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            
            tvProductDiscount.setVisibility(View.VISIBLE);
            int discount = (int) ((1 - (product.getPrice() / (float)product.getOldPrice())) * 100);
            tvProductDiscount.setText("-" + discount + "%");
        } else {
            tvProductOldPrice.setVisibility(View.INVISIBLE);
            tvProductDiscount.setVisibility(View.INVISIBLE);
        }
        
        ivProductStar.setVisibility(View.GONE);
        tvRating.setVisibility(View.GONE);
        vRatingDivider.setVisibility(View.GONE);

        if (product.getId() != null) {
            db.collection("reviews")
                    .whereEqualTo("productId", product.getId())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            double totalRating = 0;
                            int count = queryDocumentSnapshots.size();
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Double ratingVal = doc.getDouble("qualityRating");
                                if (ratingVal != null) totalRating += ratingVal;
                            }
                            double average = totalRating / count;
                            
                            ivProductStar.setVisibility(View.VISIBLE);
                            tvRating.setVisibility(View.VISIBLE);
                            vRatingDivider.setVisibility(View.VISIBLE);
                            tvRating.setText(ratingFormatter.format(average));
                        }
                    });
        }

        tvSold.setText("Đã bán " + (product.getSoldCount() >= 0 ? product.getSoldCount() : "0"));

        // LOGIC LÀM MỜ SẢN PHẨM HẾT HÀNG CHO ADMIN
        if ("admin".equals(userRole) && product.getStock() <= 0) {
            convertView.setAlpha(0.4f);
            tvProductName.setText("[HẾT HÀNG] " + product.getName());
            tvProductName.setTextColor(0xFFD32F2F); // Màu đỏ cảnh báo
        } else {
            convertView.setAlpha(1.0f);
            tvProductName.setTextColor(0xFF212121); // Màu đen bình thường
        }

        boolean isLiked = product.isLiked(userEmail);
        btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        btnLike.setColorFilter(ContextCompat.getColor(context, isLiked ? android.R.color.holo_red_light : android.R.color.darker_gray));

        btnLike.setOnClickListener(v -> {
            if (userEmail.isEmpty()) return;
            if (isLiked) {
                db.collection("products").document(product.getId())
                        .update("likedBy", FieldValue.arrayRemove(userEmail));
            } else {
                db.collection("products").document(product.getId())
                        .update("likedBy", FieldValue.arrayUnion(userEmail));
            }
        });

        String imgData = product.getImageUrl();
        if (imgData != null && !imgData.isEmpty()) {
            if (imgData.startsWith("http")) {
                Glide.with(context).load(imgData).into(ivProductImage);
            } else {
                try {
                    byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ivProductImage.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    ivProductImage.setImageResource(R.drawable.ic_launcher_background);
                }
            }
        } else {
            ivProductImage.setImageResource(R.drawable.ic_launcher_background);
        }

        return convertView;
    }
}
