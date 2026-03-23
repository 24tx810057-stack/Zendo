package com.example.buoi1;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class OrderManager {
    private FirebaseFirestore db;

    public OrderManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void updateOrderStatus(String orderId, String newStatus, OnActionCompleteListener listener) {
        db.collection("orders").document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    // Nếu đơn hàng chuyển sang trạng thái Đã giao, có thể log thêm logic ở đây nếu cần
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
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
                    // Sau khi đánh giá thành công, cập nhật lại rating cho sản phẩm
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
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Review r = doc.toObject(Review.class);
                        if (r != null) {
                            // Review class có getQualityRating hoặc getRating
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
