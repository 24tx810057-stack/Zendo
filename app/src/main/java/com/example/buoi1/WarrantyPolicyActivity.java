package com.example.buoi1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class WarrantyPolicyActivity extends AppCompatActivity {

    private TextView tvPolicy, btnEdit, btnSave;
    private EditText etPolicy;
    private FirebaseFirestore db;
    private String userRole;
    private boolean isEditing = false;

    private static final String DEFAULT_POLICY = "1. Chính sách đổi trả 15 ngày\n" +
            "Khách hàng có quyền trả hàng và hoàn tiền trong vòng 15 ngày kể từ khi nhận hàng nếu sản phẩm bị lỗi.\n\n" +
            "2. Cam kết chính hãng 100%\n" +
            "Zendo Store cam kết tất cả sản phẩm được bán ra là hàng chính hãng 100%.\n\n" +
            "3. Điều kiện bảo hành\n" +
            "• Sản phẩm còn trong thời hạn bảo hành.\n" +
            "• Lỗi do nhà sản xuất.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warranty_policy);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userRole = sharedPref.getString("user_role", "user");

        initViews();
        loadPolicy();
    }

    private void initViews() {
        tvPolicy = findViewById(R.id.tvPolicyContent);
        etPolicy = findViewById(R.id.etPolicyEdit);
        btnEdit = findViewById(R.id.btnEditPolicy);
        btnSave = findViewById(R.id.btnSavePolicy);
        ImageView btnBack = findViewById(R.id.btnBackPolicy);

        btnBack.setOnClickListener(v -> finish());
        
        if ("admin".equals(userRole)) {
            btnEdit.setVisibility(View.VISIBLE);
        }

        btnEdit.setOnClickListener(v -> toggleEditMode());
        btnSave.setOnClickListener(v -> savePolicy());
    }

    private void loadPolicy() {
        db.collection("app_settings").document("warranty_policy").get()
                .addOnSuccessListener(documentSnapshot -> {
                    String content = documentSnapshot.getString("content");
                    if (content == null || content.isEmpty()) {
                        content = DEFAULT_POLICY;
                    }
                    tvPolicy.setText(content);
                    etPolicy.setText(content);
                })
                .addOnFailureListener(e -> {
                    tvPolicy.setText(DEFAULT_POLICY);
                    etPolicy.setText(DEFAULT_POLICY);
                });
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        if (isEditing) {
            tvPolicy.setVisibility(View.GONE);
            etPolicy.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.VISIBLE);
            btnEdit.setText("Hủy");
        } else {
            tvPolicy.setVisibility(View.VISIBLE);
            etPolicy.setVisibility(View.GONE);
            btnSave.setVisibility(View.GONE);
            btnEdit.setText("Sửa");
            etPolicy.setText(tvPolicy.getText());
        }
    }

    private void savePolicy() {
        String newContent = etPolicy.getText().toString().trim();
        if (newContent.isEmpty()) {
            Toast.makeText(this, "Nội dung không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("content", newContent);

        btnSave.setEnabled(false);
        btnSave.setText("ĐANG LƯU...");

        db.collection("app_settings").document("warranty_policy").set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật chính sách mới!", Toast.LENGTH_SHORT).show();
                    tvPolicy.setText(newContent);
                    toggleEditMode();
                    btnSave.setEnabled(true);
                    btnSave.setText("LƯU CHÍNH SÁCH");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("LƯU CHÍNH SÁCH");
                });
    }
}
