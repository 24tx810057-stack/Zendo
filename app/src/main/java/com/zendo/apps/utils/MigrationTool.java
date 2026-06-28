package com.zendo.apps.utils;

import com.zendo.apps.data.models.Product;

import com.zendo.apps.data.models.User;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationTool {
    private static final String TAG = "MigrationTool";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final Context context;

    public MigrationTool(Context context) {
        this.context = context;
    }

    public void startMigration() {
        Log.d(TAG, "Starting migration...");
        migrateProducts();
        migrateUsers();
    }

    private void migrateProducts() {
        db.collection("products").get().addOnSuccessListener(queryDocumentSnapshots -> {
            Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " products");
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String imageUrl = doc.getString("imageUrl");
                List<String> images = (List<String>) doc.get("images");

                if (isBase64(imageUrl)) {
                    uploadBase64ToStorage(imageUrl, "products/" + doc.getId() + "/main.jpg", uri -> {
                        db.collection("products").document(doc.getId()).update("imageUrl", uri.toString());
                        Log.d(TAG, "Updated product main image: " + doc.getId());
                    });
                }

                if (images != null) {
                    List<String> newImages = new ArrayList<>();
                    AtomicInteger uploadedCount = new AtomicInteger(0);
                    boolean hasBase64 = false;

                    for (int i = 0; i < images.size(); i++) {
                        String img = images.get(i);
                        if (isBase64(img)) {
                            hasBase64 = true;
                            final int index = i;
                            uploadBase64ToStorage(img, "products/" + doc.getId() + "/extra_" + index + ".jpg", uri -> {
                                synchronized (newImages) {
                                    newImages.add(uri.toString());
                                }
                                if (uploadedCount.incrementAndGet() == countBase64(images)) {
                                    // Merge with existing non-base64 images if any
                                    for (String existing : images) {
                                        if (!isBase64(existing)) newImages.add(existing);
                                    }
                                    db.collection("products").document(doc.getId()).update("images", newImages);
                                    Log.d(TAG, "Updated product images list: " + doc.getId());
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    private void migrateUsers() {
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " users");
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String avatar = doc.getString("avatar");
                if (isBase64(avatar)) {
                    uploadBase64ToStorage(avatar, "users/" + doc.getId() + "/avatar.jpg", uri -> {
                        db.collection("users").document(doc.getId()).update("avatar", uri.toString());
                        Log.d(TAG, "Updated user avatar: " + doc.getId());
                    });
                }
            }
        });
    }

    private boolean isBase64(String str) {
        return str != null && !str.startsWith("http") && str.length() > 100;
    }

    private int countBase64(List<String> list) {
        int count = 0;
        for (String s : list) if (isBase64(s)) count++;
        return count;
    }

    private void uploadBase64ToStorage(String base64, String path, OnUploadSuccessListener listener) {
        try {
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            StorageReference ref = storage.getReference().child(path);
            ref.putBytes(decodedString).addOnSuccessListener(taskSnapshot -> {
                ref.getDownloadUrl().addOnSuccessListener(listener::onSuccess);
            }).addOnFailureListener(e -> Log.e(TAG, "Upload failed for " + path, e));
        } catch (Exception e) {
            Log.e(TAG, "Error decoding base64 for " + path, e);
        }
    }

    interface OnUploadSuccessListener {
        void onSuccess(Uri uri);
    }
}


