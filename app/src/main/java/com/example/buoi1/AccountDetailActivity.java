package com.example.buoi1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountDetailActivity extends AppCompatActivity {

    private EditText etFullName, etPhone, etAddress;
    private TextView tvEmail, btnEdit;
    private ImageView btnBack, ivAvatar, ivCameraBadge;
    private Button btnSave;
    private RelativeLayout layoutAvatar;
    private FirebaseFirestore db;
    private String userEmail;
    private boolean isEditing = false;
    private String base64Avatar = "";
    
    private String selectedProvince = "", selectedDistrict = "", selectedWard = "";

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

        btnEdit.setOnClickListener(v -> toggleEditMode());
        btnSave.setOnClickListener(v -> saveUserData());
        btnBack.setOnClickListener(v -> finish());
        
        layoutAvatar.setOnClickListener(v -> {
            if (isEditing) pickImageLauncher.launch("image/*");
        });

        // Bấm vào ô địa chỉ để hiện Picker
        etAddress.setOnClickListener(v -> {
            if (isEditing) {
                showAddressPicker();
            }
        });
    }

    private void initViews() {
        etFullName = findViewById(R.id.etDetailFullName);
        tvEmail = findViewById(R.id.tvDetailEmail);
        etPhone = findViewById(R.id.etDetailPhone);
        etAddress = findViewById(R.id.etDetailAddress);
        btnBack = findViewById(R.id.btnBackDetail);
        btnEdit = findViewById(R.id.btnEditAccount);
        btnSave = findViewById(R.id.btnSaveAccount);
        ivAvatar = findViewById(R.id.ivDetailAvatar);
        ivCameraBadge = findViewById(R.id.ivCameraBadge);
        layoutAvatar = findViewById(R.id.layoutChangeAvatar);
        
        tvEmail.setText(userEmail);
        
        // CẤU HÌNH QUAN TRỌNG: Không cho hiện bàn phím nhưng vẫn cho phép Click
        etAddress.setFocusable(false);
        etAddress.setFocusableInTouchMode(false);
        etAddress.setClickable(true);
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        etFullName.setEnabled(isEditing);
        etPhone.setEnabled(isEditing);
        
        // PHẢI ENABLE THÌ MỚI CLICK ĐƯỢC
        etAddress.setEnabled(isEditing); 
        
        ivCameraBadge.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        btnSave.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        btnEdit.setText(isEditing ? "Hủy" : "Sửa");
        
        if (isEditing) {
            etFullName.requestFocus();
            etAddress.setHint("Chạm để chọn địa chỉ...");
        } else {
            etAddress.setHint("Chưa cập nhật");
        }
    }

    private void showAddressPicker() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_address_picker, null);
        bottomSheetDialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvPickerTitle);
        ListView lvData = view.findViewById(R.id.lvPickerData);

        List<String> provinces = Arrays.asList("TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, provinces);
        lvData.setAdapter(adapter);

        tvTitle.setText("Chọn Tỉnh / Thành phố");

        lvData.setOnItemClickListener((parent, view1, position, id) -> {
            selectedProvince = provinces.get(position);
            if (selectedProvince.equals("TP. Hồ Chí Minh")) {
                showDistrictPicker(bottomSheetDialog);
            } else {
                etAddress.setText(selectedProvince);
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    private void showDistrictPicker(BottomSheetDialog dialog) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_address_picker, null);
        dialog.setContentView(view);
        
        TextView tvTitle = view.findViewById(R.id.tvPickerTitle);
        ListView lvData = view.findViewById(R.id.lvPickerData);
        
        tvTitle.setText("Chọn Quận / Thành phố thuộc tỉnh");
        
        List<String> districts = Arrays.asList("Thành phố Thủ Đức", "Quận 1", "Quận 12", "Quận Gò Vấp");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, districts);
        lvData.setAdapter(adapter);

        lvData.setOnItemClickListener((parent, view1, position, id) -> {
            selectedDistrict = districts.get(position);
            showWardPicker(dialog);
        });
    }

    private void showWardPicker(BottomSheetDialog dialog) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_address_picker, null);
        dialog.setContentView(view);
        
        TextView tvTitle = view.findViewById(R.id.tvPickerTitle);
        ListView lvData = view.findViewById(R.id.lvPickerData);
        
        tvTitle.setText("Chọn Phường / Xã");
        
        List<String> wards = Arrays.asList("Phường Trung Mỹ Tây", "Phường Bến Nghé", "Phường Thảo Điền", "Phường An Phú");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wards);
        lvData.setAdapter(adapter);

        lvData.setOnItemClickListener((parent, view1, position, id) -> {
            selectedWard = wards.get(position);
            etAddress.setText(selectedWard + ", " + selectedDistrict + ", " + selectedProvince);
            dialog.dismiss();
        });
    }

    private void loadUserData() {
        if (userEmail.isEmpty()) return;
        db.collection("users").document(userEmail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            etFullName.setText(user.getFullName());
                            etPhone.setText(user.getPhone());
                            etAddress.setText(user.getAddress());
                            
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
        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("fullName", name);
        updateData.put("phone", phone);
        updateData.put("address", address);
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
}
