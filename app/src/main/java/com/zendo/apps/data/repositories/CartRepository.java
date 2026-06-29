package com.zendo.apps.data.repositories;

import com.zendo.apps.data.models.CartItem;
import com.zendo.apps.data.models.AuthResultState;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class CartRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<AuthResultState<Void>> addToCart(CartItem item) {
        MutableLiveData<AuthResultState<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResultState.loading());

        db.collection("cart")
                .whereEqualTo("userEmail", item.getUserEmail())
                .whereEqualTo("productId", item.getProductId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Item already exists, update quantity
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Long existingQty = doc.getLong("quantity");
                        int currentQty = (existingQty != null) ? existingQty.intValue() : 0;
                        doc.getReference().update("quantity", currentQty + item.getQuantity())
                                .addOnSuccessListener(aVoid -> result.setValue(AuthResultState.success(null)))
                                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
                    } else {
                        // New item, add to cart
                        item.setTimestamp(System.currentTimeMillis());
                        db.collection("cart").add(item)
                                .addOnSuccessListener(documentReference -> result.setValue(AuthResultState.success(null)))
                                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
                    }
                })
                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));

        return result;
    }

    public LiveData<List<CartItem>> getCartItems(String email) {
        MutableLiveData<List<CartItem>> cartItems = new MutableLiveData<>();
        db.collection("cart")
                .whereEqualTo("userEmail", email)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("CartRepository", "Error fetching cart items: " + error.getMessage());
                        cartItems.setValue(null);
                        return;
                    }
                    List<CartItem> items = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            CartItem item = doc.toObject(CartItem.class);
                            item.setId(doc.getId());
                            items.add(item);
                        }
                    }
                    // Sort locally if needed, or just return as is for the badge
                    items.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    cartItems.setValue(items);
                });
        return cartItems;
    }

    public void updateQuantity(String itemId, int quantity) {
        db.collection("cart").document(itemId).update("quantity", quantity);
    }

    public LiveData<AuthResultState<Void>> deleteItems(List<String> itemIds) {
        MutableLiveData<AuthResultState<Void>> result = new MutableLiveData<>();
        WriteBatch batch = db.batch();
        for (String id : itemIds) {
            batch.delete(db.collection("cart").document(id));
        }
        batch.commit()
                .addOnSuccessListener(aVoid -> result.setValue(AuthResultState.success(null)))
                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
        return result;
    }
}
