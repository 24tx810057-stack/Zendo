package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AccountDetailActivity extends AppCompatActivity {

    private EditText etNickname, etBio, etGender, etBirthdate, etPhone;
    private TextView tvEmail, btnEdit;
    private ImageView btnBack, ivAvatar, ivCameraBadge;
    private Button btnSave;
    private RelativeLayout layoutAvatar;
    private View btnOpenPersonalInfo;
    private FirebaseFirestore db;
    private String userEmail;
    private boolean isEditing = false;
    private String base64Avatar = "";

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processSelectedImage(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");

        initViews();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        // Nhóm 1: Cơ bản
        etNickname = findViewById(R.id.etNicknameDetail);
        etBio = findViewById(R.id.etBioDetail);
        etGender = findViewById(R.id.etGenderDetail);
        etBirthdate = findViewById(R.id.etBirthdateDetail);
        
        // Nhóm 2: Mở trang Thông tin cá nhân
        btnOpenPersonalInfo = findViewById(R.id.btnOpenPersonalInfo);
        
        // Nhóm 3: Liên hệ
        etPhone = findViewById(R.id.etPhoneAccountDetail);
        tvEmail = findViewById(R.id.tvEmailAccountDetail);
        
        // Các thành phần chung
        btnBack = findViewById(R.id.btnBackDetail);
        btnEdit = findViewById(R.id.btnEditAccount);
        btnSave = findViewById(R.id.btnSaveAccountDetail);
        ivAvatar = findViewById(R.id.ivDetailAvatar);
        ivCameraBadge = findViewById(R.id.ivCameraBadge);
        layoutAvatar = findViewById(R.id.layoutChangeAvatar);
        
        if (tvEmail != null) tvEmail.setText(userEmail);
    }

    private void setupListeners() {
        btnEdit.setOnClickListener(v -> toggleEditMode());
        btnSave.setOnClickListener(v -> saveUserData());
        btnBack.setOnClickListener(v -> finish());
        
        layoutAvatar.setOnClickListener(v -> {
            if (isEditing) pickImageLauncher.launch("image/*");
        });

        if (btnOpenPersonalInfo != null) {
            btnOpenPersonalInfo.setOnClickListener(v -> {
                startActivity(new Intent(this, PersonalInfoActivity.class));
            });
        }
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        
        // Cho phép/Chặn nhập liệu
        etNickname.setEnabled(isEditing);
        etBio.setEnabled(isEditing);
        etGender.setEnabled(isEditing);
        etBirthdate.setEnabled(isEditing);
        etPhone.setEnabled(isEditing);
        
        // Hiển thị các nút tương ứng
        ivCameraBadge.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        btnSave.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        btnEdit.setText(isEditing ? "Hủy" : "Sửa");
        
        if (isEditing) {
            etNickname.requestFocus();
        }
    }

    private void loadUserData() {
        if (userEmail == null || userEmail.isEmpty()) return;
        db.collection("users").document(userEmail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            String displayName = user.getNickname();
                            if (displayName == null || displayName.isEmpty()) {
                                displayName = user.getFullName();
                            }
                            etNickname.setText(displayName);
                            etBio.setText(user.getBio());
                            etGender.setText(user.getGender());
                            etBirthdate.setText(user.getBirthdate());
                            etPhone.setText(user.getPhone());
                            
                            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                                base64Avatar = user.getAvatar();
                                try {
                                    byte[] decodedString = Base64.decode(base64Avatar, Base64.DEFAULT);
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    ivAvatar.setImageBitmap(decodedByte);
                                } catch (Exception e) {}
                            }
                        }
                    }
                });
    }

    private void processSelectedImage(Uri uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            ivAvatar.setImageBitmap(bitmap);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] data = baos.toByteArray();
            base64Avatar = Base64.encodeToString(data, Base64.DEFAULT);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserData() {
        String newName = etNickname.getText().toString().trim();
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("nickname", newName);
        // Không cập nhật fullName ở đây để giữ tên thật trong CCCD
        updateData.put("bio", etBio.getText().toString().trim());
        updateData.put("gender", etGender.getText().toString().trim());
        updateData.put("birthdate", etBirthdate.getText().toString().trim());
        updateData.put("phone", etPhone.getText().toString().trim());
        updateData.put("avatar", base64Avatar);

        db.collection("users").document(userEmail).update(updateData)
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật SharedPreferences để đồng bộ tên ra bên ngoài
                    SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                    sharedPref.edit().putString("user_name", newName).apply();

                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    toggleEditMode();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại dữ liệu đề phòng trường hợp thông tin cá nhân vừa được cập nhật ở trang kia
        loadUserData();
    }
}
