package com.zendo.apps.data.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zendo.apps.data.models.AuthResultState;
import com.zendo.apps.data.models.User;

public class UserRepository {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

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

    public LiveData<AuthResultState<User>> login(String email, String password) {
        MutableLiveData<AuthResultState<User>> loginState = new MutableLiveData<>();
        loginState.setValue(AuthResultState.loading());

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    db.collection("users").document(email).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    User user = documentSnapshot.toObject(User.class);
                                    loginState.setValue(AuthResultState.success(user));
                                } else {
                                    loginState.setValue(AuthResultState.error("Không tìm thấy thông tin người dùng."));
                                }
                            })
                            .addOnFailureListener(e -> loginState.setValue(AuthResultState.error(e.getMessage())));
                })
                .addOnFailureListener(e -> loginState.setValue(AuthResultState.error("Sai email hoặc mật khẩu.")));

        return loginState;
    }

    public LiveData<AuthResultState<User>> register(User user, String password) {
        MutableLiveData<AuthResultState<User>> registerState = new MutableLiveData<>();
        registerState.setValue(AuthResultState.loading());

        mAuth.createUserWithEmailAndPassword(user.getEmail(), password)
                .addOnSuccessListener(authResult -> {
                    user.setId(authResult.getUser().getUid());
                    db.collection("users").document(user.getEmail()).set(user)
                            .addOnSuccessListener(aVoid -> registerState.setValue(AuthResultState.success(user)))
                            .addOnFailureListener(e -> registerState.setValue(AuthResultState.error(e.getMessage())));
                })
                .addOnFailureListener(e -> registerState.setValue(AuthResultState.error(e.getMessage())));

        return registerState;
    }

    public LiveData<AuthResultState<String>> findEmailByPhone(String phone) {
        MutableLiveData<AuthResultState<String>> result = new MutableLiveData<>();
        result.setValue(AuthResultState.loading());

        // Step 1: Check 'phone' field
        db.collection("users").whereEqualTo("phone", phone).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String email = querySnapshot.getDocuments().get(0).getString("email");
                        if (email == null) email = querySnapshot.getDocuments().get(0).getString("e-mail");
                        result.setValue(AuthResultState.success(email));
                    } else {
                        // Step 2: Check if phone is stored in 'email' field
                        db.collection("users").whereEqualTo("email", phone).get()
                                .addOnSuccessListener(qs2 -> {
                                    if (!qs2.isEmpty()) {
                                        String email = qs2.getDocuments().get(0).getString("email");
                                        if (email == null) email = qs2.getDocuments().get(0).getString("e-mail");
                                        result.setValue(AuthResultState.success(email));
                                    } else {
                                        // Step 3: Check 'e-mail' field
                                        db.collection("users").whereEqualTo("e-mail", phone).get()
                                                .addOnSuccessListener(qs3 -> {
                                                    if (!qs3.isEmpty()) {
                                                        String email = qs3.getDocuments().get(0).getString("email");
                                                        if (email == null) email = qs3.getDocuments().get(0).getString("e-mail");
                                                        result.setValue(AuthResultState.success(email));
                                                    } else {
                                                        result.setValue(AuthResultState.error("Số điện thoại chưa được đăng ký."));
                                                    }
                                                })
                                                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
                                    }
                                })
                                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
                    }
                })
                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));

        return result;
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
