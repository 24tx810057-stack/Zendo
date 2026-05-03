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
            if (fullId != null) {
                String idToDisplay = fullId.length() > 8 ? fullId.substring(fullId.length() - 8).toUpperCase() : fullId.toUpperCase();
                tvOrderIdDisplay.setText("#" + idToDisplay);
            }
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

        // KIỂM TRA XEM ĐÃ CÓ YÊU CẦU BẢO HÀNH CHƯA RỒI MỚI SETUP NÚT
        db.collection("warranty_requests")
                .whereEqualTo("orderId", order.getId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    boolean hasWarrantyRequest = !snapshots.isEmpty();
                    checkAndSetupFooter(hasWarrantyRequest);
                })
                .addOnFailureListener(e -> checkAndSetupFooter(false));
    }

    private void checkAndSetupFooter(boolean hasWarrantyRequest) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            setupFooterActions(hasWarrantyRequest);
            return;
        }

        // Kiểm tra xem tất cả các item đã có warranty snapshot chưa
        boolean itemsChecksPassed = true;
        for (CartItem item : order.getItems()) {
            if (item.getWarranty() == null || item.getWarranty().isEmpty()) {
                itemsChecksPassed = false;
                break;
            }
        }

        if (itemsChecksPassed) {
            setupFooterActions(hasWarrantyRequest);
        } else {
            // Nếu thiếu, đi lấy từ database (cho các đơn test cũ)
            final int total = order.getItems().size();
            java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
            
            for (CartItem item : order.getItems()) {
                if (item.getWarranty() == null || item.getWarranty().isEmpty()) {
                    db.collection("products").document(item.getProductId()).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    item.setWarranty(doc.getString("warranty"));
                                }
                                if (count.incrementAndGet() == total) setupFooterActions(hasWarrantyRequest);
                            })
                            .addOnFailureListener(e -> {
                                if (count.incrementAndGet() == total) setupFooterActions(hasWarrantyRequest);
                            });
                } else {
                    if (count.incrementAndGet() == total) setupFooterActions(hasWarrantyRequest);
                }
            }
        }
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

            // KHỐI HIỂN THỊ BẢO HÀNH (CÓ FALLBACK CHO DỮ LIỆU CŨ)
            LinearLayout itemContainer = (LinearLayout)view.findViewById(R.id.tvName).getParent();
            String itemWarranty = item.getWarranty();
            
            if (itemWarranty != null && !itemWarranty.isEmpty()) {
                displayItemWarranty(itemContainer, item, itemWarranty);
            } else {
                // Nếu đơn cũ chưa có warranty snapshot, thử lấy từ Product gốc
                db.collection("products").document(item.getProductId()).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String currentWarranty = doc.getString("warranty");
                                if (currentWarranty != null && !currentWarranty.isEmpty()) {
                                    displayItemWarranty(itemContainer, item, currentWarranty);
                                }
                            }
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
                long currentTime = System.currentTimeMillis();
                long deliveryTime = order.getDeliveryDate() != null ? order.getDeliveryDate().getTime() : 0;
                long fifteenDaysInMillis = 15L * 24 * 60 * 60 * 1000;

                // Lấy hạn bảo hành lớn nhất trong các sản phẩm
                long maxExpiryTime = 0;
                if (order.getItems() != null) {
                    for (CartItem item : order.getItems()) {
                        Date exp = calculateExpiryDate(order.getDeliveryDate(), item.getWarranty());
                        if (exp != null && exp.getTime() > maxExpiryTime) maxExpiryTime = exp.getTime();
                    }
                }

                if (currentTime - deliveryTime <= fifteenDaysInMillis) {
                    // TRƯỜNG HỢP 1: TRONG 15 NGÀY ĐẦU
                    if (order.isReviewed()) {
                        btnCancelAction.setVisibility(View.GONE); // Đã đánh giá -> Cấm trả hàng
                    } else {
                        btnCancelAction.setVisibility(View.VISIBLE);
                        btnCancelAction.setText("TRẢ HÀNG/HOÀN TIỀN");
                        btnCancelAction.setEnabled(true);
                        btnCancelAction.setAlpha(1.0f);
                        btnCancelAction.setOnClickListener(v -> {
                            Intent intent = new Intent(this, RequestReturnActivity.class);
                            intent.putExtra("order_data", order);
                            startActivity(intent);
                        });
                    }
                } else {
                    // TRƯỜNG HỢP 2: QUÁ 15 NGÀY -> CHUYỂN SANG CHẾ ĐỘ BẢO HÀNH
                    if (maxExpiryTime > currentTime) {
                        if (hasWarrantyRequest) {
                            btnCancelAction.setVisibility(View.VISIBLE);
                            btnCancelAction.setText("ĐÃ GỬI YÊU CẦU BH");
                            btnCancelAction.setEnabled(false);
                            btnCancelAction.setAlpha(0.6f);
                        } else {
                            btnCancelAction.setVisibility(View.VISIBLE);
                            btnCancelAction.setText("YÊU CẦU BẢO HÀNH");
                            btnCancelAction.setEnabled(true);
                            btnCancelAction.setAlpha(1.0f);
                            btnCancelAction.setOnClickListener(v -> {
                                if (order.getItems() != null && !order.getItems().isEmpty()) {
                                    CartItem firstItem = order.getItems().get(0);
                                    Date expiryDate = calculateExpiryDate(order.getDeliveryDate(), firstItem.getWarranty());
                                    
                                    Intent intent = new Intent(this, RequestWarrantyActivity.class);
                                    intent.putExtra("order_data", order);
                                    intent.putExtra("cart_item", firstItem);
                                    intent.putExtra("expiry_date", expiryDate);
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

    private void displayItemWarranty(LinearLayout container, CartItem item, String warranty) {
        String warrantyText = warranty;
        if (warrantyText != null && warrantyText.matches("\\d+")) warrantyText += " tháng";
        final String finalWarrantyDisplay = (warrantyText == null) ? "" : warrantyText;

        TextView tvWarrantyItem = new TextView(this);
        tvWarrantyItem.setText("Bảo hành: " + finalWarrantyDisplay);
        tvWarrantyItem.setTextSize(11);
        tvWarrantyItem.setTextColor(0xFF757575);
        
        // KIỂM TRA TRẠNG THÁI YÊU CẦU BẢO HÀNH TRƯỚC
        db.collection("warranty_requests")
                .whereEqualTo("orderId", order.getId())
                .whereEqualTo("productId", item.getProductId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    String statusBadge = "";
                    if (!snapshots.isEmpty()) {
                        String status = snapshots.getDocuments().get(0).getString("status");
                        if ("pending_repair".equals(status)) {
                            statusBadge = " [Chờ tiếp nhận]";
                        } else if ("repairing".equals(status)) {
                            statusBadge = " [Đang sửa chữa]";
                        } else if ("repaired".equals(status)) {
                            statusBadge = " [Đã sửa xong]";
                        } else if ("rejected".equals(status)) {
                            statusBadge = " [Từ chối bảo hành]";
                        }
                    }

                    // Tính ngày hết hạn nếu đơn đã hoàn thành
                    if ("Hoàn thành".equals(order.getStatus()) && order.getDeliveryDate() != null) {
                        Date expiryDate = calculateExpiryDate(order.getDeliveryDate(), warranty);
                        if (expiryDate != null) {
                            String expiryStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expiryDate);
                            tvWarrantyItem.setText("Bảo hành: " + finalWarrantyDisplay + " (Hết hạn: " + expiryStr + ")");
                            
                            if (!statusBadge.isEmpty()) {
                                tvWarrantyItem.append(statusBadge);
                            }

                            if (expiryDate.before(new Date())) tvWarrantyItem.setTextColor(0xFFF44336);
                            else tvWarrantyItem.setTextColor(0xFF2E7D32);

                            tvWarrantyItem.setPaintFlags(tvWarrantyItem.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                            tvWarrantyItem.setOnClickListener(v -> showWarrantyCard(item, order.getDeliveryDate(), expiryDate, warranty));
                        }
                    }
                });

        container.addView(tvWarrantyItem);
    }

    private void showWarrantyCard(CartItem item, Date deliveryDate, Date expiryDate, String warranty) {
        // Kiểm tra xem item này đã gửi yêu cầu bảo hành chưa
        db.collection("warranty_requests")
                .whereEqualTo("orderId", order.getId())
                .whereEqualTo("productId", item.getProductId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    boolean itemHasRequest = !snapshots.isEmpty();
                    showWarrantyCardUI(item, deliveryDate, expiryDate, itemHasRequest);
                });
    }

    private void showWarrantyCardUI(CartItem item, Date deliveryDate, Date expiryDate, boolean hasRequest) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_warranty_card, null);
        builder.setView(view);

        TextView tvName = view.findViewById(R.id.tvCardProductName);
        TextView tvId = view.findViewById(R.id.tvCardOrderId);
        TextView tvActive = view.findViewById(R.id.tvCardActivationDate);
        TextView tvExpire = view.findViewById(R.id.tvCardExpiryDate);
        TextView tvStatusCard = view.findViewById(R.id.tvCardStatus);
        Button btnClose = view.findViewById(R.id.btnCloseCard);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        tvName.setText(item.getProductName());
        String fullId = order.getId();
        tvId.setText("ID: #" + (fullId.length() > 8 ? fullId.substring(fullId.length() - 8).toUpperCase() : fullId.toUpperCase()));
        tvActive.setText(sdf.format(deliveryDate));
        tvExpire.setText(sdf.format(expiryDate));

        // KIỂM TRA TRẠNG THÁI CỤ THỂ TRONG DATABASE ĐỂ ĐỔI TEXT NÚT
        db.collection("warranty_requests")
                .whereEqualTo("orderId", order.getId())
                .whereEqualTo("productId", item.getProductId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    View btnContact = view.findViewById(R.id.btnContactWarranty);
                    if (btnContact != null) {
                        if (expiryDate.before(new Date())) {
                            btnContact.setVisibility(View.GONE);
                            tvStatusCard.setText("Trạng thái: Hết hạn bảo hành");
                            tvStatusCard.setTextColor(0xFFF44336);
                        } else if (!snapshots.isEmpty()) {
                            String status = snapshots.getDocuments().get(0).getString("status");
                            btnContact.setEnabled(false);
                            btnContact.setAlpha(0.6f);
                            
                            if ("pending_repair".equals(status)) {
                                ((Button)btnContact).setText("ĐANG CHỜ TIẾP NHẬN");
                            } else if ("repairing".equals(status)) {
                                ((Button)btnContact).setText("ĐANG TRONG QUÁ TRÌNH SỬA CHỮA");
                            } else if ("repaired".equals(status)) {
                                ((Button)btnContact).setText("ĐÃ SỬA XONG");
                            }
                        } else {
                            btnContact.setVisibility(View.VISIBLE);
                            btnContact.setOnClickListener(v -> {
                                // dismiss và intent cũ
                            });
                        }
                    }
                });

        if (expiryDate.before(new Date())) {
            tvStatusCard.setText("Trạng thái: Hết hạn bảo hành");
            tvStatusCard.setTextColor(0xFFF44336);
            tvStatusCard.setBackgroundColor(0xFFFFEBEE);
        } else {
            tvStatusCard.setText("Trạng thái: Đang bảo hành");
            tvStatusCard.setTextColor(0xFF2E7D32);
            tvStatusCard.setBackgroundColor(0xFFE8F5E9);
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnContact = view.findViewById(R.id.btnContactWarranty);
        if (btnContact != null && !hasRequest) {
             btnContact.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, RequestWarrantyActivity.class);
                intent.putExtra("order_data", order);
                intent.putExtra("cart_item", item);
                intent.putExtra("expiry_date", expiryDate);
                startActivity(intent);
            });
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
        if (startDate == null) return null;
        
        // NẾU KHÔNG CÓ BẢO HÀNH -> MẶC ĐỊNH 12 THÁNG CHO DỮ LIỆU TEST
        String safeWarranty = (warranty == null || warranty.trim().isEmpty()) ? "12" : warranty;
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(startDate);
        try {
            String lower = safeWarranty.toLowerCase();
            // Lấy phần số (ví dụ: "12 tháng" -> "12")
            String numericPart = lower.replaceAll("[^0-9]", "");
            if (numericPart.isEmpty()) {
                cal.add(java.util.Calendar.MONTH, 12); // Không có số thì mặc định cộng 12 tháng
            } else {
                int value = Integer.parseInt(numericPart);
                if (lower.contains("năm")) cal.add(java.util.Calendar.YEAR, value);
                else if (lower.contains("ngày")) cal.add(java.util.Calendar.DAY_OF_YEAR, value);
                else cal.add(java.util.Calendar.MONTH, value); // Mặc định là tháng
            }
            return cal.getTime();
        } catch (Exception e) { return null; }
    }
}
