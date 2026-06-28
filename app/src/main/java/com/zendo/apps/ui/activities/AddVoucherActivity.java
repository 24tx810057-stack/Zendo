package com.zendo.apps.ui.activities;

import com.zendo.apps.R;

import com.zendo.apps.data.models.Voucher;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.zendo.apps.databinding.ActivityAddVoucherBinding;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddVoucherActivity extends AppCompatActivity {

    private ActivityAddVoucherBinding binding;
    private FirebaseFirestore db;
    private String selectedType = "PERCENT"; // "PERCENT" or "CASH"
    private long selectedExpiryTimestamp = 0;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddVoucherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        setupListeners();
        setupTextFormatters();

        // Check if editing
        Voucher editingVoucher = (Voucher) getIntent().getSerializableExtra("VOUCHER_DATA");
        if (editingVoucher != null) {
            fillVoucherData(editingVoucher);
        }
    }

    private void fillVoucherData(Voucher voucher) {
        binding.etVoucherTitle.setText(voucher.getTitle());
        binding.etVoucherCode.setText(voucher.getCode());
        binding.etVoucherCode.setEnabled(false);

        // 1. Xác định loại và cập nhật UI trước
        selectedType = voucher.getType();
        if (selectedType != null && selectedType.equalsIgnoreCase("PERCENT")) {
            selectedType = "PERCENT";
            binding.toggleGroupVoucherType.check(R.id.btnTypePercent);
            updateUIForType("PERCENT");
        } else {
            selectedType = "CASH";
            binding.toggleGroupVoucherType.check(R.id.btnTypeCash);
            updateUIForType("CASH");
        }
        
        // 2. Sau khi UI đã đúng đơn vị, mới nạp giá trị vào
        binding.etVoucherValue.setText(String.valueOf((long)voucher.getValue()));
        binding.etMinOrder.setText(String.valueOf((long)voucher.getMinOrder()));
        binding.etMaxDiscount.setText(String.valueOf((long)voucher.getMaxDiscount()));

        if (voucher.getExpiryDate() > 0) {
            selectedExpiryTimestamp = voucher.getExpiryDate();
            binding.etExpiryDate.setText(sdf.format(new java.util.Date(selectedExpiryTimestamp)));
        }
    }

    private void setupListeners() {
        binding.btnBackAddVoucher.setOnClickListener(v -> finish());

        // Xử lý chuyển đổi loại Voucher
        binding.toggleGroupVoucherType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String newType = (checkedId == R.id.btnTypePercent) ? "PERCENT" : "CASH";
                
                // Chỉ xử lý nếu thực sự đổi tab khác với loại hiện tại
                if (!newType.equalsIgnoreCase(selectedType)) {
                    // XÓA TRẮNG Ô NHẬP KHI ĐỔI LOẠI ĐỂ TRÁNH NHẦM LẪN
                    binding.etVoucherValue.setText(""); 
                    selectedType = newType;
                    updateUIForType(selectedType);
                }
            }
        });

        // Chọn ngày hết hạn
        binding.etExpiryDate.setOnClickListener(v -> {
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
                binding.etExpiryDate.setText(sdf.format(cal.getTime()));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            
            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dialog.show();
        });

        binding.btnSaveVoucher.setOnClickListener(v -> saveVoucher());
    }

    private void updateUIForType(String type) {
        if ("PERCENT".equalsIgnoreCase(type)) {
            binding.tilVoucherValue.setHint("Nhập % giảm giá (1-100)");
            binding.tilVoucherValue.setSuffixText("%");
            binding.tilMaxDiscount.setVisibility(View.VISIBLE);
        } else {
            binding.tilVoucherValue.setHint("Nhập số tiền giảm (₫)");
            binding.tilVoucherValue.setSuffixText("₫");
            binding.tilMaxDiscount.setVisibility(View.GONE);
        }
    }

    private void setupTextFormatters() {
        // Định dạng dấu phẩy cho giá trị giảm (chỉ khi là CASH)
        binding.etVoucherValue.addTextChangedListener(new MoneyTextWatcher(binding.etVoucherValue, () -> "CASH".equalsIgnoreCase(selectedType)));
        
        // Định dạng dấu phẩy cho các ô luôn là tiền mặt
        binding.etMaxDiscount.addTextChangedListener(new MoneyTextWatcher(binding.etMaxDiscount, () -> true));
        binding.etMinOrder.addTextChangedListener(new MoneyTextWatcher(binding.etMinOrder, () -> true));
    }

    private void saveVoucher() {
        String title = binding.etVoucherTitle.getText().toString().trim();
        String code = binding.etVoucherCode.getText().toString().trim().toUpperCase();
        String valueStr = binding.etVoucherValue.getText().toString().replace(",", "");
        String minOrderStr = binding.etMinOrder.getText().toString().replace(",", "");
        String maxDiscountStr = binding.etMaxDiscount.getText().toString().replace(",", "");

        if (title.isEmpty() || code.isEmpty() || valueStr.isEmpty() || minOrderStr.isEmpty() || selectedExpiryTimestamp == 0) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double value = Double.parseDouble(valueStr);
            if (selectedType.equals("PERCENT") && (value <= 0 || value > 100)) {
                binding.etVoucherValue.setError("Phần trăm phải từ 1 đến 100");
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



