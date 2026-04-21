package com.example.buoi1;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddVoucherActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String selectedType = "PERCENT"; // "PERCENT" or "CASH"
    private Calendar calendar = Calendar.getInstance();
    private long selectedExpiryTimestamp = 0;

    private EditText etVoucherTitle, etVoucherCode, etVoucherValue, etMaxDiscount, etMinOrder, etExpiryDate;
    private TextInputLayout tilVoucherValue, tilMaxDiscount;
    private MaterialButtonToggleGroup toggleGroupVoucherType;
    private Button btnSaveVoucher;
    private ImageView btnBack;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_voucher);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        setupTextFormatters();

        // Check if editing
        Voucher editingVoucher = (Voucher) getIntent().getSerializableExtra("VOUCHER_DATA");
        if (editingVoucher != null) {
            fillVoucherData(editingVoucher);
        }
    }

    private void fillVoucherData(Voucher voucher) {
        etVoucherTitle.setText(voucher.getTitle());
        etVoucherCode.setText(voucher.getCode());
        etVoucherCode.setEnabled(false);
        etVoucherValue.setText(String.valueOf((long)voucher.getValue()));
        etMinOrder.setText(String.valueOf((long)voucher.getMinOrder()));
        etMaxDiscount.setText(String.valueOf((long)voucher.getMaxDiscount()));
        
        selectedType = voucher.getType();
        if ("PERCENT".equals(selectedType)) {
            toggleGroupVoucherType.check(R.id.btnTypePercent);
        } else {
            toggleGroupVoucherType.check(R.id.btnTypeCash);
        }

        if (voucher.getExpiryDate() > 0) {
            selectedExpiryTimestamp = voucher.getExpiryDate();
            etExpiryDate.setText(sdf.format(new java.util.Date(selectedExpiryTimestamp)));
            calendar.setTimeInMillis(selectedExpiryTimestamp);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackAddVoucher);
        etVoucherTitle = findViewById(R.id.etVoucherTitle);
        etVoucherCode = findViewById(R.id.etVoucherCode);
        etVoucherValue = findViewById(R.id.etVoucherValue);
        etMaxDiscount = findViewById(R.id.etMaxDiscount);
        etMinOrder = findViewById(R.id.etMinOrder);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        
        tilVoucherValue = findViewById(R.id.tilVoucherValue);
        tilMaxDiscount = findViewById(R.id.tilMaxDiscount);
        toggleGroupVoucherType = findViewById(R.id.toggleGroupVoucherType);
        btnSaveVoucher = findViewById(R.id.btnSaveVoucher);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Toggle Loại giảm giá
        toggleGroupVoucherType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnTypePercent) {
                    selectedType = "PERCENT";
                    tilVoucherValue.setSuffixText("%");
                    tilMaxDiscount.setVisibility(View.VISIBLE);
                } else {
                    selectedType = "CASH";
                    tilVoucherValue.setSuffixText("₫");
                    tilMaxDiscount.setVisibility(View.GONE);
                }
            }
        });

        // Chọn ngày hết hạn
        etExpiryDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                
                selectedExpiryTimestamp = calendar.getTimeInMillis();
                etExpiryDate.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSaveVoucher.setOnClickListener(v -> saveVoucher());
    }

    private void setupTextFormatters() {
        // Định dạng dấu phẩy cho giá trị giảm (nếu là tiền)
        etVoucherValue.addTextChangedListener(new MoneyTextWatcher(etVoucherValue, () -> "CASH".equals(selectedType)));
        
        // Định dạng dấu phẩy cho mức giảm tối đa
        etMaxDiscount.addTextChangedListener(new MoneyTextWatcher(etMaxDiscount, () -> true));
        
        // Định dạng dấu phẩy cho đơn hàng tối thiểu
        etMinOrder.addTextChangedListener(new MoneyTextWatcher(etMinOrder, () -> true));
    }

    private void saveVoucher() {
        String title = etVoucherTitle.getText().toString().trim();
        String code = etVoucherCode.getText().toString().trim().toUpperCase();
        String valueStr = etVoucherValue.getText().toString().replace(",", "");
        String minOrderStr = etMinOrder.getText().toString().replace(",", "");
        String maxDiscountStr = etMaxDiscount.getText().toString().replace(",", "");

        if (title.isEmpty() || code.isEmpty() || valueStr.isEmpty() || minOrderStr.isEmpty() || selectedExpiryTimestamp == 0) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double value = Double.parseDouble(valueStr);
        double minOrder = Double.parseDouble(minOrderStr);
        double maxDiscount = maxDiscountStr.isEmpty() ? value : Double.parseDouble(maxDiscountStr);

        Voucher voucher = new Voucher();
        voucher.setTitle(title);
        voucher.setCode(code);
        voucher.setType(selectedType);
        voucher.setValue(value);
        voucher.setMinOrder(minOrder);
        voucher.setMaxDiscount(maxDiscount);
        voucher.setExpiryDate(selectedExpiryTimestamp);
        voucher.setActive(true);

        db.collection("vouchers").document(code).set(voucher)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Lưu Voucher thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Helper class để định dạng tiền tệ khi gõ
    private static class MoneyTextWatcher implements TextWatcher {
        private final EditText editText;
        private final java.util.function.BooleanSupplier shouldFormat;

        public MoneyTextWatcher(EditText editText, java.util.function.BooleanSupplier shouldFormat) {
            this.editText = editText;
            this.shouldFormat = shouldFormat;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (!shouldFormat.getAsBoolean()) return;
            editText.removeTextChangedListener(this);
            try {
                String originalString = s.toString();
                if (originalString.isEmpty()) return;
                if (originalString.contains(",")) originalString = originalString.replaceAll(",", "");
                
                Long longval = Long.parseLong(originalString);
                DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
                formatter.applyPattern("#,###,###,###");
                String formattedString = formatter.format(longval);

                editText.setText(formattedString);
                editText.setSelection(editText.getText().length());
            } catch (NumberFormatException nfe) {}
            finally {
                editText.addTextChangedListener(this);
            }
        }
    }
}
