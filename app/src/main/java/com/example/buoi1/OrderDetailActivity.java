package com.example.buoi1;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView tvStatus, tvOrderDate, tvDeliveryDate, tvNamePhone, tvAddress, tvNote, tvOrderIdDisplay, tvReturnStatusDesc;
    private TextView tvSubtotal, tvShipping, tvVoucher, tvTotal, tvPaymentMethod;
    private View layoutVoucherContainer, layoutReturnInfo;
    private LinearLayout layoutProducts;
    private Button btnMainAction, btnCancelAction;

    private Order order;
    private OrderManager orderManager;
    private String userEmail, userRole;
    private final DecimalFormat formatter = new DecimalFormat("###,###,###");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private FirebaseFirestore db;
    private ListenerRegistration orderListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");
        userRole = sharedPref.getString("user_role", "user");
        
        order = (Order) getIntent().getSerializableExtra("order_data");
        orderManager = new OrderManager();

        initViews();
        observeOrderChanges(); 

        if (getIntent().getBooleanExtra("trigger_review", false)) {
            startAddReviewActivity();
        }
    }

    private void observeOrderChanges() {
        if (order == null) return;
        orderListener = db.collection("orders").document(order.getId())
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    Order updatedOrder = value.toObject(Order.class);
                    if (updatedOrder != null) {
                        this.order = updatedOrder;
                        this.order.setId(value.getId());
                        displayOrderDetails();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (orderListener != null) orderListener.remove();
        super.onDestroy();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvOrderStatus);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvDeliveryDate = findViewById(R.id.tvDeliveryDate);
        tvNamePhone = findViewById(R.id.tvReceiverNamePhone);
        tvAddress = findViewById(R.id.tvReceiverAddress);
        tvNote = findViewById(R.id.tvOrderNote);
        tvOrderIdDisplay = findViewById(R.id.tvOrderIdDetail);
        tvReturnStatusDesc = findViewById(R.id.tvReturnStatusDesc);
        
        tvSubtotal = findViewById(R.id.tvOrderSubtotal);
        tvShipping = findViewById(R.id.tvOrderShipping);
        tvVoucher = findViewById(R.id.tvOrderVoucher);
        tvTotal = findViewById(R.id.tvOrderTotal);
        tvPaymentMethod = findViewById(R.id.tvOrderPaymentMethod);
        
        layoutVoucherContainer = findViewById(R.id.layoutOrderVoucher);
        layoutReturnInfo = findViewById(R.id.layoutReturnInfo);
        layoutProducts = findViewById(R.id.layoutOrderProducts);
        
        btnMainAction = findViewById(R.id.btnMainAction);
        btnCancelAction = findViewById(R.id.btnCancelOrder);

        findViewById(R.id.btnBackOrder).setOnClickListener(v -> finish());
    }

    private void startAddReviewActivity() {
        Intent intent = new Intent(this, ListActivity.class);
        intent.putExtra("open_review_dialog", true);
        intent.putExtra("review_order_id", order.getId());
        intent.putExtra("review_product_id", (order.getItems() != null && !order.getItems().isEmpty()) ? order.getItems().get(0).getProductId() : "");
        startActivity(intent);
    }

    private void displayOrderDetails() {
        if (order == null) return;

        // Nếu đơn đã hủy, chuyển sang trang chi tiết hủy (nếu là user)
        if ("Đã hủy".equals(order.getStatus()) && !"admin".equals(userRole)) {
            Intent intent = new Intent(this, CancelOrderDetailActivity.class);
            intent.putExtra("order_data", order);
            startActivity(intent);
            finish();
            return;
        }

        tvOrderIdDisplay.setText("#" + order.getId().toUpperCase());
        tvStatus.setText(order.getStatus());
        tvOrderDate.setText("Ngày đặt hàng: " + (order.getTimestamp() != null ? dateFormat.format(order.getTimestamp()) : "---"));
        
        if (order.getDeliveryDate() != null) {
            tvDeliveryDate.setText("Giao hàng thành công: " + dateFormat.format(order.getDeliveryDate()));
            tvDeliveryDate.setVisibility(View.VISIBLE);
        } else {
            tvDeliveryDate.setVisibility(View.GONE);
        }

        tvNamePhone.setText(order.getUserName() + " | " + order.getPhone());
        tvAddress.setText(order.getAddress());
        tvNote.setText((order.getNote() == null || order.getNote().isEmpty()) ? "Không có ghi chú" : order.getNote());

        tvSubtotal.setText(formatter.format(order.getSubtotal()) + "đ");
        tvShipping.setText("+" + formatter.format(order.getShippingFee()) + "đ");
        
        if (order.getVoucherDiscount() > 0) {
            layoutVoucherContainer.setVisibility(View.VISIBLE);
            tvVoucher.setText("-" + formatter.format(order.getVoucherDiscount()) + "đ");
        } else {
            layoutVoucherContainer.setVisibility(View.GONE);
        }
        
        tvTotal.setText(formatter.format(order.getTotalAmount()) + "đ");
        tvPaymentMethod.setText(order.getPaymentMethod());

        displayProducts(order.getItems());
        setupReturnBanner();

        db.collection("warranty_requests")
                .whereEqualTo("orderId", order.getId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    setupFooterActions(!snapshots.isEmpty());
                });
    }

    private void setupReturnBanner() {
        String status = order.getStatus();
        if ("Yêu cầu trả hàng".equals(status)) {
            layoutReturnInfo.setVisibility(View.VISIBLE);
            tvReturnStatusDesc.setText("Hệ thống đang xem xét yêu cầu trả hàng/hoàn tiền của bạn. Vui lòng chờ phản hồi từ Shop.");
            tvReturnStatusDesc.setTextColor(0xFFE65100);
        } else if ("Đã trả hàng".equals(status)) {
            layoutReturnInfo.setVisibility(View.VISIBLE);
            tvReturnStatusDesc.setText("Yêu cầu hoàn tiền đã được chấp nhận. Tiền sẽ được hoàn về trong vòng 3-5 ngày làm việc.");
            tvReturnStatusDesc.setTextColor(0xFF2E7D32);
        } else {
            layoutReturnInfo.setVisibility(View.GONE);
        }
    }

    private void displayProducts(List<CartItem> items) {
        if (items == null) return;
        layoutProducts.removeAllViews();
        for (CartItem item : items) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_checkout_product, layoutProducts, false);
            TextView tvName = view.findViewById(R.id.tvName);
            TextView tvPrice = view.findViewById(R.id.tvPrice);
            TextView tvQuantity = view.findViewById(R.id.tvQuantity);
            ImageView ivProduct = view.findViewById(R.id.ivProduct);

            tvName.setText(item.getProductName());
            tvPrice.setText(formatter.format(item.getProductPrice()) + "đ");
            tvQuantity.setText("x" + item.getQuantity());

            LinearLayout itemContainer = (LinearLayout)view.findViewById(R.id.tvName).getParent();
            String itemWarranty = item.getWarranty();
            
            if (itemWarranty != null && !itemWarranty.isEmpty()) {
                displayItemWarranty(itemContainer, item, itemWarranty);
            } else {
                db.collection("products").document(item.getProductId()).get()
                        .addOnSuccessListener(doc -> {
                            String warrantyToUse = "12 tháng"; 
                            if (doc.exists() && doc.getString("warranty") != null && !doc.getString("warranty").isEmpty()) {
                                warrantyToUse = doc.getString("warranty");
                            }
                            displayItemWarranty(itemContainer, item, warrantyToUse);
                        })
                        .addOnFailureListener(e -> {
                            displayItemWarranty(itemContainer, item, "12 tháng");
                        });
            }

            String imgData = item.getProductImageUrl();
            if (imgData != null && !imgData.isEmpty()) {
                if (imgData.startsWith("http")) Glide.with(this).load(imgData).into(ivProduct);
                else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivProduct.setImageBitmap(bitmap);
                    } catch (Exception e) {}
                }
            }
            layoutProducts.addView(view);
        }
    }

    private void setupFooterActions(boolean hasWarrantyRequest) {
        String status = order.getStatus();
        btnMainAction.setVisibility(View.VISIBLE);
        btnMainAction.setEnabled(true);
        btnMainAction.setAlpha(1.0f);
        btnCancelAction.setVisibility(View.GONE);
        btnCancelAction.setEnabled(true);
        btnCancelAction.setAlpha(1.0f);

        if ("admin".equals(userRole)) {
            if ("Yêu cầu hủy".equals(status)) {
                btnMainAction.setText("Đồng ý hủy đơn");
                btnMainAction.setOnClickListener(v -> updateStatus("Đã hủy"));
                btnCancelAction.setVisibility(View.VISIBLE);
                btnCancelAction.setText("Từ chối hủy");
                btnCancelAction.setOnClickListener(v -> {
                    // Khi Admin từ chối hủy -> Đánh dấu flag để user không gửi lại được nữa
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "Chờ xác nhận");
                    updates.put("cancelRejected", true);
                    db.collection("orders").document(order.getId()).update(updates)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã từ chối yêu cầu hủy", Toast.LENGTH_SHORT).show());
                });
            } else if ("Chờ xác nhận".equals(status)) {
                btnMainAction.setText("Xác nhận đơn hàng");
                btnMainAction.setOnClickListener(v -> updateStatus("Chờ lấy hàng"));
                btnCancelAction.setVisibility(View.VISIBLE);
                btnCancelAction.setText("TỪ CHỐI ĐƠN");
                btnCancelAction.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Từ chối đơn hàng")
                            .setMessage("Bạn chắc chắn muốn từ chối đơn hàng này?")
                            .setPositiveButton("Đúng", (d, w) -> updateStatus("Đã hủy"))
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            } else if ("Chờ lấy hàng".equals(status)) {
                btnMainAction.setText("Giao cho ĐVVC");
                btnMainAction.setOnClickListener(v -> updateStatus("Đang giao"));
            } else if ("Đang giao".equals(status)) {
                btnMainAction.setText("Xác nhận đã giao");
                btnMainAction.setOnClickListener(v -> updateStatus("Đã giao"));
            } else {
                btnMainAction.setVisibility(View.GONE);
            }
        } else {
            if ("Chờ xác nhận".equals(status)) {
                // CHỈ CHO PHÉP HỦY KHI CHƯA XÁC NHẬN
                btnMainAction.setText("Hủy đơn hàng");
                btnMainAction.setOnClickListener(v -> showCancelReasonDialog());
            } else if ("Chờ lấy hàng".equals(status)) {
                // CHỈ HIỆN NÚT YÊU CẦU HỦY NẾU ADMIN CHƯA TỪ CHỐI TRƯỚC ĐÓ
                if (order.isCancelRejected()) {
                    btnMainAction.setVisibility(View.GONE);
                } else {
                    btnMainAction.setText("Gửi yêu cầu hủy");
                    btnMainAction.setOnClickListener(v -> updateStatus("Yêu cầu hủy"));
                }
            } else if ("Đang giao".equals(status) || "Đã giao".equals(status)) {
                btnMainAction.setText("Đã nhận được hàng");
                btnMainAction.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Xác nhận")
                            .setMessage("Bạn đã thực sự nhận được hàng và hài lòng?")
                            .setPositiveButton("Đã nhận", (d, w) -> updateStatus("Hoàn thành"))
                            .setNegativeButton("Chưa", null)
                            .show();
                });
            } else if ("Yêu cầu hủy".equals(status)) {
                btnMainAction.setText("Đang chờ duyệt hủy...");
                btnMainAction.setEnabled(false);
                btnMainAction.setAlpha(0.6f);
            } else if ("Hoàn thành".equals(status)) {
                long currentTime = System.currentTimeMillis();
                long deliveryTime = order.getDeliveryDate() != null ? order.getDeliveryDate().getTime() : currentTime;
                long fifteenDaysInMillis = 15L * 24 * 60 * 60 * 1000;
                long maxExpiryTime = deliveryTime + (365L * 24 * 60 * 60 * 1000); 

                if (currentTime - deliveryTime < fifteenDaysInMillis) {
                    btnCancelAction.setVisibility(View.VISIBLE);
                    btnCancelAction.setText("TRẢ HÀNG/HOÀN TIỀN");
                    btnCancelAction.setOnClickListener(v -> {
                        Intent intent = new Intent(this, RequestReturnActivity.class);
                        intent.putExtra("order_data", order);
                        startActivity(intent);
                    });
                } else {
                    if (maxExpiryTime > currentTime) {
                        btnCancelAction.setVisibility(View.VISIBLE);
                        if (hasWarrantyRequest) {
                            btnCancelAction.setText("ĐÃ GỬI YÊU CẦU BH");
                            btnCancelAction.setEnabled(false);
                            btnCancelAction.setAlpha(0.6f);
                        } else {
                            btnCancelAction.setText("YÊU CẦU BẢO HÀNH");
                            btnCancelAction.setOnClickListener(v -> {
                                if (order.getItems() != null && !order.getItems().isEmpty()) {
                                    CartItem firstItem = order.getItems().get(0);
                                    Date expiryDate = calculateExpiryDate(order.getDeliveryDate(), firstItem.getWarranty());
                                    Intent intent = new Intent(this, RequestWarrantyActivity.class);
                                    intent.putExtra("order_id", order.getId());
                                    intent.putExtra("user_email", order.getUserEmail());
                                    intent.putExtra("product_id", firstItem.getProductId());
                                    intent.putExtra("product_name", firstItem.getProductName());
                                    intent.putExtra("product_image", firstItem.getProductImageUrl());
                                    intent.putExtra("expiry_date_long", expiryDate != null ? expiryDate.getTime() : 0);
                                    startActivity(intent);
                                }
                            });
                        }
                    } else {
                        btnCancelAction.setVisibility(View.VISIBLE);
                        btnCancelAction.setText("HẾT HẠN BẢO HÀNH");
                        btnCancelAction.setEnabled(false);
                        btnCancelAction.setAlpha(0.5f);
                    }
                }

                if (order.isReviewed()) {
                    btnMainAction.setText("Tiếp tục mua sắm");
                    btnMainAction.setOnClickListener(v -> navigateToHome());
                } else {
                    db.collection("reviews").whereEqualTo("orderId", order.getId()).get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    btnMainAction.setText("Tiếp tục mua sắm");
                                    btnMainAction.setOnClickListener(v -> navigateToHome());
                                    db.collection("orders").document(order.getId()).update("reviewed", true);
                                } else {
                                    btnMainAction.setText("Đánh giá sản phẩm");
                                    btnMainAction.setOnClickListener(v -> startAddReviewActivity());
                                }
                            });
                }
            } else {
                btnMainAction.setText("Tiếp tục mua sắm");
                btnMainAction.setOnClickListener(v -> navigateToHome());
            }
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, ListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void displayItemWarranty(LinearLayout container, CartItem item, String warranty) {
        String warrantyText = warranty;
        if (warrantyText != null && warrantyText.matches("\\d+")) warrantyText += " tháng";
        final String finalWarrantyDisplay = (warrantyText == null || warrantyText.isEmpty()) ? "12 tháng" : warrantyText;

        TextView tvWarrantyItem = new TextView(this);
        tvWarrantyItem.setText("Bảo hành: " + finalWarrantyDisplay);
        tvWarrantyItem.setTextSize(11);
        tvWarrantyItem.setTextColor(0xFF757575);
        container.addView(tvWarrantyItem); 
        
        db.collection("warranty_requests")
                .whereEqualTo("orderId", order.getId())
                .whereEqualTo("productId", item.getProductId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    String statusBadge = "";
                    if (!snapshots.isEmpty()) {
                        String status = snapshots.getDocuments().get(0).getString("status");
                        if ("pending_repair".equals(status)) statusBadge = " [Chờ tiếp nhận]";
                        else if ("repairing".equals(status)) statusBadge = " [Đang sửa chữa]";
                        else if ("repaired".equals(status)) statusBadge = " [Đã sửa xong]";
                        else if ("rejected".equals(status)) statusBadge = " [Từ chối bảo hành]";
                    }

                    String currentStatus = order.getStatus();
                    if (("Hoàn thành".equals(currentStatus) || "Đã giao".equals(currentStatus))) {
                        Date startDate = order.getDeliveryDate();
                        if (startDate == null) startDate = order.getTimestamp(); 

                        if (startDate != null) {
                            Date expiryDate = calculateExpiryDate(startDate, finalWarrantyDisplay);
                            if (expiryDate != null) {
                                String expiryStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expiryDate);
                                tvWarrantyItem.setText("Bảo hành: " + finalWarrantyDisplay + " (Hết hạn: " + expiryStr + ")");
                                if (!statusBadge.isEmpty()) tvWarrantyItem.append(statusBadge);
                                if (expiryDate.before(new Date())) tvWarrantyItem.setTextColor(0xFFF44336);
                                else tvWarrantyItem.setTextColor(0xFF2E7D32);

                                tvWarrantyItem.setPaintFlags(tvWarrantyItem.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                                Date finalStartDate = startDate;
                                tvWarrantyItem.setOnClickListener(v -> showWarrantyCard(item, finalStartDate, expiryDate, finalWarrantyDisplay));
                            }
                        }
                    } else if (!statusBadge.isEmpty()) {
                        tvWarrantyItem.append(statusBadge);
                    }
                });
    }

    private void showWarrantyCard(CartItem item, Date deliveryDate, Date expiryDate, String warranty) {
        db.collection("warranty_requests")
                .whereEqualTo("orderId", order.getId())
                .whereEqualTo("productId", item.getProductId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    String status = null;
                    if (!snapshots.isEmpty()) {
                        status = snapshots.getDocuments().get(0).getString("status");
                    }
                    showWarrantyCardUI(item, deliveryDate, expiryDate, status);
                });
    }

    private void showWarrantyCardUI(CartItem item, Date deliveryDate, Date expiryDate, String requestStatus) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_warranty_card, null);
        builder.setView(view);

        TextView tvName = view.findViewById(R.id.tvCardProductName);
        TextView tvId = view.findViewById(R.id.tvCardOrderId);
        TextView tvActive = view.findViewById(R.id.tvCardActivationDate);
        TextView tvExpire = view.findViewById(R.id.tvCardExpiryDate);
        TextView tvStatusCard = view.findViewById(R.id.tvCardStatus);
        Button btnClose = view.findViewById(R.id.btnCloseCard);
        Button btnContact = view.findViewById(R.id.btnContactWarranty);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        tvName.setText(item.getProductName());
        String fullId = order.getId();
        tvId.setText("ID: #" + (fullId.length() > 8 ? fullId.substring(fullId.length() - 8).toUpperCase() : fullId.toUpperCase()));
        tvActive.setText(sdf.format(deliveryDate));
        tvExpire.setText(sdf.format(expiryDate));

        if (expiryDate.before(new Date())) {
            tvStatusCard.setText("Trạng thái: Hết hạn bảo hành");
            tvStatusCard.setTextColor(0xFFF44336);
            tvStatusCard.setBackgroundColor(0xFFFFEBEE);
            if (btnContact != null) btnContact.setVisibility(View.GONE);
        } else {
            tvStatusCard.setText("Trạng thái: Đang bảo hành");
            tvStatusCard.setTextColor(0xFF2E7D32);
            tvStatusCard.setBackgroundColor(0xFFE8F5E9);
            
            if (btnContact != null) {
                if (requestStatus != null) {
                    btnContact.setEnabled(false);
                    btnContact.setAlpha(0.6f);
                    if ("pending_repair".equals(requestStatus)) btnContact.setText("ĐANG CHỜ TIẾP NHẬN");
                    else if ("repairing".equals(requestStatus)) btnContact.setText("ĐANG TRONG QUÁ TRÌNH SỬA CHỮA");
                    else if ("repaired".equals(requestStatus)) btnContact.setText("ĐÃ SỬA XONG");
                    else btnContact.setText("ĐÃ GỬI YÊU CẦU");
                } else btnContact.setVisibility(View.VISIBLE);
            }
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if (btnContact != null && requestStatus == null && !expiryDate.before(new Date())) {
             btnContact.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, RequestWarrantyActivity.class);
                intent.putExtra("order_id", order.getId());
                intent.putExtra("user_email", order.getUserEmail());
                intent.putExtra("product_id", item.getProductId());
                intent.putExtra("product_name", item.getProductName());
                intent.putExtra("product_image", item.getProductImageUrl());
                intent.putExtra("expiry_date_long", expiryDate.getTime());
                startActivity(intent);
            });
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showCancelReasonDialog() {
        String[] reasons = {
                "Tôi muốn thay đổi địa chỉ nhận hàng",
                "Tôi muốn thay đổi sản phẩm (màu sắc, kích thước...)",
                "Tìm thấy giá rẻ hơn ở nơi khác",
                "Đổi ý, không muốn mua nữa",
                "Khác (Vui lòng ghi chú)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Lý do hủy đơn hàng")
                .setItems(reasons, (dialog, which) -> {
                    String selectedReason = reasons[which];
                    performCancellation(selectedReason);
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    private void performCancellation(String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Đã hủy");
        updates.put("cancelReason", reason);
        updates.put("cancelledBy", "user");
        updates.put("cancelTimestamp", new Date());

        db.collection("orders").document(order.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    // Firebase Listener sẽ tự động load lại và chuyển trang
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi hủy đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateStatus(String newStatus) {
        orderManager.updateOrderStatus(order.getId(), newStatus, new OrderManager.OnActionCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(OrderDetailActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                order.setStatus(newStatus);
                displayOrderDetails();
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Date calculateExpiryDate(Date startDate, String warranty) {
        if (startDate == null) return null;
        String safeWarranty = (warranty == null || warranty.trim().isEmpty()) ? "12" : warranty;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(startDate);
        try {
            String lower = safeWarranty.toLowerCase();
            String numericPart = lower.replaceAll("[^0-9]", "");
            if (numericPart.isEmpty()) cal.add(java.util.Calendar.MONTH, 12);
            else {
                int value = Integer.parseInt(numericPart);
                if (lower.contains("năm")) cal.add(java.util.Calendar.YEAR, value);
                else if (lower.contains("ngày")) cal.add(java.util.Calendar.DAY_OF_YEAR, value);
                else cal.add(java.util.Calendar.MONTH, value);
            }
            return cal.getTime();
        } catch (Exception e) { return null; }
    }
}
