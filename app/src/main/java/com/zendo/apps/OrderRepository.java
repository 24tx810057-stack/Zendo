package com.zendo.apps;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderRepository {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Order>> getOrders(String email, String role) {
        MutableLiveData<List<Order>> ordersLiveData = new MutableLiveData<>();
        
        Query query;
        if ("admin".equals(role)) {
            query = db.collection("orders").orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            query = db.collection("orders")
                    .whereEqualTo("userEmail", email)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        query.addSnapshotListener((value, error) -> {
            if (error != null) {
                ordersLiveData.setValue(null);
                return;
            }

            List<Order> orderList = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Order order = doc.toObject(Order.class);
                    order.setId(doc.getId());
                    orderList.add(order);
                }
            }
            ordersLiveData.setValue(orderList);
        });

        return ordersLiveData;
    }

    public LiveData<List<Order>> getOrdersByStatus(String status, Date start, Date end) {
        MutableLiveData<List<Order>> ordersLiveData = new MutableLiveData<>();
        Query query = db.collection("orders").whereEqualTo("status", status);
        
        // Firestore doesn't support complex date filtering without composite indexes 
        // in combination with status easily for all cases, but we can try simple range if needed.
        // For revenue, we often fetch all and filter client side if the dataset is small, 
        // OR use specific fields.

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Order> orderList = new ArrayList<>();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Order order = doc.toObject(Order.class);
                order.setId(doc.getId());
                
                Date orderDate = order.getTimestamp();
                if (start != null && (orderDate == null || orderDate.before(start))) continue;
                if (end != null && (orderDate == null || orderDate.after(end))) continue;
                
                orderList.add(order);
            }
            ordersLiveData.setValue(orderList);
        }).addOnFailureListener(e -> ordersLiveData.setValue(null));

        return ordersLiveData;
    }

    public void updateOrderStatus(String orderId, String status, OnCompleteListener listener) {
        db.collection("orders").document(orderId).update("status", status)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }
}
