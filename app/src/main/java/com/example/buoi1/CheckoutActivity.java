package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private TextView tvNamePhone, tvAddress, tvSummarySubtotal, tvSummaryShipping, tvSummaryTotal, tvSummaryTotalSticky;
    private TextView tvFastPrice, tvFastOldPrice, tvStandardPrice, tvStandardOldPrice, tvShippingBadge;
    private TextView tvSelectedVoucher, tvSummaryDiscount;
    private View layoutVoucher, layoutSummaryDiscount;
    private TextView btnEditAddress;
    private LinearLayout layoutProductItems;
    private RadioGroup rgPayment;
    private View btnPlaceOrder;
    private ImageView btnBack;

    private View layoutItemFast, layoutItemStandard;
    private RadioButton rbFast, rbStandard;

    private FirebaseFirestore db;
    private String userEmail;
    private User currentUser;
    private List<CartItem> checkoutItems;
    private double subtotal = 0;
    private double shippingFee = 15000;
    private double discountAmount = 0;
    private String selectedVoucherCode = "";
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    private String editName, editPhone, editAddress;

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
        btnEditAddress.setOnClickListener(v -> showEditAddressDialog());
    }

    private void initViews() {
        tvNamePhone = findViewById(R.id.tvCheckoutNamePhone);
        tvAddress = findViewById(R.id.tvCheckoutAddress);
        btnEditAddress = findViewById(R.id.btnEditAddress);
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
    }

    private void setupShippingSelection() {
        rbStandard.setChecked(true);
        rbFast.setChecked(false);

        View.OnClickListener fastListener = v -> {
            rbFast.setChecked(true);
            rbStandard.setChecked(false);
            updateShippingFee();
            calculateTotals();
        };

        View.OnClickListener standardListener = v -> {
            rbFast.setChecked(false);
            rbStandard.setChecked(true);
            updateShippingFee();
            calculateTotals();
        };

        layoutItemFast.setOnClickListener(fastListener);
        rbFast.setOnClickListener(fastListener);

        layoutItemStandard.setOnClickListener(standardListener);
        rbStandard.setOnClickListener(standardListener);
    }

    private void updateShippingFee() {
        if (subtotal >= 100000) {
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
                            boolean isMinOrderMet = (subtotal >= voucher.getMinOrder());

                            if (isNotExpired && isMinOrderMet) {
                                availableVouchers.add(voucher);
                                
                                String valueStr = "percent".equals(voucher.getType()) ? 
                                        (int)voucher.getValue() + "%" : formatter.format(voucher.getValue()) + "đ";
                                String vTitle = (voucher.getTitle() != null && !voucher.getTitle().isEmpty()) ? voucher.getTitle() : "Ưu đãi hấp dẫn";
                                String info = voucher.getCode() + " - " + vTitle + " (Giảm " + valueStr + ")";

                                voucherOptions.add(info);
                            }
                        } catch (Exception e) {}
                    }

                    if (availableVouchers.isEmpty()) {
                        Toast.makeText(this, "Không có voucher nào khả dụng cho đơn hàng này", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] optionsArray = voucherOptions.toArray(new String[0]);
                    new AlertDialog.Builder(this)
                            .setTitle("Chọn Zendo Voucher")
                            .setItems(optionsArray, (dialog, which) -> {
                                Voucher selected = availableVouchers.get(which);
                                applyVoucherObject(selected);
                            })
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyVoucherObject(Voucher voucher) {
        selectedVoucherCode = voucher.getCode();
        double amount = 0;
        if ("percent".equals(voucher.getType())) {
            amount = subtotal * (voucher.getValue() / 100.0);
        } else {
            amount = voucher.getValue();
        }
        
        if (amount > subtotal) amount = subtotal;

        discountAmount = amount;
        tvSelectedVoucher.setText(voucher.getCode() + " (-" + formatter.format(amount) + "đ)");
        tvSelectedVoucher.setTextColor(0xFF2E7D32);
        calculateTotals();
        Toast.makeText(this, "Đã áp dụng mã giảm giá: " + voucher.getCode(), Toast.LENGTH_SHORT).show();
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
        String nameStr = (editName != null && !editName.isEmpty()) ? editName : "Họ tên";
        String phoneStr = (editPhone != null && !editPhone.isEmpty()) ? editPhone : userEmail;
        tvNamePhone.setText(nameStr + " | " + phoneStr);
        tvAddress.setText((editAddress != null && !editAddress.isEmpty()) ? editAddress : "Chưa có địa chỉ");
    }

    private void showEditAddressDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_checkout_address, null);
        EditText etName = dialogView.findViewById(R.id.etEditName);
        EditText etPhone = dialogView.findViewById(R.id.etEditPhone);
        EditText etAddress = dialogView.findViewById(R.id.etEditAddress);

        etName.setText(editName);
        etPhone.setText(editPhone);
        etAddress.setText(editAddress);

        new AlertDialog.Builder(this)
                .setTitle("Thông tin nhận hàng")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    editName = etName.getText().toString().trim();
                    editPhone = etPhone.getText().toString().trim();
                    editAddress = etAddress.getText().toString().trim();
                    updateAddressUI();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void displayProducts() {
        if (checkoutItems == null) return;
        layoutProductItems.removeAllViews();
        subtotal = 0;
        for (CartItem item : checkoutItems) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_cart, layoutProductItems, false);
            if(view.findViewById(R.id.cbSelectItem) != null) view.findViewById(R.id.cbSelectItem).setVisibility(View.GONE);
            if(view.findViewById(R.id.btnMinus) != null) view.findViewById(R.id.btnMinus).setVisibility(View.GONE);
            if(view.findViewById(R.id.btnPlus) != null) view.findViewById(R.id.btnPlus).setVisibility(View.GONE);

            ImageView ivProduct = view.findViewById(R.id.ivCartProduct);
            TextView tvName = view.findViewById(R.id.tvCartProductName);
            TextView tvPrice = view.findViewById(R.id.tvCartProductPrice);
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
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivProduct.setImageBitmap(decodedByte);
                    } catch (Exception e) {}
                }
            }
            subtotal += item.getProductPrice() * item.getQuantity();
            layoutProductItems.addView(view);
        }
    }

    private void calculateTotals() {
        updateShippingFee();
        if (shippingFee == 0) {
            tvSummaryShipping.setText("Miễn phí");
            tvShippingBadge.setVisibility(View.VISIBLE);
            
            tvFastOldPrice.setVisibility(View.VISIBLE);
            tvFastOldPrice.setPaintFlags(tvFastOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvFastPrice.setText("MIỄN PHÍ");
            tvFastPrice.setTextColor(0xFF2E7D32);

            tvStandardOldPrice.setVisibility(View.VISIBLE);
            tvStandardOldPrice.setPaintFlags(tvStandardOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvStandardPrice.setText("MIỄN PHÍ");
            tvStandardPrice.setTextColor(0xFF2E7D32);
        } else {
            tvShippingBadge.setVisibility(View.GONE);
            tvFastOldPrice.setVisibility(View.GONE);
            tvStandardOldPrice.setVisibility(View.GONE);
            
            tvFastPrice.setText("30.000đ");
            tvFastPrice.setTextColor(0xFF01579B);
            tvStandardPrice.setText("15.000đ");
            tvStandardPrice.setTextColor(0xFF01579B);

            tvSummaryShipping.setText(formatter.format(shippingFee) + "đ");
        }
        
        tvSummarySubtotal.setText(formatter.format(subtotal) + "đ");
        
        if (discountAmount > 0) {
            layoutSummaryDiscount.setVisibility(View.VISIBLE);
            tvSummaryDiscount.setText("-" + formatter.format(discountAmount) + "đ");
        } else {
            layoutSummaryDiscount.setVisibility(View.GONE);
        }

        double total = subtotal + shippingFee - discountAmount;
        if (total < 0) total = 0;
        
        String totalStr = formatter.format(total) + "đ";
        tvSummaryTotal.setText(totalStr);
        if (tvSummaryTotalSticky != null) tvSummaryTotalSticky.setText(totalStr);
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

        double finalAmount = subtotal + shippingFee - discountAmount;
        WriteBatch batch = db.batch();
        
        for (CartItem item : checkoutItems) {
            if (item.getProductId() != null && !item.getProductId().isEmpty()) {
                batch.update(db.collection("products").document(item.getProductId()),
                        "soldCount", FieldValue.increment(item.getQuantity()),
                        "stock", FieldValue.increment(-item.getQuantity()));
            }
            if (item.getId() != null && !item.getId().isEmpty()) {
                batch.delete(db.collection("cart").document(item.getId()));
            }
        }

        Order order = new Order();
        order.setUserEmail(userEmail);
        order.setUserName(editName);
        order.setPhone(editPhone);
        order.setAddress(editAddress);
        order.setItems(checkoutItems);
        order.setTotalAmount(finalAmount);
        order.setShippingFee(shippingFee);
        order.setPaymentMethod(paymentMethod);
        order.setStatus("Chờ xác nhận");
        order.setTimestamp(new Date());

        String orderId = db.collection("orders").document().getId();
        order.setId(orderId);
        batch.set(db.collection("orders").document(orderId), order);

        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        
        Map<String, Object> userNotif = new HashMap<>();
        userNotif.put("userEmail", userEmail);
        userNotif.put("title", "Đặt hàng thành công");
        userNotif.put("message", "Đơn hàng của bạn đã được đặt thành công. Tổng tiền: " + formatter.format(finalAmount) + "đ");
        userNotif.put("date", dateStr);
        userNotif.put("read", false);
        userNotif.put("timestamp", FieldValue.serverTimestamp());
        batch.set(db.collection("notifications").document(), userNotif);

        Map<String, Object> adminNotif = new HashMap<>();
        adminNotif.put("userEmail", "admin");
        adminNotif.put("title", "Đơn hàng mới chờ duyệt");
        adminNotif.put("message", "Khách hàng " + editName + " vừa đặt một đơn hàng mới. Tổng tiền: " + formatter.format(finalAmount) + "đ");
        adminNotif.put("date", dateStr);
        adminNotif.put("read", false);
        adminNotif.put("timestamp", FieldValue.serverTimestamp());
        batch.set(db.collection("notifications").document(), adminNotif);

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, ListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}
