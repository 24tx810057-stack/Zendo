package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CheckoutActivity extends AppCompatActivity {

    private TextView tvNamePhone, tvAddress, tvSummarySubtotal, tvSummaryShipping, tvSummaryTotal, tvSummaryTotalSticky;
    private TextView tvFastPrice, tvFastOldPrice, tvStandardPrice, tvStandardOldPrice, tvShippingBadge;
    private TextView tvSelectedVoucher, tvSummaryDiscount;
    private View layoutVoucher, layoutSummaryDiscount;
    private TextView btnEditAddress;
    private Button btnAddFirstAddress;
    private LinearLayout layoutProductItems;
    private RadioGroup rgPayment;
    private View btnPlaceOrder;
    private ImageView btnBack;
    private EditText etOrderNote;

    private View layoutItemFast, layoutItemStandard;
    private RadioButton rbFast, rbStandard;

    private FirebaseFirestore db;
    private String userEmail;
    private User currentUser;
    private List<CartItem> checkoutItems;
    private double subtotalValue = 0;
    private double shippingFee = 15000;
    private double discountAmount = 0;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    private String editName, editPhone, editAddress;
    private static final int REQUEST_CODE_SELECT_ADDRESS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");

        checkoutItems = (ArrayList<CartItem>) getIntent().getSerializableExtra("checkout_items");

        initViews();
        loadUserInfo();
        displayProducts();
        setupShippingSelection();
        calculateTotals();

        layoutVoucher.setOnClickListener(v -> showVoucherDialog());
        btnPlaceOrder.setOnClickListener(v -> handlePlaceOrder());
        btnBack.setOnClickListener(v -> finish());
        
        // SỬA TẠI ĐÂY: Mở AddressListActivity thay vì hiện Dialog cũ
        View.OnClickListener addressClickListener = v -> {
            Intent intent = new Intent(CheckoutActivity.this, AddressListActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SELECT_ADDRESS);
        };
        
        btnEditAddress.setOnClickListener(addressClickListener);
        btnAddFirstAddress.setOnClickListener(addressClickListener);
    }

    // NHẬN ĐỊA CHỈ TRẢ VỀ
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

    private void initViews() {
        tvNamePhone = findViewById(R.id.tvCheckoutNamePhone);
        tvAddress = findViewById(R.id.tvCheckoutAddress);
        btnEditAddress = findViewById(R.id.btnEditAddress);
        btnAddFirstAddress = findViewById(R.id.btnAddFirstAddress);
        layoutProductItems = findViewById(R.id.layoutProductItems);
        rgPayment = findViewById(R.id.rgPayment);
        tvSummarySubtotal = findViewById(R.id.tvSummarySubtotal);
        tvSummaryShipping = findViewById(R.id.tvSummaryShipping);
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        tvSummaryTotalSticky = findViewById(R.id.tvSummaryTotalSticky);
        
        tvFastPrice = findViewById(R.id.tvFastPrice);
        tvFastOldPrice = findViewById(R.id.tvFastOldPrice);
        tvStandardPrice = findViewById(R.id.tvStandardPrice);
        tvStandardOldPrice = findViewById(R.id.tvStandardOldPrice);
        tvShippingBadge = findViewById(R.id.tvShippingBadge);

        layoutVoucher = findViewById(R.id.layoutVoucher);
        tvSelectedVoucher = findViewById(R.id.tvSelectedVoucher);
        layoutSummaryDiscount = findViewById(R.id.layoutSummaryDiscount);
        tvSummaryDiscount = findViewById(R.id.tvSummaryDiscount);

        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        btnBack = findViewById(R.id.btnBackCheckout);

        layoutItemFast = findViewById(R.id.layoutItemFast);
        layoutItemStandard = findViewById(R.id.layoutItemStandard);
        rbFast = findViewById(R.id.rbFast);
        rbStandard = findViewById(R.id.rbStandard);
        
        etOrderNote = findViewById(R.id.etOrderNote);
    }

    private void setupShippingSelection() {
        rbStandard.setChecked(true);
        layoutItemStandard.setSelected(true);
        rbFast.setChecked(false);
        layoutItemFast.setSelected(false);

        View.OnClickListener fastListener = v -> {
            rbFast.setChecked(true);
            rbStandard.setChecked(false);
            layoutItemFast.setSelected(true);
            layoutItemStandard.setSelected(false);
            updateShippingFee();
            calculateTotals();
        };

        View.OnClickListener standardListener = v -> {
            rbFast.setChecked(false);
            rbStandard.setChecked(true);
            layoutItemFast.setSelected(false);
            layoutItemStandard.setSelected(true);
            updateShippingFee();
            calculateTotals();
        };

        layoutItemFast.setOnClickListener(fastListener);
        rbFast.setOnClickListener(fastListener);

        layoutItemStandard.setOnClickListener(standardListener);
        rbStandard.setOnClickListener(standardListener);
    }

    private void updateShippingFee() {
        if (subtotalValue >= 100000) {
            shippingFee = 0;
        } else {
            shippingFee = rbFast.isChecked() ? 30000 : 15000;
        }
    }

    private void showVoucherDialog() {
        db.collection("vouchers")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Voucher> availableVouchers = new ArrayList<>();
                    List<String> voucherOptions = new ArrayList<>();
                    long currentTime = System.currentTimeMillis();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Voucher voucher = doc.toObject(Voucher.class);
                            if (voucher == null) continue;
                            voucher.setId(doc.getId());

                            boolean isNotExpired = (voucher.getExpiryDate() == 0 || voucher.getExpiryDate() > currentTime);
                            boolean isMinOrderMet = (subtotalValue >= voucher.getMinOrder());

                            if (isNotExpired && isMinOrderMet) {
                                availableVouchers.add(voucher);
                                String valueStr = "PERCENT".equalsIgnoreCase(voucher.getType()) ? (int)voucher.getValue() + "%" : formatter.format(voucher.getValue()) + "đ";
                                voucherOptions.add(voucher.getCode() + " - Giảm " + valueStr);
                            }
                        } catch (Exception e) {}
                    }

                    if (availableVouchers.isEmpty()) {
                        Toast.makeText(this, "Không có voucher nào khả dụng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Chọn Voucher")
                            .setItems(voucherOptions.toArray(new String[0]), (dialog, which) -> {
                                applyVoucherObject(availableVouchers.get(which));
                            })
                            .show();
                });
    }

    private void applyVoucherObject(Voucher voucher) {
        if ("PERCENT".equalsIgnoreCase(voucher.getType())) {
            discountAmount = subtotalValue * (voucher.getValue() / 100.0);
            if (voucher.getMaxDiscount() > 0 && discountAmount > voucher.getMaxDiscount()) {
                discountAmount = voucher.getMaxDiscount();
            }
        } else {
            discountAmount = voucher.getValue();
        }
        if (discountAmount > subtotalValue) discountAmount = subtotalValue;

        tvSelectedVoucher.setText(voucher.getCode() + " (-" + formatter.format(discountAmount) + "đ)");
        tvSelectedVoucher.setTextColor(0xFF2E7D32);
        calculateTotals();
    }

    private void loadUserInfo() {
        if (userEmail.isEmpty()) return;
        db.collection("users").whereEqualTo("email", userEmail).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        currentUser = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        if (currentUser != null) {
                            editName = currentUser.getFullName();
                            editPhone = currentUser.getPhone();
                            editAddress = currentUser.getAddress();
                            updateAddressUI();
                        }
                    }
                });
    }

    private void updateAddressUI() {
        if (editAddress == null || editAddress.isEmpty()) {
            tvNamePhone.setVisibility(View.GONE);
            tvAddress.setVisibility(View.GONE);
            btnEditAddress.setVisibility(View.GONE);
            btnAddFirstAddress.setVisibility(View.VISIBLE);
        } else {
            tvNamePhone.setVisibility(View.VISIBLE);
            tvAddress.setVisibility(View.VISIBLE);
            btnEditAddress.setVisibility(View.VISIBLE);
            btnAddFirstAddress.setVisibility(View.GONE);
            tvNamePhone.setText(editName + " | " + editPhone);
            tvAddress.setText(editAddress);
        }
    }

    private void displayProducts() {
        if (checkoutItems == null) return;
        layoutProductItems.removeAllViews();
        subtotalValue = 0;
        for (CartItem item : checkoutItems) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_checkout_product, layoutProductItems, false);
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
            layoutProductItems.addView(view);
        }
    }

    private void calculateTotals() {
        updateShippingFee();
        
        if (shippingFee == 0) {
            tvShippingBadge.setVisibility(View.VISIBLE);
            
            tvFastOldPrice.setVisibility(View.VISIBLE);
            tvFastOldPrice.setPaintFlags(tvFastOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvFastPrice.setText("MIỄN PHÍ");
            tvFastPrice.setTextColor(0xFF2E7D32);

            tvStandardOldPrice.setVisibility(View.VISIBLE);
            tvStandardOldPrice.setPaintFlags(tvStandardOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvStandardPrice.setText("MIỄN PHÍ");
            tvStandardPrice.setTextColor(0xFF2E7D32);
            
            tvSummaryShipping.setText("Miễn phí");
        } else {
            tvShippingBadge.setVisibility(View.GONE);
            tvFastOldPrice.setVisibility(View.GONE);
            tvFastPrice.setText("30.000đ");
            tvFastPrice.setTextColor(ContextCompat.getColor(this, R.color.blue_main));

            tvStandardOldPrice.setVisibility(View.GONE);
            tvStandardPrice.setText("15.000đ");
            tvStandardPrice.setTextColor(ContextCompat.getColor(this, R.color.blue_main));
            
            tvSummaryShipping.setText(formatter.format(shippingFee) + "đ");
        }

        tvSummarySubtotal.setText(formatter.format(subtotalValue) + "đ");
        
        if (discountAmount > 0) {
            layoutSummaryDiscount.setVisibility(View.VISIBLE);
            tvSummaryDiscount.setText("-" + formatter.format(discountAmount) + "đ");
        } else {
            layoutSummaryDiscount.setVisibility(View.GONE);
        }

        double total = subtotalValue + shippingFee - discountAmount;
        if (total < 0) total = 0;
        String totalStr = formatter.format(total) + "đ";
        tvSummaryTotal.setText(totalStr);
        tvSummaryTotalSticky.setText(totalStr);
    }

    private void handlePlaceOrder() {
        if (editAddress == null || editAddress.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ nhận hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        String paymentMethod = "";
        int checkedPaymentId = rgPayment.getCheckedRadioButtonId();
        if (checkedPaymentId == R.id.rbCash) paymentMethod = "Tiền mặt";
        else if (checkedPaymentId == R.id.rbBank) paymentMethod = "Chuyển khoản";
        else if (checkedPaymentId == R.id.rbBeepay) paymentMethod = "Ví ZendoPay";

        double finalAmount = subtotalValue + shippingFee - discountAmount;
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
        order.setTotalAmount(finalAmount);
        order.setPaymentMethod(paymentMethod);
        order.setStatus("Chờ xác nhận");
        order.setTimestamp(new Date());
        order.setNote(etOrderNote.getText().toString().trim());

        String orderId = db.collection("orders").document().getId();
        order.setId(orderId);
        batch.set(db.collection("orders").document(orderId), order);

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, ListActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }
}
