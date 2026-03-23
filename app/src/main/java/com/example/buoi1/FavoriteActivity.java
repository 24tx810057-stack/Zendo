package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity {

    private GridView gridView;
    private ProductAdapter adapter;
    private List<Product> favoriteList = new ArrayList<>();
    private FirebaseFirestore db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");

        gridView = findViewById(R.id.gridViewFavorite);
        ImageButton btnBack = findViewById(R.id.btnBackFav);

        adapter = new ProductAdapter(this, favoriteList);
        gridView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Product selectedProduct = favoriteList.get(position);
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("product_data", selectedProduct);
            startActivity(intent);
        });

        loadFavoriteProducts();
    }

    private void loadFavoriteProducts() {
        if (userEmail.isEmpty()) return;

        db.collection("products")
                .whereArrayContains("likedBy", userEmail)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    favoriteList.clear();
                    for (QueryDocumentSnapshot document : value) {
                        Product prod = document.toObject(Product.class);
                        prod.setId(document.getId());
                        favoriteList.add(prod);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
