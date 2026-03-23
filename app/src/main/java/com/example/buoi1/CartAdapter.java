package com.example.buoi1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DecimalFormat;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItemList;
    private OnCartItemChangeListener listener;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    public interface OnCartItemChangeListener {
        void onQuantityChange(CartItem item, int newQuantity);
        void onSelectionChange();
    }

    public CartAdapter(List<CartItem> cartItemList, OnCartItemChangeListener listener) {
        this.cartItemList = cartItemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItemList.get(position);
        holder.tvName.setText(item.getProductName());
        holder.tvPrice.setText(formatter.format(item.getProductPrice()) + "đ");
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(item.isSelected());

        // Xử lý hiển thị hình ảnh (hỗ trợ cả URL và Base64)
        String imgData = item.getProductImageUrl();
        if (imgData != null && !imgData.isEmpty()) {
            if (imgData.startsWith("http")) {
                Glide.with(holder.itemView.getContext()).load(imgData).into(holder.ivProduct);
            } else {
                try {
                    byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivProduct.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    holder.ivProduct.setImageResource(R.drawable.ic_launcher_background);
                }
            }
        } else {
            holder.ivProduct.setImageResource(R.drawable.ic_launcher_background);
        }

        // Sự kiện click vào item để xem chi tiết sản phẩm
        holder.itemView.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("products").document(item.getProductId()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Product product = documentSnapshot.toObject(Product.class);
                            if (product != null) {
                                product.setId(documentSnapshot.getId());
                                Intent intent = new Intent(v.getContext(), DetailActivity.class);
                                intent.putExtra("product_data", product);
                                v.getContext().startActivity(intent);
                            }
                        } else {
                            Toast.makeText(v.getContext(), "Sản phẩm không còn tồn tại", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(v.getContext(), "Lỗi khi tải thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                    });
        });

        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setSelected(isChecked);
            listener.onSelectionChange();
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                listener.onQuantityChange(item, item.getQuantity() - 1);
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            listener.onQuantityChange(item, item.getQuantity() + 1);
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        CheckBox cbSelect;
        TextView tvName, tvPrice, tvQuantity, btnMinus, btnPlus;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivCartProduct);
            cbSelect = itemView.findViewById(R.id.cbSelectItem);
            tvName = itemView.findViewById(R.id.tvCartProductName);
            tvPrice = itemView.findViewById(R.id.tvCartProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
        }
    }
}
