package com.zendo.apps.ui.activities;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zendo.apps.R;
import com.zendo.apps.data.models.Wallet;
import com.zendo.apps.utils.SharedPrefManager;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class ZendoPayActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userEmail;
    private Wallet currentWallet;
    private final DecimalFormat formatter = new DecimalFormat("###,###,###");

    // UI Elements
    private TextView tvWalletBalance, tvBankStatus, tvMoMoStatus, tvPinStatus;
    private ImageView btnBackWallet;
    private View btnDeposit, btnWithdraw, btnLinkBank, btnLinkMoMo, btnSetWalletPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zendopay);

        db = FirebaseFirestore.getInstance();
        userEmail = SharedPrefManager.getInstance(this).getUserEmail();

        initViews();
        
        if (btnBackWallet != null) {
            btnBackWallet.setOnClickListener(v -> finish());
        }
        
        setupInitialUI();
        loadWalletData();
    }

    private void initViews() {
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        tvBankStatus = findViewById(R.id.tvBankStatus);
        tvMoMoStatus = findViewById(R.id.tvMoMoStatus);
        tvPinStatus = findViewById(R.id.tvPinStatus);
        btnBackWallet = findViewById(R.id.btnBackWallet);
        btnDeposit = findViewById(R.id.btnDeposit);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        btnLinkBank = findViewById(R.id.btnLinkBank);
        btnLinkMoMo = findViewById(R.id.btnLinkMoMo);
        btnSetWalletPin = findViewById(R.id.btnSetWalletPin);
    }

    private void setupInitialUI() {
        if (btnDeposit != null) btnDeposit.setOnClickListener(v -> showDepositDialog());
        if (btnWithdraw != null) btnWithdraw.setOnClickListener(v -> Toast.makeText(this, "Tính năng Rút tiền sẽ sớm ra mắt!", Toast.LENGTH_SHORT).show());
        
        if (btnLinkBank != null) {
            btnLinkBank.setOnClickListener(v -> {
                if (currentWallet != null && currentWallet.isLinkedBank()) {
                    showManageLinkDialog("BANK");
                } else {
                    showLinkDialog("BANK");
                }
            });
        }
        
        if (btnLinkMoMo != null) {
            btnLinkMoMo.setOnClickListener(v -> {
                if (currentWallet != null && currentWallet.isLinkedMoMo()) {
                    showManageLinkDialog("MOMO");
                } else {
                    showLinkDialog("MOMO");
                }
            });
        }
        
        if (btnSetWalletPin != null) btnSetWalletPin.setOnClickListener(v -> showSetPinDialog());
    }

    private void showManageLinkDialog(String type) {
        if (currentWallet == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_manage_link, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = view.findViewById(R.id.tvManageLinkTitle);
        TextView tvLabel1 = view.findViewById(R.id.tvManageLinkLabel1);
        TextView tvVal1 = view.findViewById(R.id.tvManageLinkVal1);
        TextView tvLabel2 = view.findViewById(R.id.tvManageLinkLabel2);
        TextView tvVal2 = view.findViewById(R.id.tvManageLinkVal2);
        TextView tvLabel3 = view.findViewById(R.id.tvManageLinkLabel3);
        TextView tvVal3 = view.findViewById(R.id.tvManageLinkVal3);

        if ("BANK".equals(type)) {
            tvTitle.setText("Quản lý Ngân hàng");
            tvLabel1.setText("Tên ngân hàng");
            tvVal1.setText(currentWallet.getBankName());
            tvLabel2.setText("Số tài khoản");
            tvVal2.setText(currentWallet.getAccountNo());
            tvLabel3.setText("Họ tên chủ thẻ");
            tvVal3.setText(currentWallet.getAccountName());
        } else {
            tvTitle.setText("Quản lý Ví MoMo");
            tvLabel1.setText("Số điện thoại");
            tvVal1.setText(currentWallet.getMomoPhone());
            tvLabel2.setVisibility(View.GONE);
            tvVal2.setVisibility(View.GONE);
            tvLabel3.setVisibility(View.GONE);
            tvVal3.setVisibility(View.GONE);
        }

        view.findViewById(R.id.btnCloseManage).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnUnlink).setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            if ("BANK".equals(type)) {
                updates.put("isLinkedBank", false);
                updates.put("bankName", null);
                updates.put("accountNo", null);
                updates.put("accountName", null);
            } else {
                updates.put("isLinkedMoMo", false);
                updates.put("momoPhone", null);
            }

            db.collection("wallets").document(userEmail).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã hủy liên kết thành công", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
        });

        dialog.show();
    }

    private void showDepositDialog() {
        if (currentWallet == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_wallet_deposit, null);
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etAmount = view.findViewById(R.id.etDepositAmount);
        RadioGroup rgSource = view.findViewById(R.id.rgDepositSource);
        
        view.findViewById(R.id.btnCancelDeposit).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirmDeposit).setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int selectedId = rgSource.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Vui lòng chọn nguồn tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            
            boolean fromBank = selectedId == R.id.rbSourceBank;
            if (fromBank && !currentWallet.isLinkedBank()) {
                Toast.makeText(this, "Vui lòng liên kết Ngân hàng trước", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!fromBank && !currentWallet.isLinkedMoMo()) {
                Toast.makeText(this, "Vui lòng liên kết Ví MoMo trước", Toast.LENGTH_SHORT).show();
                return;
            }

            double depositVal = Double.parseDouble(amountStr);
            double newBalance = currentWallet.getBalance() + depositVal;
            db.collection("wallets").document(userEmail).update("balance", newBalance)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã nạp thành công " + formatter.format(depositVal) + "đ", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                });
        });

        dialog.show();
    }

    private void showLinkDialog(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_link_account, null);
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = view.findViewById(R.id.tvLinkTitle);
        View layoutBank = view.findViewById(R.id.layoutBankFields);
        View layoutMoMo = view.findViewById(R.id.layoutMoMoFields);
        Spinner spinner = view.findViewById(R.id.spinnerBankSelect);
        EditText etAccNo = view.findViewById(R.id.etLinkAccountNo);
        EditText etAccName = view.findViewById(R.id.etLinkAccountName);
        EditText etMoMoPhone = view.findViewById(R.id.etLinkMoMoPhone);

        if ("BANK".equals(type)) {
            tvTitle.setText("Liên kết Ngân hàng");
            layoutBank.setVisibility(View.VISIBLE);
            layoutMoMo.setVisibility(View.GONE);
            
            String[] banks = {"Vietcombank", "MB Bank", "Techcombank", "TP Bank", "ACB"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, banks);
            spinner.setAdapter(adapter);
        } else {
            tvTitle.setText("Liên kết Ví MoMo");
            layoutBank.setVisibility(View.GONE);
            layoutMoMo.setVisibility(View.VISIBLE);
        }

        view.findViewById(R.id.btnCancelLink).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirmLink).setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            if ("BANK".equals(type)) {
                String accNo = etAccNo.getText().toString().trim();
                String accName = etAccName.getText().toString().trim();
                
                if (accNo.isEmpty() || accName.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                updates.put("isLinkedBank", true);
                updates.put("bankName", spinner.getSelectedItem().toString());
                updates.put("accountNo", accNo);
                updates.put("accountName", accName);
            } else {
                String momoPhone = etMoMoPhone.getText().toString().trim();
                if (momoPhone.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập số điện thoại MoMo", Toast.LENGTH_SHORT).show();
                    return;
                }

                updates.put("isLinkedMoMo", true);
                updates.put("momoPhone", momoPhone);
            }
            
            db.collection("wallets").document(userEmail).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Liên kết thành công!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
        });

        dialog.show();
    }

    private void showSetPinDialog() {
        boolean hasPin = currentWallet != null && currentWallet.getPinCode() != null && !currentWallet.getPinCode().isEmpty();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_wallet_pin, null);
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = view.findViewById(R.id.tvPinDialogTitle);
        View tilCurrent = view.findViewById(R.id.tilCurrentPin);
        EditText etCurrent = view.findViewById(R.id.etCurrentPin);
        EditText etNew = view.findViewById(R.id.etNewPin);
        EditText etConfirm = view.findViewById(R.id.etConfirmNewPin);

        if (!hasPin) {
            tvTitle.setText("Thiết lập mã PIN ví");
            tilCurrent.setVisibility(View.GONE);
        } else {
            tvTitle.setText("Đổi mã PIN ví");
            tilCurrent.setVisibility(View.VISIBLE);
        }
        
        view.findViewById(R.id.btnCancelPinChange).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnSaveNewPin).setOnClickListener(v -> {
            String newPin = etNew.getText().toString().trim();
            String confirmPin = etConfirm.getText().toString().trim();
            
            if (hasPin) {
                String currentInput = etCurrent.getText().toString().trim();
                if (!currentWallet.getPinCode().equals(currentInput)) {
                    Toast.makeText(this, "Mã PIN hiện tại không đúng", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (newPin.length() != 6) {
                Toast.makeText(this, "Mã PIN mới phải đủ 6 chữ số", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPin.equals(confirmPin)) {
                Toast.makeText(this, "Xác nhận mã PIN không khớp", Toast.LENGTH_SHORT).show();
                return;
            }
            
            db.collection("wallets").document(userEmail).update("pinCode", newPin)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã lưu mã PIN mới thành công", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
        });

        dialog.show();
    }

    private void loadWalletData() {
        if (userEmail == null) return;

        DocumentReference walletRef = db.collection("wallets").document(userEmail);
        walletRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("ZendoPayActivity", "Error loading wallet", error);
                return;
            }

            if (value != null && value.exists()) {
                currentWallet = value.toObject(Wallet.class);
                runOnUiThread(this::updateUI);
            } else {
                // Create default wallet if not exists
                currentWallet = new Wallet(userEmail);
                walletRef.set(currentWallet);
                runOnUiThread(this::updateUI);
            }
        });
    }

    private void updateUI() {
        if (currentWallet == null) return;

        tvWalletBalance.setText(formatter.format(currentWallet.getBalance()) + "đ");
        
        Log.d("ZendoPayActivity", "Updating UI: linkedBank=" + currentWallet.isLinkedBank() + ", bankName=" + currentWallet.getBankName());

        if (currentWallet.isLinkedBank()) {
            String accNo = currentWallet.getAccountNo();
            String masked = (accNo != null && accNo.length() > 4) ? "****" + accNo.substring(accNo.length() - 4) : "****";
            String displayName = currentWallet.getBankName() != null ? currentWallet.getBankName() : "Ngân hàng";
            tvBankStatus.setText(displayName + " " + masked);
            tvBankStatus.setTextColor(getResources().getColor(R.color.blue_modern_primary));
        } else {
            tvBankStatus.setText("Chưa liên kết");
            tvBankStatus.setTextColor(getResources().getColor(R.color.slate_500));
        }

        if (currentWallet.isLinkedMoMo()) {
            String phone = currentWallet.getMomoPhone();
            String masked = (phone != null && phone.length() > 4) ? "****" + phone.substring(phone.length() - 4) : "****";
            tvMoMoStatus.setText("MoMo " + masked);
            tvMoMoStatus.setTextColor(getResources().getColor(R.color.blue_modern_primary));
        } else {
            tvMoMoStatus.setText("Chưa liên kết");
            tvMoMoStatus.setTextColor(getResources().getColor(R.color.slate_500));
        }
        
        boolean hasPin = currentWallet.getPinCode() != null && !currentWallet.getPinCode().isEmpty();
        tvPinStatus.setText(hasPin ? "Đã thiết lập" : "Chưa thiết lập");
        tvPinStatus.setTextColor(hasPin ?
            getResources().getColor(R.color.emerald_600) : 
            getResources().getColor(R.color.rose_600));
    }
}
