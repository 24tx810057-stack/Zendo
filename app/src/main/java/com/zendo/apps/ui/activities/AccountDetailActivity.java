package com.zendo.apps.ui.activities;

import com.zendo.apps.utils.SharedPrefManager;

import com.zendo.apps.data.models.User;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zendo.apps.databinding.ActivityAccountDetailBinding;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AccountDetailActivity extends AppCompatActivity {

    private ActivityAccountDetailBinding binding;
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
        binding = ActivityAccountDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        userEmail = SharedPrefManager.getInstance(this).getUserEmail();

        if (binding.tvEmailAccountDetail != null) binding.tvEmailAccountDetail.setText(userEmail);
        
        loadUserData();
        setupListeners();
    }

    private void setupListeners() {
        binding.btnEditAccount.setOnClickListener(v -> toggleEditMode());
        binding.btnSaveAccountDetail.setOnClickListener(v -> saveUserData());
        binding.btnBackDetail.setOnClickListener(v -> finish());
        
        binding.layoutChangeAvatar.setOnClickListener(v -> {
            if (isEditing) pickImageLauncher.launch("image/*");
        });

        binding.btnOpenPersonalInfo.setOnClickListener(v -> {
            startActivity(new Intent(this, PersonalInfoActivity.class));
        });
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        
        // Cho phép/Chặn nhập liệu
        binding.etNicknameDetail.setEnabled(isEditing);
        binding.etBioDetail.setEnabled(isEditing);
        binding.etGenderDetail.setEnabled(isEditing);
        binding.etBirthdateDetail.setEnabled(isEditing);
        binding.etPhoneAccountDetail.setEnabled(isEditing);
        
        // Hiển thị các nút tương ứng
        binding.ivCameraBadge.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        binding.btnSaveAccountDetail.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        binding.btnEditAccount.setText(isEditing ? "Hủy" : "Sửa");
        
        if (isEditing) {
            binding.etNicknameDetail.requestFocus();
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
                            binding.etNicknameDetail.setText(displayName);
                            binding.etBioDetail.setText(user.getBio());
                            binding.etGenderDetail.setText(user.getGender());
                            binding.etBirthdateDetail.setText(user.getBirthdate());
                            binding.etPhoneAccountDetail.setText(user.getPhone());
                            
                            String avatarData = user.getAvatar();
                            if (avatarData != null && !avatarData.isEmpty()) {
                                base64Avatar = avatarData;
                                if (avatarData.startsWith("http")) {
                                    Glide.with(this).load(avatarData).into(binding.ivDetailAvatar);
                                } else {
                                    try {
                                        byte[] decodedString = Base64.decode(avatarData, Base64.DEFAULT);
                                        Glide.with(this).load(decodedString).into(binding.ivDetailAvatar);
                                    } catch (Exception e) {}
                                }
                            }
                        }
                    }
                });
    }

    private void processSelectedImage(Uri uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            binding.ivDetailAvatar.setImageBitmap(bitmap);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] data = baos.toByteArray();
            base64Avatar = Base64.encodeToString(data, Base64.DEFAULT);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserData() {
        String newName = binding.etNicknameDetail.getText().toString().trim();
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("nickname", newName);
        updateData.put("bio", binding.etBioDetail.getText().toString().trim());
        updateData.put("gender", binding.etGenderDetail.getText().toString().trim());
        updateData.put("birthdate", binding.etBirthdateDetail.getText().toString().trim());
        updateData.put("phone", binding.etPhoneAccountDetail.getText().toString().trim());
        updateData.put("avatar", base64Avatar);

        db.collection("users").document(userEmail).update(updateData)
                .addOnSuccessListener(aVoid -> {
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
        loadUserData();
    }
}


