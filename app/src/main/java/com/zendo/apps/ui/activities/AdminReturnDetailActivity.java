package com.zendo.apps.ui.activities;

import com.zendo.apps.R;

import com.zendo.apps.data.models.ReturnRequest;

import com.zendo.apps.data.models.CartItem;

import com.zendo.apps.data.models.Order;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zendo.apps.databinding.ActivityAdminReturnDetailBinding;
import java.text.DecimalFormat;

public class AdminReturnDetailActivity extends AppCompatActivity {

    private ActivityAdminReturnDetailBinding binding;
    private ReturnRequest returnRequest;
    private FirebaseFirestore db;
    private final DecimalFormat formatter = new DecimalFormat("###,###,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReturnDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        returnRequest = (ReturnRequest) getIntent().getSerializableExtra("return_data");

        if (returnRequest == null) {
            finish();
            return;
        }

        initViews();
        displayData();
    }

    private void initViews() {
        binding.btnBackReturnDetail.setOnClickListener(v -> finish());

        binding.btnApproveReturn.setOnClickListener(v -> showConfirmDialog(true));
        binding.btnRejectReturn.setOnClickListener(v -> showConfirmDialog(false));
        
        // Nếu đã xử lý rồi thì ẩn nút
        if (!"pending".equals(returnRequest.getStatus())) {
            binding.layoutAdminReturnActions.setVisibility(View.GONE);
        }
    }

    private void displayData() {
        binding.tvDetailReturnOrderId.setText("Mã đơn hàng: " + returnRequest.getOrderId());
        binding.tvDetailReturnUser.setText("Người gửi: " + returnRequest.getUserEmail());
        binding.tvDetailReturnAmount.setText("Số tiền yêu cầu hoàn: " + formatter.format(returnRequest.getRefundAmount()) + "đ");
        binding.tvDetailReturnReason.setText(returnRequest.getReason());
        binding.tvDetailReturnDesc.setText(returnRequest.getDescription());

        if (returnRequest.getEvidenceImages() != null) {
            binding.layoutDetailEvidenceImages.removeAllViews();
            for (String imgData : returnRequest.getEvidenceImages()) {
                ImageView iv = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 800);
                lp.setMargins(0, 0, 0, 20);
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iv.setBackgroundResource(R.drawable.bg_border_gray_light);
                
                if (imgData.startsWith("http")) {
                    Glide.with(this).load(imgData).into(iv);
                } else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        Glide.with(this).load(decodedString).into(iv);
                    } catch (Exception e) {}
                }
                
                binding.layoutDetailEvidenceImages.addView(iv);
            }
        }
    }

    private void showConfirmDialog(boolean isApprove) {
        String title = isApprove ? "Duyệt hoàn tiền" : "Từ chối khiếu nại";
        String message = isApprove ? "Bạn có chắc chắn muốn duyệt hoàn tiền cho đơn hàng này?" 
                                   : "Bạn có chắc chắn muốn từ chối yêu cầu trả hàng này?";
        
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    if (isApprove) approveReturn();
                    else rejectReturn();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void approveReturn() {
        db.collection("return_requests").document(returnRequest.getId())
                .update("status", "approved")
                .addOnSuccessListener(aVoid -> {
                    // Lấy thông tin đơn hàng để hoàn lại kho và giảm lượt bán
                    db.collection("orders").document(returnRequest.getOrderId()).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                Order order = documentSnapshot.toObject(Order.class);
                                if (order != null && order.getItems() != null) {
                                    for (CartItem item : order.getItems()) {
                                        if (item.getProductId() != null) {
                                            // Hoàn lại kho
                                            db.collection("products").document(item.getProductId())
                                                    .update("stock", com.google.firebase.firestore.FieldValue.increment(item.getQuantity()));
                                            
                                            // Giảm lượt bán (vì đã trả hàng)
                                            db.collection("products").document(item.getProductId())
                                                    .update("soldCount", com.google.firebase.firestore.FieldValue.increment(-item.getQuantity()));
                                        }
                                    }
                                }

                                // Cập nhật trạng thái đơn hàng
                                db.collection("orders").document(returnRequest.getOrderId())
                                        .update("status", "Đã hoàn tiền")
                                        .addOnSuccessListener(aVoid2 -> {
                                            sendNotification(true);
                                            Toast.makeText(this, "Đã duyệt hoàn tiền và hoàn lại kho thành công!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                            });
                });
    }

    private void rejectReturn() {
        db.collection("return_requests").document(returnRequest.getId())
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> {
                    db.collection("orders").document(returnRequest.getOrderId())
                            .update("status", "Đã giao") // Trả về trạng thái đã giao
                            .addOnSuccessListener(aVoid2 -> {
                                sendNotification(false);
                                Toast.makeText(this, "Đã từ chối khiếu nại", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                });
    }

    private void sendNotification(boolean isApproved) {
        String title = "Kết quả khiếu nại đơn hàng";
        String content = "";
        String shortId = returnRequest.getOrderId();
        if (shortId != null && shortId.length() > 8) shortId = shortId.substring(shortId.length() - 8).toUpperCase();

        if (isApproved) {
            content = "Yêu cầu trả hàng/hoàn tiền cho đơn hàng #" + shortId + " đã được chấp nhận. Tiền sẽ được hoàn về cho bạn sớm.";
        } else {
            content = "Rất tiếc, yêu cầu trả hàng cho đơn hàng #" + shortId + " đã bị từ chối.";
        }

        java.util.Map<String, Object> notif = new java.util.HashMap<>();
        notif.put("userEmail", returnRequest.getUserEmail());
        notif.put("title", title);
        notif.put("message", content);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("read", false);
        notif.put("type", "return_status");
        notif.put("orderId", returnRequest.getOrderId());
        notif.put("date", new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));

        db.collection("notifications").add(notif);
    }
}



