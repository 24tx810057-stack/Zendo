package com.example.buoi1;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

        // 1. Xác định loại và cập nhật UI trước
        selectedType = voucher.getType();
        if (selectedType != null && selectedType.equalsIgnoreCase("PERCENT")) {
            selectedType = "PERCENT";
            toggleGroupVoucherType.check(R.id.btnTypePercent);
            updateUIForType("PERCENT");
        } else {
            selectedType = "CASH";
            toggleGroupVoucherType.check(R.id.btnTypeCash);
            updateUIForType("CASH");
        }
        
        // 2. Sau khi UI đã đúng đơn vị, mới nạp giá trị vào
        etVoucherValue.setText(String.valueOf((long)voucher.getValue()));
        etMinOrder.setText(String.valueOf((long)voucher.getMinOrder()));
        etMaxDiscount.setText(String.valueOf((long)voucher.getMaxDiscount()));

        if (voucher.getExpiryDate() > 0) {
            selectedExpiryTimestamp = voucher.getExpiryDate();
            etExpiryDate.setText(sdf.format(new java.util.Date(selectedExpiryTimestamp)));
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

        // Xử lý chuyển đổi loại Voucher
        toggleGroupVoucherType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String newType = (checkedId == R.id.btnTypePercent) ? "PERCENT" : "CASH";
                
                // Chỉ xử lý nếu thực sự đổi tab khác với loại hiện tại
                if (!newType.equalsIgnoreCase(selectedType)) {
                    // XÓA TRẮNG Ô NHẬP KHI ĐỔI LOẠI ĐỂ TRÁNH NHẦM LẪN
                    etVoucherValue.setText(""); 
                    selectedType = newType;
                    updateUIForType(selectedType);
                }
            }
        });

        // Chọn ngày hết hạn
        etExpiryDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (selectedExpiryTimestamp > 0) cal.setTimeInMillis(selectedExpiryTimestamp);

            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                
                selectedExpiryTimestamp = cal.getTimeInMillis();
                etExpiryDate.setText(sdf.format(cal.getTime()));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            
            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dialog.show();
        });

        btnSaveVoucher.setOnClickListener(v -> saveVoucher());
    }

    private void updateUIForType(String type) {
        if ("PERCENT".equalsIgnoreCase(type)) {
            tilVoucherValue.setHint("Nhập % giảm giá (1-100)");
            tilVoucherValue.setSuffixText("%");
            tilMaxDiscount.setVisibility(View.VISIBLE);
        } else {
            tilVoucherValue.setHint("Nhập số tiền giảm (₫)");
            tilVoucherValue.setSuffixText("₫");
            tilMaxDiscount.setVisibility(View.GONE);
        }
    }

    private void setupTextFormatters() {
        // Định dạng dấu phẩy cho giá trị giảm (chỉ khi là CASH)
        etVoucherValue.addTextChangedListener(new MoneyTextWatcher(etVoucherValue, () -> "CASH".equalsIgnoreCase(selectedType)));
        
        // Định dạng dấu phẩy cho các ô luôn là tiền mặt
        etMaxDiscount.addTextChangedListener(new MoneyTextWatcher(etMaxDiscount, () -> true));
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

        try {
            double value = Double.parseDouble(valueStr);
            if (selectedType.equals("PERCENT") && (value <= 0 || value > 100)) {
                etVoucherValue.setError("Phần trăm phải từ 1 đến 100");
                return;
            }

            double minOrder = Double.parseDouble(minOrderStr);
            double maxDiscount = selectedType.equals("CASH") ? value : (maxDiscountStr.isEmpty() ? value : Double.parseDouble(maxDiscountStr));

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
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá trị nhập không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

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
                originalString = originalString.replaceAll(",", "");
                
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
