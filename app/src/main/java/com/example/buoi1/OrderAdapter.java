package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DecimalFormat;
import java.util.List;

public class OrderAdapter extends BaseAdapter {
    private Context context;
    private List<Order> orderList;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userRole;

    public OrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
        SharedPreferences sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        this.userRole = sharedPref.getString("user_role", "user");
    }

    @Override
    public int getCount() { return orderList.size(); }

    @Override
    public Object getItem(int position) { return orderList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_order_list, parent, false);
        }

        final Order order = orderList.get(position);

        View cardOrder = convertView.findViewById(R.id.cardOrder);
        TextView tvStatus = convertView.findViewById(R.id.tvOrderListStatus);
        ImageView ivProduct = convertView.findViewById(R.id.ivOrderFirstProduct);
        TextView tvProductName = convertView.findViewById(R.id.tvOrderFirstProductName);
        TextView tvQuantity = convertView.findViewById(R.id.tvOrderTotalQuantity);
        TextView tvTotal = convertView.findViewById(R.id.tvOrderListTotal);
        Button btnAction = convertView.findViewById(R.id.btnOrderAction);
        Button btnDetail = convertView.findViewById(R.id.btnOrderDetail);

        tvStatus.setText(order.getStatus());
        tvTotal.setText(formatter.format(order.getTotalAmount()) + "đ");

        // Sự kiện click Toàn bộ ô hoặc Nút Chi tiết -> Màn hình Chi tiết đơn hàng
        View.OnClickListener goToDetail = v -> {
            Intent intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("order_data", order);
            context.startActivity(intent);
        };
        cardOrder.setOnClickListener(goToDetail);
        btnDetail.setOnClickListener(goToDetail);

        // LOGIC PHÂN QUYỀN HIỂN THỊ NÚT
        if ("admin".equals(userRole)) {
            // Admin chỉ xem chi tiết, không đánh giá/mua lại
            btnAction.setVisibility(View.GONE);
        } else {
            // User: Hiển thị logic Đánh giá / Mua lại
            btnAction.setVisibility(View.VISIBLE);
            if ("Đã giao".equals(order.getStatus())) {
                btnAction.setText("..."); // Loading state
                db.collection("reviews")
                        .whereEqualTo("orderId", order.getId())
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                // Đã đánh giá -> Chuyển thành nút Mua lại
                                btnAction.setText("Mua lại");
                                btnAction.setOnClickListener(v -> goToProductDetail(order));
                            } else {
                                // Chưa đánh giá -> Nút Đánh giá
                                btnAction.setText("Đánh giá");
                                btnAction.setOnClickListener(v -> {
                                    Intent intent = new Intent(context, OrderDetailActivity.class);
                                    intent.putExtra("order_data", order);
                                    intent.putExtra("trigger_review", true);
                                    context.startActivity(intent);
                                });
                            }
                        });
            } else {
                // Các trạng thái khác vẫn hiện nút Mua lại cho User
                btnAction.setText("Mua lại");
                btnAction.setOnClickListener(v -> goToProductDetail(order));
            }
        }

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            CartItem firstItem = order.getItems().get(0);
            tvProductName.setText(firstItem.getProductName());
            tvQuantity.setText("x" + firstItem.getQuantity() + " sản phẩm" + (order.getItems().size() > 1 ? " (và " + (order.getItems().size()-1) + " sp khác)" : ""));

            String imgData = firstItem.getProductImageUrl();
            if (imgData != null && !imgData.isEmpty()) {
                if (imgData.startsWith("http")) Glide.with(context).load(imgData).into(ivProduct);
                else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivProduct.setImageBitmap(bitmap);
                    } catch (Exception e) {}
                }
            }
        }

        return convertView;
    }

    private void goToProductDetail(Order order) {
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            String productId = order.getItems().get(0).getProductId();
            db.collection("products").document(productId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Product p = documentSnapshot.toObject(Product.class);
                        if (p != null) {
                            Intent intent = new Intent(context, DetailActivity.class);
                            intent.putExtra("product_data", p);
                            context.startActivity(intent);
                        } else {
                            Toast.makeText(context, "Sản phẩm không còn tồn tại", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show());
        }
    }
}
