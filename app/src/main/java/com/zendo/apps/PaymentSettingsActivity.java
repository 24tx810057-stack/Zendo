package com.zendo.apps;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class PaymentSettingsActivity extends AppCompatActivity {

    private Spinner spinnerBank;
    private EditText etAccountNo, etAccountName;
    private Button btnSave;
    private ImageView btnBack;
    private FirebaseFirestore db;

    // Danh sách một số ngân hàng phổ biến tại VN
    private final String[] banks = {
            "VIB - Ngân hàng Quốc tế",
            "VCB - Vietcombank",
            "MB - Ngân hàng Quân đội",
            "TCB - Techcombank",
            "CTG - VietinBank",
            "BID - BIDV",
            "ACB - Ngân hàng Á Châu",
            "TPB - TPBank",
            "STB - Sacombank",
            "VPB - VPBank"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_settings);

        db = FirebaseFirestore.getInstance();
        initViews();
        loadCurrentSettings();

        btnSave.setOnClickListener(v -> saveSettings());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        spinnerBank = findViewById(R.id.spinnerBank);
        etAccountNo = findViewById(R.id.etAccountNo);
        etAccountName = findViewById(R.id.etAccountName);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, banks);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBank.setAdapter(adapter);
    }

    private void loadCurrentSettings() {
        db.collection("admin_settings").document("bank_info").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String bankId = documentSnapshot.getString("bankId");
                        String accountNo = documentSnapshot.getString("accountNo");
                        String accountName = documentSnapshot.getString("accountName");

                        etAccountNo.setText(accountNo);
                        etAccountName.setText(accountName);

                        if (bankId != null) {
                            for (int i = 0; i < banks.length; i++) {
                                if (banks[i].startsWith(bankId)) {
                                    spinnerBank.setSelection(i);
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    private void saveSettings() {
        String selectedBank = spinnerBank.getSelectedItem().toString();
        String bankId = selectedBank.split(" - ")[0]; // Lấy mã ví dụ "VIB"
        String accountNo = etAccountNo.getText().toString().trim();
        String accountName = etAccountName.getText().toString().trim().toUpperCase();

        if (accountNo.isEmpty() || accountName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("bankId", bankId);
        data.put("accountNo", accountNo);
        data.put("accountName", accountName);

        db.collection("admin_settings").document("bank_info").set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật thông tin thanh toán!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
