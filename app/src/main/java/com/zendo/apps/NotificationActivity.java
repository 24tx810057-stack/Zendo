package com.zendo.apps;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.zendo.apps.databinding.ActivityNotificationBinding;
import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private ActivityNotificationBinding binding;
    private NotificationAdapter adapter;
    private final List<Notification> notificationList = new ArrayList<>();
    private FirebaseFirestore db;
    private String userEmail, userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");
        userRole = sharedPref.getString("user_role", "user");

        binding.btnBackNotif.setOnClickListener(v -> finish());
        binding.btnReadAll.setOnClickListener(v -> markAllAsRead());

        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        binding.rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        String targetEmail = userRole.equals("admin") ? "admin" : userEmail;
        
        db.collection("notifications")
                .whereEqualTo("userEmail", targetEmail)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("NotificationActivity", "Error sorting: " + error.getMessage());
                        // Fallback if sorting fails due to inconsistent types in Firestore
                        db.collection("notifications")
                                .whereEqualTo("userEmail", targetEmail)
                                .addSnapshotListener((v2, e2) -> {
                                   if (v2 != null) {
                                       notificationList.clear();
                                       for (QueryDocumentSnapshot doc : v2) {
                                           Notification n = doc.toObject(Notification.class);
                                           n.setId(doc.getId());
                                           notificationList.add(n);
                                       }
                                       // Manual sort as fallback
                                       java.util.Collections.sort(notificationList, (n1, n2) -> 
                                           Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                                       updateUI();
                                   }
                                });
                        return;
                    }
                    if (value != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Notification notification = doc.toObject(Notification.class);
                            notification.setId(doc.getId());
                            notificationList.add(notification);
                        }
                        updateUI();
                    }
                });
    }

    private void markAllAsRead() {
        if (notificationList.isEmpty()) return;

        WriteBatch batch = db.batch();
        boolean hasUnread = false;
        for (Notification n : notificationList) {
            if (!n.isRead()) {
                batch.update(db.collection("notifications").document(n.getId()), "read", true);
                n.setRead(true); // Cập nhật local để UI thay đổi ngay lập tức
                hasUnread = true;
            }
        }

        if (hasUnread) {
            adapter.notifyDataSetChanged(); // Cập nhật UI ngay lập tức
            batch.commit().addOnSuccessListener(aVoid -> 
                Toast.makeText(this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateUI() {
        if (notificationList.isEmpty()) {
            binding.rvNotifications.setVisibility(View.GONE);
            binding.emptyViewNotif.setVisibility(View.VISIBLE);
            binding.btnReadAll.setVisibility(View.GONE);
        } else {
            binding.rvNotifications.setVisibility(View.VISIBLE);
            binding.emptyViewNotif.setVisibility(View.GONE);
            binding.btnReadAll.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }
}
