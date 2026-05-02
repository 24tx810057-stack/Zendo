package com.example.buoi1;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.HashMap;
import java.util.Map;

public class OrderManager {
    private FirebaseFirestore db;

    public OrderManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void recalculateAllProductSoldCounts(OnActionCompleteListener listener) {
        db.collection("orders")
                .whereEqualTo("status", "Hoàn thành")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> trueSoldCountMap = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        if (order != null && order.getItems() != null) {
                            for (CartItem item : order.getItems()) {
                                String pid = item.getProductId();
                                if (pid != null) {
                                    int current = trueSoldCountMap.getOrDefault(pid, 0);
                                    trueSoldCountMap.put(pid, current + item.getQuantity());
                                }
                            }
                        }
                    }

                    db.collection("products").get().addOnSuccessListener(productDocs -> {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot pDoc : productDocs) {
                            String pId = pDoc.getId();
                            int actualSold = trueSoldCountMap.getOrDefault(pId, 0);
                            batch.update(db.collection("products").document(pId), "soldCount", actualSold);
                        }
                        batch.commit()
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
                    });
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void updateOrderStatus(String orderId, String newStatus, OnActionCompleteListener listener) {
        db.collection("orders").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
            Order order = documentSnapshot.toObject(Order.class);
            if (order == null) {
                listener.onFailure("Không tìm thấy đơn hàng");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", newStatus);

            if ("Đã giao".equals(newStatus)) {
                // Chỉ ghi nhận ngày giao nếu đơn này chưa từng được giao (tránh ghi đè)
                if (order.getDeliveryDate() == null) {
                    updates.put("deliveryDate", new java.util.Date());
                }
            } else if ("Hoàn thành".equals(newStatus)) {
                // LOGIC THÔNG MINH:
                // 1. Kiểm tra xem đây có phải là đơn hàng cũ (> 3 ngày) cần "hợp thức hóa" ngày không
                long threeDaysInMillis = 3L * 24 * 60 * 60 * 1000;
                long currentTime = System.currentTimeMillis();
                long orderTime = order.getTimestamp() != null ? order.getTimestamp().getTime() : currentTime;

                if (currentTime - orderTime > threeDaysInMillis) {
                    // Đơn hàng cũ: Ép ngày nhận = Ngày đặt + 3 ngày (Kể cả khi đã có deliveryDate do Admin bấm nhầm lúc nãy)
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(new java.util.Date(orderTime));
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 3);
                    updates.put("deliveryDate", cal.getTime());
                } else {
                    // Đơn hàng mới (trong vòng 3 ngày): Giữ nguyên ngày Admin bấm, hoặc set = hôm nay nếu chưa có
                    if (order.getDeliveryDate() == null) {
                        updates.put("deliveryDate", new java.util.Date());
                    }
                }
            }

            db.collection("orders").document(orderId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        // CHỈ TĂNG LƯỢT BÁN KHI ĐƠN HÀNG HOÀN THÀNH
                        if ("Hoàn thành".equals(newStatus)) {
                            for (CartItem item : order.getItems()) {
                                if (item.getProductId() != null) {
                                    db.collection("products").document(item.getProductId())
                                            .update("soldCount", FieldValue.increment(item.getQuantity()));
                                }
                            }
                        }
                        // Hoàn lại kho nếu đơn bị Hủy
                        else if ("Đã hủy".equals(newStatus)) {
                            for (CartItem item : order.getItems()) {
                                if (item.getProductId() != null) {
                                    db.collection("products").document(item.getProductId())
                                            .update("stock", FieldValue.increment(item.getQuantity()));
                                }
                            }
                        }
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
        });
    }

    public void deleteOrder(String orderId, OnActionCompleteListener listener) {
        db.collection("orders").document(orderId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void submitReview(Review review, OnActionCompleteListener listener) {
        db.collection("reviews").add(review)
                .addOnSuccessListener(documentReference -> {
                    updateProductStats(review.getProductId());
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    private void updateProductStats(String productId) {
        db.collection("reviews").whereEqualTo("productId", productId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        db.collection("products").document(productId).update("rating", 0);
                        return;
                    }

                    float totalRating = 0;
                    int count = queryDocumentSnapshots.size();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Review r = doc.toObject(Review.class);
                        if (r != null) {
                            totalRating += r.getQualityRating();
                        }
                    }
                    float average = totalRating / count;
                    db.collection("products").document(productId).update("rating", average);
                });
    }

    public interface OnActionCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }
}
