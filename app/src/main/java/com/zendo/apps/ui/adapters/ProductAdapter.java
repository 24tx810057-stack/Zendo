package com.zendo.apps.ui.adapters;

import com.zendo.apps.R;

import com.zendo.apps.utils.SharedPrefManager;

import com.zendo.apps.data.models.Product;

import android.content.Context;
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
        SharedPrefManager prefManager = SharedPrefManager.getInstance(context);
        this.userEmail = prefManager.getUserEmail();
        this.userRole = prefManager.getUserRole();
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

        TextView tvProductDiscountBadge = convertView.findViewById(R.id.tvProductDiscountBadge);

        tvProductName.setText(product.getName());
        tvProductPrice.setText(formatter.format(product.getPrice()) + "đ");
        
        if (product.getOldPrice() > 0 && product.getOldPrice() > product.getPrice()) {
            tvProductOldPrice.setVisibility(View.VISIBLE);
            tvProductOldPrice.setText(formatter.format(product.getOldPrice()) + "đ");
            tvProductOldPrice.setPaintFlags(tvProductOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            
            int discount = (int) ((1 - (product.getPrice() / (float)product.getOldPrice())) * 100);
            tvProductDiscount.setVisibility(View.VISIBLE);
            tvProductDiscount.setText("-" + discount + "%");
            
            // Hiện tem chéo mới
            tvProductDiscountBadge.setVisibility(View.VISIBLE);
            tvProductDiscountBadge.setText("-" + discount + "%");
        } else {
            tvProductOldPrice.setVisibility(View.INVISIBLE);
            tvProductDiscount.setVisibility(View.INVISIBLE);
            tvProductDiscountBadge.setVisibility(View.GONE);
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

        if ("admin".equals(userRole) && product.getStock() <= 0) {
            convertView.setAlpha(0.4f);
            tvProductName.setText("[HẾT HÀNG] " + product.getName());
            tvProductName.setTextColor(0xFFD32F2F);
        } else {
            convertView.setAlpha(1.0f);
            tvProductName.setTextColor(0xFF212121);
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
                Glide.with(context)
                        .load(imgData)
                        .placeholder(R.drawable.bg_border_gray_light)
                        .into(ivProductImage);
            } else {
                try {
                    byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                    Glide.with(context)
                            .load(decodedString)
                            .placeholder(R.drawable.bg_border_gray_light)
                            .into(ivProductImage);
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



