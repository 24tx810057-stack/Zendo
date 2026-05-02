package com.example.buoi1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class PersonalInfoActivity extends AppCompatActivity {

    private EditText etFullName, etIdCard, etAddress;
    private TextView tvAddressCount;
    private ImageView btnBack;
    private AppCompatButton btnConfirm;
    private FirebaseFirestore db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");

        initViews();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullNamePersonal);
        etIdCard = findViewById(R.id.etIdCardPersonal);
        etAddress = findViewById(R.id.etAddressPersonal);
        tvAddressCount = findViewById(R.id.tvAddressCountPersonal);
        btnBack = findViewById(R.id.btnBackPersonal);
        btnConfirm = findViewById(R.id.btnConfirmPersonal);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnConfirm.setOnClickListener(v -> saveUserData());

        etAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvAddressCount.setText(s.length() + "/200");
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
                            etFullName.setText(user.getFullName());
                            etIdCard.setText(user.getIdCard());
                            etAddress.setText(user.getAddress());
                        }
                    }
                });
    }

    private void saveUserData() {
        String fullName = etFullName.getText().toString().trim();
        String idCard = etIdCard.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("fullName", fullName);
        updateData.put("idCard", idCard);
        updateData.put("address", address);

        db.collection("users").document(userEmail).update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
