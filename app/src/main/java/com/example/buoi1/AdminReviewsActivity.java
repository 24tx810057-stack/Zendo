package com.example.buoi1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AdminReviewsActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private AdminReviewAdapter adapter;
    private List<Review> reviewList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reviews);

        db = FirebaseFirestore.getInstance();
        rvReviews = findViewById(R.id.rvAdminReviews);
        tvEmpty = findViewById(R.id.tvEmptyReviews);
        ImageView btnBack = findViewById(R.id.btnBackReviews);

        btnBack.setOnClickListener(v -> finish());

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminReviewAdapter(this, reviewList);
        rvReviews.setAdapter(adapter);

        loadUnrepliedReviews();
    }

    private void loadUnrepliedReviews() {
        // Lấy tất cả đánh giá và lọc trong code để tránh lỗi Firestore query null field/index
        db.collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("AdminReviews", "Firestore error: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        reviewList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Review review = doc.toObject(Review.class);
                            review.setId(doc.getId()); // Cực kỳ quan trọng để update được
                            
                            // Lọc lấy những đánh giá chưa có phản hồi
                            if (review.getSellerReply() == null || review.getSellerReply().trim().isEmpty()) {
                                reviewList.add(review);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        if (tvEmpty != null) {
                            tvEmpty.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                });
    }
}
