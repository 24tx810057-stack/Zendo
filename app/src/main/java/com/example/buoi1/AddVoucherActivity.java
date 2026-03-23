package com.example.buoi1;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddVoucherActivity extends AppCompatActivity {

    private EditText etTitle, etCode, etValue, etMinOrder;
    private Spinner spType;
    private Button btnSave;
    private FirebaseFirestore db;

    private String[] types = {"Giảm theo %", "Giảm tiền mặt"};
    private String[] typeValues = {"percent", "amount"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_voucher);

        db = FirebaseFirestore.getInstance();

        etTitle = findViewById(R.id.etVoucherTitle);
        etCode = findViewById(R.id.etVoucherCode);
        etValue = findViewById(R.id.etVoucherValue);
        etMinOrder = findViewById(R.id.etMinOrder);
        spType = findViewById(R.id.spVoucherType);
        btnSave = findViewById(R.id.btnSaveVoucher);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(adapter);

        btnSave.setOnClickListener(v -> saveVoucher());
    }

    private void saveVoucher() {
        String title = etTitle.getText().toString().trim();
        String code = etCode.getText().toString().trim().toUpperCase();
        String valueStr = etValue.getText().toString().trim();
        String minOrderStr = etMinOrder.getText().toString().trim();
        String type = typeValues[spType.getSelectedItemPosition()];

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(code) || TextUtils.isEmpty(valueStr)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Voucher voucher = new Voucher();
        voucher.setTitle(title);
        voucher.setCode(code);
        voucher.setType(type);
        voucher.setValue(Double.parseDouble(valueStr));
        voucher.setMinOrder(minOrderStr.isEmpty() ? 0 : Double.parseDouble(minOrderStr));
        voucher.setActive(true);
        voucher.setExpiryDate(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)); // Mặc định 30 ngày

        db.collection("vouchers").add(voucher)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Đã thêm Voucher thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
