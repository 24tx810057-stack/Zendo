package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());
        holder.tvDate.setText(notification.getDate());

        // Nếu chưa đọc thì in đậm và đổi màu nền nhẹ
        if (!notification.isRead()) {
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.tvMessage.setTypeface(null, Typeface.BOLD);
            holder.itemView.setBackgroundColor(Color.parseColor("#F0F9FF"));
        } else {
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.tvMessage.setTypeface(null, Typeface.NORMAL);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        // Click vào thông báo
        holder.itemView.setOnClickListener(v -> {
            Context context = holder.itemView.getContext();
            
            // Đánh dấu là đã đọc
            if (!notification.isRead()) {
                notification.setRead(true);
                notifyItemChanged(position);
                FirebaseFirestore.getInstance().collection("notifications")
                        .document(notification.getId())
                        .update("read", true);
            }

            // ĐIỀU HƯỚNG THÔNG MINH
            String type = notification.getType();
            String orderId = notification.getOrderId();

            if (orderId != null && !orderId.isEmpty()) {
                if ("order_status".equals(type) || "admin_order".equals(type)) {
                    // Mở trang chi tiết đơn hàng
                    FirebaseFirestore.getInstance().collection("orders").document(orderId).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Order order = documentSnapshot.toObject(Order.class);
                                    if (order != null) {
                                        order.setId(documentSnapshot.getId());
                                        Intent intent = new Intent(context, OrderDetailActivity.class);
                                        intent.putExtra("order_data", order);
                                        context.startActivity(intent);
                                    }
                                } else {
                                    Toast.makeText(context, "Đơn hàng không còn tồn tại", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else if ("return_status".equals(type)) {
                    // Mở trang Yêu cầu trả hàng (cho user) - Chuyển đúng Tab Trả hàng
                    Intent intent = new Intent(context, OrderListActivity.class);
                    intent.putExtra("tab_index", 5); // Giả định Tab Trả hàng/Hoàn tiền ở index 5
                    context.startActivity(intent);
                } else if ("warranty".equals(type)) {
                    // Mở trang chi tiết đơn hàng để user xem bảo hành
                    FirebaseFirestore.getInstance().collection("orders").document(orderId).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Order order = documentSnapshot.toObject(Order.class);
                                    if (order != null) {
                                        order.setId(documentSnapshot.getId());
                                        Intent intent = new Intent(context, OrderDetailActivity.class);
                                        intent.putExtra("order_data", order);
                                        context.startActivity(intent);
                                    }
                                }
                            });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvDate = itemView.findViewById(R.id.tvNotifDate);
        }
    }
}
