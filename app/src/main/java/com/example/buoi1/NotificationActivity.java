package com.example.buoi1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private LinearLayout emptyView;
    private ImageView btnBack;
    private TextView btnReadAll;
    private FirebaseFirestore db;
    private String userEmail, userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");
        userRole = sharedPref.getString("user_role", "user");

        rvNotifications = findViewById(R.id.rvNotifications);
        emptyView = findViewById(R.id.emptyViewNotif);
        btnBack = findViewById(R.id.btnBackNotif);
        btnReadAll = findViewById(R.id.btnReadAll);

        btnBack.setOnClickListener(v -> finish());
        btnReadAll.setOnClickListener(v -> markAllAsRead());

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        String targetEmail = userRole.equals("admin") ? "admin" : userEmail;
        
        db.collection("notifications")
                .whereEqualTo("userEmail", targetEmail)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("NotificationActivity", "Error: " + error.getMessage());
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
            rvNotifications.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            btnReadAll.setVisibility(View.GONE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            btnReadAll.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }
}
