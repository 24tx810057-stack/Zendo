package com.example.buoi1;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminWarrantyDetailActivity extends AppCompatActivity {

    private TextView tvOrderId, tvUser, tvStatus, tvError, tvDesc;
    private LinearLayout layoutImages;
    private Button btnReject, btnProcess, btnComplete;
    private FirebaseFirestore db;
    private WarrantyRequest request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_warranty_detail);

        db = FirebaseFirestore.getInstance();
        request = (WarrantyRequest) getIntent().getSerializableExtra("warranty_data");

        if (request == null) {
            finish();
            return;
        }

        initViews();
        displayData();
    }

    private void initViews() {
        tvOrderId = findViewById(R.id.tvDetailWarrantyOrderId);
        tvUser = findViewById(R.id.tvDetailWarrantyUser);
        tvStatus = findViewById(R.id.tvDetailWarrantyStatus);
        tvError = findViewById(R.id.tvDetailWarrantyError);
        tvDesc = findViewById(R.id.tvDetailWarrantyDesc);
        layoutImages = findViewById(R.id.layoutWarrantyEvidenceImages);
        btnReject = findViewById(R.id.btnRejectWarranty);
        btnProcess = findViewById(R.id.btnProcessWarranty);
        btnComplete = findViewById(R.id.btnCompleteWarranty);

        findViewById(R.id.btnBackWarrantyDetail).setOnClickListener(v -> finish());
        findViewById(R.id.tvAdminViewWarrantyCard).setOnClickListener(v -> fetchAndShowWarrantyCard());
        findViewById(R.id.btnCopyOrderId).setOnClickListener(v -> copyToClipboard(request.getOrderId()));

        btnReject.setOnClickListener(v -> updateStatus("rejected", "Đã từ chối yêu cầu bảo hành"));
        btnProcess.setOnClickListener(v -> updateStatus("repairing", "Đã tiếp nhận sửa chữa"));
        btnComplete.setOnClickListener(v -> updateStatus("repaired", "Đã sửa xong sản phẩm"));
    }

    private void displayData() {
        String fullId = request.getOrderId();
        tvOrderId.setText("Mã đơn hàng: #" + getShortId(fullId));
        
        tvUser.setText("Khách hàng: " + request.getUserEmail());
        tvError.setText(request.getErrorType());
        tvDesc.setText(request.getDescription());

        String status = request.getStatus();
        updateUIByStatus(status);

        if (request.getEvidenceImages() != null) {
            layoutImages.removeAllViews();
            for (String base64 : request.getEvidenceImages()) {
                ImageView iv = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 600);
                params.setMargins(0, 0, 0, 16);
                iv.setLayoutParams(params);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                
                try {
                    byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    iv.setImageBitmap(bitmap);
                } catch (Exception e) {}
                
                layoutImages.addView(iv);
            }
        }
    }

    private String getShortId(String fullId) {
        if (fullId == null || fullId.length() < 8) return fullId != null ? fullId.toUpperCase() : "";
        return fullId.substring(fullId.length() - 8).toUpperCase();
    }

    private void updateUIByStatus(String status) {
        String statusText = "CHỜ TIẾP NHẬN";
        int color = 0xFFFFA000;

        btnReject.setVisibility(View.GONE);
        btnProcess.setVisibility(View.GONE);
        btnComplete.setVisibility(View.GONE);

        if ("pending_repair".equals(status)) {
            statusText = "CHỜ TIẾP NHẬN";
            btnReject.setVisibility(View.VISIBLE);
            btnProcess.setVisibility(View.VISIBLE);
            btnProcess.setText("TIẾP NHẬN");
        } else if ("repairing".equals(status)) {
            statusText = "ĐANG SỬA CHỮA";
            color = 0xFF2196F3;
            btnComplete.setVisibility(View.VISIBLE);
        } else if ("repaired".equals(status)) {
            statusText = "ĐÃ SỬA XONG";
            color = 0xFF4CAF50;
        } else if ("rejected".equals(status)) {
            statusText = "ĐÃ TỪ CHỐI";
            color = 0xFFF44336;
        }

        tvStatus.setText("Trạng thái: " + statusText);
        tvStatus.getBackground().setTint(color);
    }

    private void updateStatus(String newStatus, String toastMsg) {
        db.collection("warranty_requests").document(request.getId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                    request.setStatus(newStatus);
                    updateUIByStatus(newStatus);
                    sendNotification(newStatus);
                });
    }

    private void sendNotification(String status) {
        String title = "Cập nhật bảo hành";
        String content = "";
        
        switch (status) {
            case "repairing": 
                content = "Yêu cầu bảo hành mã đơn #" + getShortId(request.getOrderId()) + " đã được tiếp nhận. Nhân viên sẽ liên hệ bạn trong 24h."; 
                break;
            case "repaired": 
                content = "Sản phẩm bảo hành mã đơn #" + getShortId(request.getOrderId()) + " đã sửa xong và đang gửi lại cho bạn."; 
                break;
            case "rejected": 
                content = "Rất tiếc, yêu cầu bảo hành mã đơn #" + getShortId(request.getOrderId()) + " đã bị từ chối."; 
                break;
        }

        Map<String, Object> notif = new HashMap<>();
        notif.put("userEmail", request.getUserEmail());
        notif.put("title", title);
        notif.put("content", content);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("read", false);
        notif.put("type", "warranty");

        db.collection("notifications").add(notif);
    }

    private void fetchAndShowWarrantyCard() {
        db.collection("orders").document(request.getOrderId()).get()
                .addOnSuccessListener(orderDoc -> {
                    if (orderDoc.exists()) {
                        Order order = orderDoc.toObject(Order.class);
                        db.collection("products").document(request.getProductId()).get()
                                .addOnSuccessListener(productDoc -> {
                                    if (productDoc.exists()) {
                                        Product product = productDoc.toObject(Product.class);
                                        showWarrantyCardDialog(order, product);
                                    }
                                });
                    }
                });
    }

    private void showWarrantyCardDialog(Order order, Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_warranty_card, null);
        builder.setView(view);

        TextView tvName = view.findViewById(R.id.tvCardProductName);
        TextView tvId = view.findViewById(R.id.tvCardOrderId);
        TextView tvActive = view.findViewById(R.id.tvCardActivationDate);
        TextView tvExpire = view.findViewById(R.id.tvCardExpiryDate);
        TextView tvStatusCard = view.findViewById(R.id.tvCardStatus);
        Button btnClose = view.findViewById(R.id.btnCloseCard);
        Button btnAction = view.findViewById(R.id.btnContactWarranty);

        btnAction.setVisibility(View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvName.setText(product.getName());
        String fullId = order.getId();
        tvId.setText("ID: #" + (fullId.length() > 8 ? fullId.substring(fullId.length() - 8).toUpperCase() : fullId.toUpperCase()));

        Date deliveryDate = order.getDeliveryDate() != null ? order.getDeliveryDate() : order.getTimestamp();
        tvActive.setText(sdf.format(deliveryDate));

        // Tính ngày hết hạn
        Calendar cal = Calendar.getInstance();
        cal.setTime(deliveryDate);
        String warrantyStr = product.getWarranty() != null ? product.getWarranty() : "12 tháng";
        int months = 12;
        try {
            months = Integer.parseInt(warrantyStr.replaceAll("[^0-9]", ""));
        } catch (Exception e) {}
        cal.add(Calendar.MONTH, months);
        Date expiryDate = cal.getTime();
        tvExpire.setText(sdf.format(expiryDate));

        if (expiryDate.before(new Date())) {
            tvStatusCard.setText("Trạng thái: Hết hạn bảo hành");
            tvStatusCard.setTextColor(0xFFF44336);
            tvStatusCard.setBackgroundColor(0xFFFFEBEE);
        } else {
            tvStatusCard.setText("Trạng thái: Đang bảo hành");
            tvStatusCard.setTextColor(0xFF2E7D32);
            tvStatusCard.setBackgroundColor(0xFFE8F5E9);
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Order ID", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, " Đã sao chép mã đơn hàng đầy đủ", Toast.LENGTH_SHORT).show();
    }
}
