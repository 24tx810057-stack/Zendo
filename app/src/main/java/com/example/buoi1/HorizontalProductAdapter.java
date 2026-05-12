package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.util.List;

public class HorizontalProductAdapter extends RecyclerView.Adapter<HorizontalProductAdapter.ViewHolder> {

    private Context context;
    private List<Product> productList;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    public HorizontalProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        
        // Điều chỉnh chiều rộng để phù hợp với hiển thị nằm ngang (Horizontal Scroll)
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = (int) (160 * context.getResources().getDisplayMetrics().density);
        view.setLayoutParams(params);
        
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvName.setText(product.getName());
        holder.tvPrice.setText(formatter.format(product.getPrice()) + "đ");
        holder.tvSold.setText("Đã bán " + product.getSoldCount());

        // Ẩn bớt các view không cần thiết để tiết kiệm diện tích nếu cần
        holder.btnLike.setVisibility(View.GONE);

        String imgData = product.getImageUrl();
        if (imgData != null && !imgData.isEmpty()) {
            if (imgData.startsWith("http")) {
                Glide.with(context).load(imgData).into(holder.ivProduct);
            } else {
                try {
                    byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivProduct.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    holder.ivProduct.setImageResource(R.drawable.ic_launcher_background);
                }
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("product_data", product);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct, btnLike;
        TextView tvName, tvPrice, tvSold;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivProductImage);
            btnLike = itemView.findViewById(R.id.btnLike);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvSold = itemView.findViewById(R.id.tvProductSold);
        }
    }
}
