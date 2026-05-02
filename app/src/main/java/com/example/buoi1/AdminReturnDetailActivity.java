package com.example.buoi1;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DecimalFormat;

public class AdminReturnDetailActivity extends AppCompatActivity {

    private TextView tvOrderId, tvUser, tvAmount, tvReason, tvDesc;
    private LinearLayout layoutImages;
    private Button btnApprove, btnReject;
    
    private ReturnRequest returnRequest;
    private FirebaseFirestore db;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_return_detail);

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
        tvOrderId = findViewById(R.id.tvDetailReturnOrderId);
        tvUser = findViewById(R.id.tvDetailReturnUser);
        tvAmount = findViewById(R.id.tvDetailReturnAmount);
        tvReason = findViewById(R.id.tvDetailReturnReason);
        tvDesc = findViewById(R.id.tvDetailReturnDesc);
        layoutImages = findViewById(R.id.layoutDetailEvidenceImages);
        btnApprove = findViewById(R.id.btnApproveReturn);
        btnReject = findViewById(R.id.btnRejectReturn);

        findViewById(R.id.btnBackReturnDetail).setOnClickListener(v -> finish());

        btnApprove.setOnClickListener(v -> showConfirmDialog(true));
        btnReject.setOnClickListener(v -> showConfirmDialog(false));
        
        // Nếu đã xử lý rồi thì ẩn nút
        if (!"pending".equals(returnRequest.getStatus())) {
            findViewById(R.id.layoutAdminReturnActions).setVisibility(View.GONE);
        }
    }

    private void displayData() {
        tvOrderId.setText("Mã đơn hàng: " + returnRequest.getOrderId());
        tvUser.setText("Người gửi: " + returnRequest.getUserEmail());
        tvAmount.setText("Số tiền yêu cầu hoàn: " + formatter.format(returnRequest.getRefundAmount()) + "đ");
        tvReason.setText(returnRequest.getReason());
        tvDesc.setText(returnRequest.getDescription());

        if (returnRequest.getEvidenceImages() != null) {
            layoutImages.removeAllViews();
            for (String base64 : returnRequest.getEvidenceImages()) {
                ImageView iv = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 800);
                lp.setMargins(0, 0, 0, 20);
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iv.setBackgroundResource(R.drawable.bg_border_gray_light);
                
                try {
                    byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    iv.setImageBitmap(bitmap);
                } catch (Exception e) {}
                
                layoutImages.addView(iv);
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
                    db.collection("orders").document(returnRequest.getOrderId())
                            .update("status", "Đã hoàn tiền")
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Đã duyệt hoàn tiền thành công!", Toast.LENGTH_SHORT).show();
                                finish();
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
                                Toast.makeText(this, "Đã từ chối khiếu nại", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                });
    }
}
