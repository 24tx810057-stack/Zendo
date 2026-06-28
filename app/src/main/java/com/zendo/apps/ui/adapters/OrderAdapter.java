package com.zendo.apps.ui.adapters;

import com.zendo.apps.ui.activities.OrderDetailActivity;

import com.zendo.apps.ui.activities.DetailActivity;

import com.zendo.apps.utils.SharedPrefManager;

import com.zendo.apps.data.models.CartItem;

import com.zendo.apps.data.models.Product;

import com.zendo.apps.data.models.Order;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zendo.apps.databinding.ItemOrderListBinding;
import java.text.DecimalFormat;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private final Context context;
    private final List<Order> orderList;
    private final DecimalFormat formatter = new DecimalFormat("###,###,###");
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userRole;

    public OrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
        this.userRole = SharedPrefManager.getInstance(context).getUserRole();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderListBinding binding = ItemOrderListBinding.inflate(LayoutInflater.from(context), parent, false);
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        ItemOrderListBinding binding = holder.binding;

        binding.tvOrderListStatus.setText(order.getStatus());
        binding.tvOrderListTotal.setText(formatter.format(order.getTotalAmount()) + "đ");

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            CartItem firstItem = order.getItems().get(0);
            binding.tvOrderFirstProductName.setText(firstItem.getProductName());
            binding.tvOrderTotalQuantity.setText("x" + firstItem.getQuantity() + " sản phẩm" + (order.getItems().size() > 1 ? " (và " + (order.getItems().size() - 1) + " khác)" : ""));

            String imgData = firstItem.getProductImageUrl();
            if (imgData != null && !imgData.isEmpty()) {
                if (imgData.startsWith("http")) {
                    Glide.with(context).load(imgData).into(binding.ivOrderFirstProduct);
                } else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        Glide.with(context).load(decodedString).into(binding.ivOrderFirstProduct);
                    } catch (Exception e) {}
                }
            }
        }

        // Action Buttons logic
        binding.btnOrderDetail.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("order_data", order);
            context.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("order_data", order);
            context.startActivity(intent);
        });

        if ("Hoàn thành".equals(order.getStatus()) && !"admin".equals(userRole)) {
            binding.btnOrderAction.setVisibility(View.VISIBLE);
            // Re-constrain Detail button to the left of Action button
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = 
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) binding.btnOrderDetail.getLayoutParams();
            params.endToStart = binding.btnOrderAction.getId();
            params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
            binding.btnOrderDetail.setLayoutParams(params);

            if (order.isReviewed()) {
                binding.btnOrderAction.setText("Mua lại");
                binding.btnOrderAction.setOnClickListener(v -> goToProductDetail(order));
            } else {
                binding.btnOrderAction.setText("Đánh giá");
                binding.btnOrderAction.setOnClickListener(v -> {
                    if (order.getItems() != null && !order.getItems().isEmpty()) {
                        Intent intent = new Intent(context, com.zendo.apps.ui.activities.AddReviewActivity.class);
                        intent.putExtra("order_id", order.getId());
                        intent.putExtra("product_id", order.getItems().get(0).getProductId());
                        intent.putExtra("cart_item", order.getItems().get(0));
                        context.startActivity(intent);
                    }
                });
            }
        } else {
            binding.btnOrderAction.setVisibility(View.GONE);
            // Re-constrain Detail button to the end of parent
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = 
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) binding.btnOrderDetail.getLayoutParams();
            params.endToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
            params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
            binding.btnOrderDetail.setLayoutParams(params);
        }
    }

    private void goToProductDetail(Order order) {
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            String pId = order.getItems().get(0).getProductId();
            db.collection("products").document(pId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Product p = doc.toObject(Product.class);
                    if (p != null) {
                        p.setId(doc.getId());
                        Intent intent = new Intent(context, DetailActivity.class);
                        intent.putExtra("product_id", p.getId());
                        intent.putExtra("product_data", p);
                        context.startActivity(intent);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public void updateList(List<Order> newList) {
        orderList.clear();
        if (newList != null) {
            orderList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        final ItemOrderListBinding binding;
        public OrderViewHolder(ItemOrderListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}




