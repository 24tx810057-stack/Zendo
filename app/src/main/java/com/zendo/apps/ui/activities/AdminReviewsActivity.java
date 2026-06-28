package com.zendo.apps.ui.activities;

import com.zendo.apps.ui.adapters.AdminReviewAdapter;

import com.zendo.apps.data.models.Review;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zendo.apps.databinding.ActivityAdminReviewsBinding;
import java.util.ArrayList;
import java.util.List;

public class AdminReviewsActivity extends AppCompatActivity {

    private ActivityAdminReviewsBinding binding;
    private AdminReviewAdapter adapter;
    private List<Review> reviewList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        binding.btnBackReviews.setOnClickListener(v -> finish());

        binding.rvAdminReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminReviewAdapter(this, reviewList);
        binding.rvAdminReviews.setAdapter(adapter);

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
                        
                        binding.tvEmptyReviews.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }
}


