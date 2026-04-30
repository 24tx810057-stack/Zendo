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
                .whereEqualTo("status", "Đã giao")
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

            db.collection("orders").document(orderId)
                    .update("status", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        // Tăng lượt bán nếu đơn chuyển sang Đã giao
                        if ("Đã giao".equals(newStatus)) {
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
