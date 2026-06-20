package com.zendo.apps;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<User> getUser(String email) {
        MutableLiveData<User> userLiveData = new MutableLiveData<>();
        db.collection("users").document(email).addSnapshotListener((value, error) -> {
            if (error != null) {
                userLiveData.setValue(null);
                return;
            }
            if (value != null && value.exists()) {
                userLiveData.setValue(value.toObject(User.class));
            }
        });
        return userLiveData;
    }

    public void updateUser(User user, OnCompleteListener listener) {
        db.collection("users").document(user.getEmail()).set(user)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }
}
