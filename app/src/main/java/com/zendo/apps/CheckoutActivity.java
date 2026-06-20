package com.zendo.apps;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.zendo.apps.databinding.ActivityCheckoutBinding;

import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class CheckoutActivity extends AppCompatActivity {

    private ActivityCheckoutBinding binding;
    private FirebaseFirestore db;
    private String userEmail;
    private List<CartItem> checkoutItems;
    private double subtotalValue = 0;
    private double shippingFee = 15000;
    private double discountAmount = 0;
    private double finalTotal = 0;
    private String dynamicDescription = "THANH TOAN DON HANG";
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    private String editName, editPhone, editAddress;
    private static final int REQUEST_CODE_SELECT_ADDRESS = 1001;

    private String bankId = "";
    private String accountNo = "";
    private String accountName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        userEmail = prefManager.getUserEmail();

        checkoutItems = (ArrayList<CartItem>) getIntent().getSerializableExtra("checkout_items");

        loadAdminBankInfo();
        initViews();
        loadDefaultAddress();
        displayProducts();
        setupShippingSelection();
        setupPaymentSelection();
        calculateTotals();

        binding.layoutVoucher.setOnClickListener(v -> showVoucherBottomSheet());
        binding.btnPlaceOrder.setOnClickListener(v -> handlePlaceOrder());
        binding.btnBackCheckout.setOnClickListener(v -> finish());
        
        View.OnClickListener addressClickListener = v -> showAddressBottomSheet();
        
        binding.btnEditAddress.setOnClickListener(addressClickListener);
        binding.btnAddFirstAddress.setOnClickListener(addressClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // CỐ ĐỊNH: Lấy từ ZendoQuickExchange để thống nhất
        android.content.SharedPreferences sp = getSharedPreferences("ZendoQuickExchange", android.content.Context.MODE_PRIVATE);
        String lastAddr = sp.getString("last_added_address", "");
        if (!lastAddr.isEmpty()) {
            editName = sp.getString("last_added_name", "");
            editPhone = sp.getString("last_added_phone", "");
            editAddress = lastAddr;
            updateAddressUI();
            sp.edit().clear().apply();
        } else if (editAddress == null || editAddress.isEmpty()) {
            loadDefaultAddress();
        }
    }

    private void initViews() {
        // Most initializations handled by binding
    }

    private void setupPaymentSelection() {
        selectPaymentMethod("COD");

        binding.layoutPayCOD.setOnClickListener(v -> selectPaymentMethod("COD"));
        binding.layoutPayBank.setOnClickListener(v -> selectPaymentMethod("BANK"));
        binding.layoutPayZendo.setOnClickListener(v -> selectPaymentMethod("ZENDOPAY"));

        binding.btnCopySTK.setOnClickListener(v -> copyToClipboard(accountNo, "Đã sao chép số tài khoản"));
        binding.btnCopyNoiDung.setOnClickListener(v -> copyToClipboard(dynamicDescription, "Đã sao chép nội dung chuyển khoản"));
    }

    private void selectPaymentMethod(String method) {
        binding.rbSelectCOD.setChecked(false);
        binding.rbSelectBank.setChecked(false);
        binding.rbSelectZendo.setChecked(false);
        binding.layoutPayCOD.setSelected(false);
        binding.layoutPayBank.setSelected(false);
        binding.layoutPayZendo.setSelected(false);
        binding.layoutBankDetails.setVisibility(View.GONE);

        switch (method) {
            case "COD":
                binding.rbSelectCOD.setChecked(true);
                binding.layoutPayCOD.setSelected(true);
                break;
            case "BANK":
                binding.rbSelectBank.setChecked(true);
                binding.layoutPayBank.setSelected(true);
                binding.layoutBankDetails.setVisibility(View.VISIBLE);
                updateVietQR(); 
                break;
            case "ZENDOPAY":
                binding.rbSelectZendo.setChecked(true);
                binding.layoutPayZendo.setSelected(true);
                break;
        }
    }

    private void loadAdminBankInfo() {
        db.collection("admin_settings").document("bank_info").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        bankId = documentSnapshot.getString("bankId");
                        accountNo = documentSnapshot.getString("accountNo");
                        accountName = documentSnapshot.getString("accountName");

                        binding.tvBankSTK.setText("Số TK: " + accountNo);
                        binding.tvBankOwner.setText("Chủ TK: " + accountName);
                        binding.tvBankName.setText("Ngân hàng: " + bankId);

                        if (binding.rbSelectBank.isChecked()) {
                            updateVietQR();
                        }
                    }
                });
    }

    private void updateVietQR() {
        if (checkoutItems == null || checkoutItems.isEmpty()) return;

        String template = "compact2";
        
        String firstItemName = checkoutItems.get(0).getProductName();
        String cleanName = removeAccent(firstItemName).replaceAll("[^a-zA-Z0-9]", "");
        if (cleanName.length() > 10) cleanName = cleanName.substring(0, 10);
        
        dynamicDescription = "ZENDO " + cleanName.toUpperCase() + " " + (int)finalTotal;
        
        binding.tvTransferNote.setText("Nội dung: " + dynamicDescription);

        String url = String.format("https://img.vietqr.io/image/%s-%s-%s.png?amount=%d&addInfo=%s",
                bankId, accountNo, template, (int)finalTotal, dynamicDescription);
        
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.bg_border_gray_light)
                .into(binding.ivBankQR);
    }

    private String removeAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    private void copyToClipboard(String text, String message) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("ZendoInfo", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
    private void setupShippingSelection() {
        binding.rbStandard.setChecked(true);
        binding.layoutItemStandard.setSelected(true);

        View.OnClickListener fastListener = v -> {
            binding.rbFast.setChecked(true);
            binding.rbStandard.setChecked(false);
            binding.layoutItemFast.setSelected(true);
            binding.layoutItemStandard.setSelected(false);
            calculateTotals();
        };

        View.OnClickListener standardListener = v -> {
            binding.rbFast.setChecked(false);
            binding.rbStandard.setChecked(true);
            binding.layoutItemFast.setSelected(false);
            binding.layoutItemStandard.setSelected(true);
            calculateTotals();
        };

        binding.layoutItemFast.setOnClickListener(fastListener);
        binding.layoutItemStandard.setOnClickListener(standardListener);
    }

    private void calculateTotals() {
        if (subtotalValue >= 100000) shippingFee = 0;
        else shippingFee = binding.rbFast.isChecked() ? 30000 : 15000;

        if (shippingFee == 0) {
            binding.tvShippingBadge.setVisibility(View.VISIBLE);
            binding.tvFastOldPrice.setVisibility(View.VISIBLE);
            binding.tvFastOldPrice.setPaintFlags(binding.tvFastOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvFastPrice.setText("MIỄN PHÍ");
            binding.tvFastPrice.setTextColor(0xFF2E7D32);
            binding.tvStandardOldPrice.setVisibility(View.VISIBLE);
            binding.tvStandardOldPrice.setPaintFlags(binding.tvStandardOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvStandardPrice.setText("MIỄN PHÍ");
            binding.tvStandardPrice.setTextColor(0xFF2E7D32);
            binding.tvSummaryShipping.setText("Miễn phí");
        } else {
            binding.tvShippingBadge.setVisibility(View.GONE);
            binding.tvFastOldPrice.setVisibility(View.GONE);
            binding.tvFastPrice.setText("30.000đ");
            binding.tvFastPrice.setTextColor(ContextCompat.getColor(this, R.color.blue_main));
            binding.tvStandardOldPrice.setVisibility(View.GONE);
            binding.tvStandardPrice.setText("15.000đ");
            binding.tvStandardPrice.setTextColor(ContextCompat.getColor(this, R.color.blue_main));
            binding.tvSummaryShipping.setText(formatter.format(shippingFee) + "đ");
        }

        binding.tvSummarySubtotal.setText(formatter.format(subtotalValue) + "đ");
        if (discountAmount > 0) {
            binding.layoutSummaryDiscount.setVisibility(View.VISIBLE);
            binding.tvSummaryDiscount.setText("-" + formatter.format(discountAmount) + "đ");
        } else {
            binding.layoutSummaryDiscount.setVisibility(View.GONE);
        }

        finalTotal = subtotalValue + shippingFee - discountAmount;
        if (finalTotal < 0) finalTotal = 0;
        String totalStr = formatter.format(finalTotal) + "đ";
        binding.tvSummaryTotal.setText(totalStr);
        binding.tvSummaryTotalSticky.setText(totalStr);

        if (binding.layoutBankDetails.getVisibility() == View.VISIBLE) {
            updateVietQR();
        }
    }

    private void displayProducts() {
        if (checkoutItems == null) return;
        binding.layoutProductItems.removeAllViews();
        subtotalValue = 0;
        for (CartItem item : checkoutItems) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_checkout_product, binding.layoutProductItems, false);
            ImageView ivProduct = view.findViewById(R.id.ivProduct);
            TextView tvName = view.findViewById(R.id.tvName);
            TextView tvPrice = view.findViewById(R.id.tvPrice);
            TextView tvQuantity = view.findViewById(R.id.tvQuantity);
            tvName.setText(item.getProductName());
            tvPrice.setText(formatter.format(item.getProductPrice()) + "đ");
            tvQuantity.setText("x" + item.getQuantity());
            String imgData = item.getProductImageUrl();
            if (imgData != null && !imgData.isEmpty()) {
                if (imgData.startsWith("http")) Glide.with(this).load(imgData).into(ivProduct);
                else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        ivProduct.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                    } catch (Exception e) {}
                }
            }
            subtotalValue += item.getProductPrice() * item.getQuantity();
            binding.layoutProductItems.addView(view);
        }
    }

    private void handlePlaceOrder() {
        if (editAddress == null || editAddress.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ nhận hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (binding.rbSelectZendo.isChecked()) {
            showPinBottomSheet();
        } else {
            proceedToPlaceOrder();
        }
    }

    private void showPinBottomSheet() {
        PinEntryBottomSheet bottomSheet = new PinEntryBottomSheet();
        bottomSheet.setOnPinVerifiedListener(this::proceedToPlaceOrder);
        bottomSheet.show(getSupportFragmentManager(), "PinEntry");
    }

    private void proceedToPlaceOrder() {
        String paymentMethod = "Tiền mặt";
        if (binding.rbSelectBank.isChecked()) paymentMethod = "Chuyển khoản";
        else if (binding.rbSelectZendo.isChecked()) paymentMethod = "Ví ZendoPay";

        WriteBatch batch = db.batch();
        for (CartItem item : checkoutItems) {
            if (item.getProductId() != null) {
                batch.update(db.collection("products").document(item.getProductId()), "stock", FieldValue.increment(-item.getQuantity()));
            }
            if (item.getId() != null) {
                batch.delete(db.collection("cart").document(item.getId()));
            }
        }
        Order order = new Order();
        order.setUserEmail(userEmail);
        order.setUserName(editName);
        order.setPhone(editPhone);
        order.setAddress(editAddress);
        order.setItems(checkoutItems);
        order.setSubtotal(subtotalValue);
        order.setShippingFee(shippingFee);
        order.setVoucherDiscount(discountAmount);
        order.setTotalAmount(finalTotal);
        order.setPaymentMethod(paymentMethod);
        order.setStatus("Chờ xác nhận");
        order.setTimestamp(new Date());
        order.setNote(binding.etOrderNote.getText().toString().trim());

        String orderId = db.collection("orders").document().getId();
        order.setId(orderId);
        batch.set(db.collection("orders").document(orderId), order);

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, ListActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }

    private void loadDefaultAddress() {
        if (userEmail.isEmpty()) return;
        db.collection("addresses").whereEqualTo("userEmail", userEmail).whereEqualTo("default", true).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        UserAddress addr = queryDocumentSnapshots.getDocuments().get(0).toObject(UserAddress.class);
                        if (addr != null) {
                            editName = addr.getFullName();
                            editPhone = addr.getPhone();
                            editAddress = addr.getFullAddress();
                            updateAddressUI();
                        }
                    } else {
                        db.collection("addresses").whereEqualTo("userEmail", userEmail).limit(1).get()
                                .addOnSuccessListener(snapshots -> {
                                    if (!snapshots.isEmpty()) {
                                        UserAddress addr = snapshots.getDocuments().get(0).toObject(UserAddress.class);
                                        if (addr != null) {
                                            editName = addr.getFullName();
                                            editPhone = addr.getPhone();
                                            editAddress = addr.getFullAddress();
                                            updateAddressUI();
                                        }
                                    } else updateAddressUI();
                                });
                    }
                });
    }

    private void updateAddressUI() {
        if (editAddress == null || editAddress.isEmpty()) {
            binding.tvCheckoutNamePhone.setVisibility(View.GONE); 
            binding.tvCheckoutAddress.setVisibility(View.GONE);
            binding.btnEditAddress.setVisibility(View.GONE); 
            binding.btnAddFirstAddress.setVisibility(View.VISIBLE);
        } else {
            binding.tvCheckoutNamePhone.setVisibility(View.VISIBLE); 
            binding.tvCheckoutAddress.setVisibility(View.VISIBLE);
            binding.btnEditAddress.setVisibility(View.VISIBLE); 
            binding.btnAddFirstAddress.setVisibility(View.GONE);
            binding.tvCheckoutNamePhone.setText(editName + " | " + editPhone);
            binding.tvCheckoutAddress.setText(editAddress);
        }
    }

    private void showAddressBottomSheet() {
        AddressPickerBottomSheet bottomSheet = AddressPickerBottomSheet.newInstance(userEmail);
        bottomSheet.setOnAddressSelectedListener(selected -> {
            editName = selected.getFullName();
            editPhone = selected.getPhone();
            editAddress = selected.getFullAddress();
            updateAddressUI();
        });
        bottomSheet.show(getSupportFragmentManager(), "AddressPicker");
    }

    private void showVoucherBottomSheet() {
        VoucherPickerBottomSheet bottomSheet = VoucherPickerBottomSheet.newInstance(subtotalValue);
        bottomSheet.setOnVoucherSelectedListener(this::applyVoucherObject);
        bottomSheet.show(getSupportFragmentManager(), "VoucherPicker");
    }

    private void applyVoucherObject(Voucher voucher) {
        if ("PERCENT".equalsIgnoreCase(voucher.getType())) {
            discountAmount = subtotalValue * (voucher.getValue() / 100.0);
            if (voucher.getMaxDiscount() > 0 && discountAmount > voucher.getMaxDiscount()) discountAmount = voucher.getMaxDiscount();
        } else discountAmount = voucher.getValue();
        if (discountAmount > subtotalValue) discountAmount = subtotalValue;
        binding.tvSelectedVoucher.setText(voucher.getCode() + " (-" + formatter.format(discountAmount) + "đ)");
        binding.tvSelectedVoucher.setTextColor(0xFF2E7D32);
        calculateTotals();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_ADDRESS && resultCode == RESULT_OK && data != null) {
            UserAddress selected = (UserAddress) data.getSerializableExtra("selected_address");
            if (selected != null) {
                editName = selected.getFullName();
                editPhone = selected.getPhone();
                editAddress = selected.getFullAddress();
                updateAddressUI();
            }
        }
    }
}
