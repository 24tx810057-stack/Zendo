package com.zendo.apps.ui.activities;

import com.zendo.apps.utils.SharedPrefManager;

import com.zendo.apps.data.models.User;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zendo.apps.databinding.ActivityPersonalInfoBinding;
import java.util.HashMap;
import java.util.Map;

public class PersonalInfoActivity extends AppCompatActivity {

    private ActivityPersonalInfoBinding binding;
    private FirebaseFirestore db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPersonalInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        userEmail = SharedPrefManager.getInstance(this).getUserEmail();

        loadUserData();
        setupListeners();
    }

    private void setupListeners() {
        binding.btnBackPersonal.setOnClickListener(v -> finish());

        binding.btnConfirmPersonal.setOnClickListener(v -> saveUserData());

        binding.etAddressPersonal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvAddressCountPersonal.setText(s.length() + "/200");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUserData() {
        if (userEmail.isEmpty()) return;
        db.collection("users").document(userEmail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            binding.etFullNamePersonal.setText(user.getFullName());
                            binding.etIdCardPersonal.setText(user.getIdCard());
                            binding.etAddressPersonal.setText(user.getAddress());
                        }
                    }
                });
    }

    private void saveUserData() {
        String fullName = binding.etFullNamePersonal.getText().toString().trim();
        String idCard = binding.etIdCardPersonal.getText().toString().trim();
        String address = binding.etAddressPersonal.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("fullName", fullName);
        updateData.put("idCard", idCard);
        updateData.put("address", address);

        db.collection("users").document(userEmail).update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Báo cho màn hình trước biết để load lại data
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}


