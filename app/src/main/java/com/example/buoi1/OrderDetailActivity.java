package com.example.buoi1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView tvStatus, tvOrderDate, tvDeliveryDate, tvNamePhone, tvAddress, tvNote, tvOrderIdDisplay, tvReturnStatusDesc;
    private TextView tvSubtotal, tvShipping, tvVoucher, tvTotal, tvPaymentMethod;
    private View layoutVoucherContainer, layoutReturnInfo;
    private LinearLayout layoutProducts;
    private Button btnMainAction, btnCancelAction;
    
    private Order order;
    private OrderManager orderManager;
    private String userEmail, userRole;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private FirebaseFirestore db;
    private com.google.firebase.firestore.ListenerRegistration orderListener;

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
        observeOrderChanges(); // Theo dõi đơn hàng thời gian thực

        // Nếu đi từ nút Đánh giá ở danh sách đơn hàng
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
        super.onDestroy();
        if (orderListener != null) orderListener.remove();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvOrderStatus);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvDeliveryDate = findViewById(R.id.tvDeliveryDate);
        tvOrderIdDisplay = findViewById(R.id.tvOrderIdDetail);
        tvReturnStatusDesc = findViewById(R.id.tvReturnStatusDesc);
        layoutReturnInfo = findViewById(R.id.layoutReturnInfo);
        tvNamePhone = findViewById(R.id.tvReceiverNamePhone);
        tvAddress = findViewById(R.id.tvReceiverAddress);
        tvNote = findViewById(R.id.tvOrderNote);
        tvSubtotal = findViewById(R.id.tvOrderSubtotal);
        tvShipping = findViewById(R.id.tvOrderShipping);
        tvVoucher = findViewById(R.id.tvOrderVoucher);
        layoutVoucherContainer = findViewById(R.id.layoutOrderVoucher);
        tvTotal = findViewById(R.id.tvOrderTotal);
        tvPaymentMethod = findViewById(R.id.tvOrderPaymentMethod);
        layoutProducts = findViewById(R.id.layoutOrderProducts);
        btnMainAction = findViewById(R.id.btnMainAction);
        btnCancelAction = findViewById(R.id.btnCancelOrder);
        
        findViewById(R.id.btnBackOrder).setOnClickListener(v -> finish());
    }

    private void startAddReviewActivity() {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) return;
        Intent intent = new Intent(this, AddReviewActivity.class);
        intent.putExtra("product_id", order.getItems().get(0).getProductId());
        intent.putExtra("order_id", order.getId());
        intent.putExtra("cart_item", order.getItems().get(0));
        startActivity(intent);
    }

    private void displayOrderDetails() {
        if (order == null) return;

        tvStatus.setText(order.getStatus());
        if (tvOrderIdDisplay != null) {
            String fullId = order.getId();
            String shortId = fullId;
            if (fullId != null && fullId.length() > 8) {
                shortId = fullId.substring(fullId.length() - 8).toUpperCase();
            }
            tvOrderIdDisplay.setText("#" + shortId);
        }
        tvNamePhone.setText(order.getUserName() + " | " + order.getPhone());
        tvAddress.setText(order.getAddress());
        
        if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
            tvNote.setText(order.getNote());
            tvNote.setTextColor(0xFF212121);
        } else {
            tvNote.setText("Không có ghi chú từ khách hàng");
            tvNote.setTextColor(0xFF9E9E9E);
        }
        
        if (order.getTimestamp() != null) {
            tvOrderDate.setText("Ngày đặt hàng: " + dateFormat.format(order.getTimestamp()));
        }

        if (order.getDeliveryDate() != null) {
            tvDeliveryDate.setVisibility(View.VISIBLE);
            tvDeliveryDate.setText("Thời gian nhận hàng: " + dateFormat.format(order.getDeliveryDate()));
        } else {
            tvDeliveryDate.setVisibility(View.GONE);
        }

        tvSubtotal.setText(formatter.format(order.getSubtotal()) + "đ");
        tvShipping.setText(formatter.format(order.getShippingFee()) + "đ");
        
        if (order.getVoucherDiscount() > 0) {
            layoutVoucherContainer.setVisibility(View.VISIBLE);
            tvVoucher.setText("-" + formatter.format(order.getVoucherDiscount()) + "đ");
        } else {
            layoutVoucherContainer.setVisibility(View.GONE);
        }

        tvTotal.setText(formatter.format(order.getTotalAmount()) + "đ");
        tvPaymentMethod.setText("Phương thức thanh toán: " + order.getPaymentMethod());

        displayProducts(order.getItems());
        setupReturnBanner(); // Hiển thị banner khiếu nại nếu có
        setupFooterActions();
    }

    private void setupReturnBanner() {
        if (layoutReturnInfo == null) return;

        String status = order.getStatus();
        if ("Yêu cầu trả hàng".equals(status)) {
            layoutReturnInfo.setVisibility(View.VISIBLE);
            layoutReturnInfo.setBackgroundColor(0xFFFFF8E1); // vàng nhạt
            tvReturnStatusDesc.setText("Yêu cầu của bạn đang được hệ thống kiểm tra và duyệt trong vòng 24h.");
            tvReturnStatusDesc.setTextColor(0xFFF57F17);
        } else if ("Đã hoàn tiền".equals(status)) {
            layoutReturnInfo.setVisibility(View.VISIBLE);
            layoutReturnInfo.setBackgroundColor(0xFFE8F5E9); // xanh nhạt
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

            // HIỂN THỊ THÔNG TIN BẢO HÀNH CHO TỪNG SẢN PHẨM
            if (item.getWarranty() != null && !item.getWarranty().isEmpty()) {
                String warrantyDisplay = item.getWarranty();
                if (warrantyDisplay.matches("\\d+")) warrantyDisplay += " tháng";

                TextView tvWarrantyItem = new TextView(this);
                tvWarrantyItem.setText("Bảo hành: " + warrantyDisplay);
                tvWarrantyItem.setTextSize(11);
                tvWarrantyItem.setTextColor(0xFF757575);
                
                // Tính ngày hết hạn nếu đơn đã hoàn thành
                if ("Hoàn thành".equals(order.getStatus()) && order.getDeliveryDate() != null) {
                    Date expiryDate = calculateExpiryDate(order.getDeliveryDate(), item.getWarranty());
                    if (expiryDate != null) {
                        String expiryStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expiryDate);
                        tvWarrantyItem.append(" (Hết hạn: " + expiryStr + ")");
                        
                        // Đổi màu nếu sắp hết hạn hoặc đã hết hạn
                        if (expiryDate.before(new Date())) {
                            tvWarrantyItem.setTextColor(0xFFF44336); // Đỏ
                        } else {
                            tvWarrantyItem.setTextColor(0xFF2E7D32); // Xanh
                        }

                        // Cho phép bấm vào để xem thẻ bảo hành
                        tvWarrantyItem.setPaintFlags(tvWarrantyItem.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                        tvWarrantyItem.setOnClickListener(v -> showWarrantyCard(item, order.getDeliveryDate(), expiryDate));
                    }
                }
                ((LinearLayout)view.findViewById(R.id.tvName).getParent()).addView(tvWarrantyItem);
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

    private void setupFooterActions() {
        String status = order.getStatus();
        if (status == null) status = "";
        
        // Reset trạng thái nút mặc định
        btnMainAction.setVisibility(View.VISIBLE);
        btnMainAction.setEnabled(true);
        btnMainAction.setAlpha(1.0f);
        btnCancelAction.setVisibility(View.GONE);
        btnCancelAction.setEnabled(true);
        btnCancelAction.setAlpha(1.0f);

        if ("admin".equals(userRole)) {
            btnMainAction.setVisibility(View.VISIBLE);
            if (status.equals("Yêu cầu hủy")) {
                btnMainAction.setText("Đồng ý hủy đơn");
                btnMainAction.setOnClickListener(v -> updateStatus("Đã hủy"));
                btnCancelAction.setVisibility(View.VISIBLE);
                btnCancelAction.setText("Từ chối hủy");
                btnCancelAction.setOnClickListener(v -> updateStatus("Chờ xác nhận"));
            } else if (status.equals("Chờ xác nhận")) {
                btnMainAction.setText("Xác nhận đơn hàng");
                btnMainAction.setOnClickListener(v -> updateStatus("Chờ lấy hàng"));
                btnCancelAction.setVisibility(View.VISIBLE);
                btnCancelAction.setText("TỪ CHỐI ĐƠN");
                btnCancelAction.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Xác nhận từ chối")
                            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không? Dữ liệu vẫn sẽ được lưu lại trong mục 'Đã hủy'.")
                            .setPositiveButton("Hủy đơn", (d, w) -> updateStatus("Đã hủy"))
                            .setNegativeButton("Quay lại", null)
                            .show();
                });
            } else if (status.equals("Chờ lấy hàng")) {
                btnMainAction.setText("Giao cho ĐVVC");
                btnMainAction.setOnClickListener(v -> updateStatus("Đang giao"));
            } else if (status.equals("Đang giao")) {
                btnMainAction.setText("Xác nhận đã giao");
                btnMainAction.setOnClickListener(v -> updateStatus("Đã giao"));
            } else if (status.equals("Đã giao")) {
                btnMainAction.setVisibility(View.GONE); // Đợi khách hàng xác nhận
            } else {
                // Đã hoàn thành hoặc Đã hủy: Admin chỉ được xem, không được xóa
                btnMainAction.setVisibility(View.GONE);
                btnCancelAction.setVisibility(View.GONE);
            }
        } else {
            btnMainAction.setVisibility(View.VISIBLE);
            if (status.equals("Chờ xác nhận") || status.equals("Chờ lấy hàng")) {
                btnMainAction.setText("Gửi yêu cầu hủy");
                btnMainAction.setOnClickListener(v -> updateStatus("Yêu cầu hủy"));
            } else if (status.equals("Đang giao") || status.equals("Đã giao")) {
                btnMainAction.setVisibility(View.VISIBLE);
                btnMainAction.setText("Đã nhận được hàng");
                btnMainAction.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Xác nhận đã nhận hàng")
                            .setMessage("Bạn chỉ nên xác nhận khi đã thực sự nhận được hàng và sản phẩm không có vấn đề gì.")
                            .setPositiveButton("Xác nhận", (d, w) -> updateStatus("Hoàn thành"))
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            } else if (status.equals("Yêu cầu hủy")) {
                btnMainAction.setText("Đang chờ duyệt hủy...");
                btnMainAction.setEnabled(false);
                btnMainAction.setAlpha(0.6f);
            } else if (status.equals("Hoàn thành")) {
                // Kiểm tra xem đã đánh giá chưa.
                if (order.isReviewed()) {
                    btnCancelAction.setVisibility(View.GONE);
                } else {
                    btnCancelAction.setVisibility(View.VISIBLE);

                    long currentTime = System.currentTimeMillis();
                    long deliveryTime = order.getDeliveryDate() != null ? order.getDeliveryDate().getTime() : 0;
                    long fifteenDaysInMillis = 15L * 24 * 60 * 60 * 1000;

                    // Lấy hạn bảo hành lớn nhất trong các sản phẩm của đơn hàng để quyết định nút
                    long maxExpiryTime = 0;
                    if (order.getItems() != null) {
                        for (CartItem item : order.getItems()) {
                            Date exp = calculateExpiryDate(order.getDeliveryDate(), item.getWarranty());
                            if (exp != null && exp.getTime() > maxExpiryTime) maxExpiryTime = exp.getTime();
                        }
                    }

                    if (currentTime - deliveryTime <= fifteenDaysInMillis) {
                        // TRƯỜNG HỢP 1: CÒN HẠN TRẢ HÀNG (15 NGÀY ĐẦU)
                        btnCancelAction.setText("TRẢ HÀNG/HOÀN TIỀN");
                        btnCancelAction.setEnabled(true);
                        btnCancelAction.setAlpha(1.0f);
                        btnCancelAction.setOnClickListener(v -> {
                            Intent intent = new Intent(this, RequestReturnActivity.class);
                            intent.putExtra("order_data", order);
                            startActivity(intent);
                        });
                    } else if (maxExpiryTime > currentTime) {
                        // TRƯỜNG HỢP 2: QUÁ 15 NGÀY NHƯNG CÒN BẢO HÀNH
                        btnCancelAction.setText("YÊU CẦU BẢO HÀNH");
                        btnCancelAction.setEnabled(true);
                        btnCancelAction.setAlpha(1.0f);
                        btnCancelAction.setOnClickListener(v -> {
                            Toast.makeText(this, "Đang mở form yêu cầu sửa chữa/bảo hành...", Toast.LENGTH_SHORT).show();
                            // Sau này bạn có thể tạo RequestWarrantyActivity tương tự RequestReturnActivity
                        });
                    } else {
                        // TRƯỜNG HỢP 3: HẾT SẠCH HẠN
                        btnCancelAction.setText("HẾT HẠN BẢO HÀNH");
                        btnCancelAction.setEnabled(false);
                        btnCancelAction.setAlpha(0.5f);
                    }
                }

                if (order.isReviewed()) {
                    btnMainAction.setText("Tiếp tục mua sắm");
                    btnMainAction.setOnClickListener(v -> finish());
                } else {
                    db.collection("reviews").whereEqualTo("orderId", order.getId()).get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    btnMainAction.setText("Tiếp tục mua sắm");
                                    btnMainAction.setOnClickListener(v -> finish());
                                    // Tiện tay cập nhật luôn flag vào Order để lần sau load cho nhanh
                                    db.collection("orders").document(order.getId()).update("reviewed", true);
                                } else {
                                    // Kiểm tra dữ liệu "cổ đại" (không có orderId)
                                    String firstProductId = (order.getItems() != null && !order.getItems().isEmpty()) 
                                            ? order.getItems().get(0).getProductId() : "";
                                    
                                    db.collection("reviews")
                                            .whereEqualTo("userEmail", userEmail)
                                            .whereEqualTo("productId", firstProductId)
                                            .get()
                                            .addOnSuccessListener(snapshots -> {
                                                boolean alreadyReviewedLegacy = false;
                                                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                                                    if (!doc.contains("orderId") || doc.getString("orderId") == null || doc.getString("orderId").isEmpty()) {
                                                        alreadyReviewedLegacy = true;
                                                        break;
                                                    }
                                                }

                                                if (alreadyReviewedLegacy) {
                                                    btnMainAction.setText("Tiếp tục mua sắm");
                                                    btnMainAction.setOnClickListener(v -> finish());
                                                    db.collection("orders").document(order.getId()).update("reviewed", true);
                                                } else {
                                                    btnMainAction.setText("Đánh giá sản phẩm");
                                                    btnMainAction.setOnClickListener(v -> startAddReviewActivity());
                                                }
                                            });
                                }
                            });
                }
            } else {
                btnMainAction.setText("Tiếp tục mua sắm");
                btnMainAction.setOnClickListener(v -> finish());
            }
        }
    }

    private void showWarrantyCard(CartItem item, Date deliveryDate, Date expiryDate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_warranty_card, null);
        builder.setView(view);

        TextView tvName = view.findViewById(R.id.tvCardProductName);
        TextView tvId = view.findViewById(R.id.tvCardOrderId);
        TextView tvActive = view.findViewById(R.id.tvCardActivationDate);
        TextView tvExpire = view.findViewById(R.id.tvCardExpiryDate);
        TextView tvStatus = view.findViewById(R.id.tvCardStatus);
        Button btnClose = view.findViewById(R.id.btnCloseCard);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        tvName.setText(item.getProductName());
        String fullId = order.getId();
        tvId.setText("ID: #" + (fullId.length() > 8 ? fullId.substring(fullId.length() - 8).toUpperCase() : fullId.toUpperCase()));
        tvActive.setText(sdf.format(deliveryDate));
        tvExpire.setText(sdf.format(expiryDate));

        if (expiryDate.before(new Date())) {
            tvStatus.setText("Trạng thái: Hết hạn bảo hành");
            tvStatus.setTextColor(0xFFF44336);
            tvStatus.setBackgroundColor(0xFFFFEBEE);
        } else {
            tvStatus.setText("Trạng thái: Đang bảo hành");
            tvStatus.setTextColor(0xFF2E7D32);
            tvStatus.setBackgroundColor(0xFFE8F5E9);
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateStatus(String newStatus) {
        orderManager.updateOrderStatus(order.getId(), newStatus, new OrderManager.OnActionCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(OrderDetailActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                order.setStatus(newStatus);
                // Không set Date ở đây nữa, để OrderManager tự tính toán và SnapshotListener tự cập nhật UI
                displayOrderDetails();
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Date calculateExpiryDate(Date startDate, String warranty) {
        if (startDate == null || warranty == null || warranty.isEmpty()) return null;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(startDate);
        try {
            String lower = warranty.toLowerCase();
            // Lấy phần số (ví dụ: "12 tháng" -> "12")
            String numericPart = lower.replaceAll("[^0-9]", "");
            if (numericPart.isEmpty()) return null;
            int value = Integer.parseInt(numericPart);
            
            if (lower.contains("tháng")) cal.add(java.util.Calendar.MONTH, value);
            else if (lower.contains("năm")) cal.add(java.util.Calendar.YEAR, value);
            else if (lower.contains("ngày")) cal.add(java.util.Calendar.DAY_OF_YEAR, value);
            else cal.add(java.util.Calendar.MONTH, value); // Mặc định là tháng
            
            return cal.getTime();
        } catch (Exception e) { return null; }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Hành động này không thể hoàn tác. Xóa đơn hàng?")
                .setPositiveButton("Xóa", (d, w) -> {
                    orderManager.deleteOrder(order.getId(), new OrderManager.OnActionCompleteListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(OrderDetailActivity.this, "Đã xóa đơn hàng", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(OrderDetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
