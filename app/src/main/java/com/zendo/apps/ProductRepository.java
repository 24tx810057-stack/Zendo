package com.zendo.apps;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRepository {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ProductDao productDao;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public ProductRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        productDao = database.productDao();
    }

    public LiveData<List<Product>> getProducts(String brand, String sortOrder) {
        MutableLiveData<List<Product>> productsLiveData = new MutableLiveData<>();
        
        // Load from local first (simplified)
        executor.execute(() -> {
            List<ProductEntity> entities = productDao.getAllProducts();
            if (!entities.isEmpty()) {
                List<Product> localList = new ArrayList<>();
                for (ProductEntity e : entities) localList.add(mapToDomain(e));
                productsLiveData.postValue(localList);
            }
        });

        Query query = db.collection("products");
        if (brand != null && !brand.equals("Tất cả")) {
            query = query.whereEqualTo("brand", brand);
        }

        switch (sortOrder) {
            case "best_selling": query = query.orderBy("soldCount", Query.Direction.DESCENDING); break;
            case "new_arrival": query = query.orderBy("createdAt", Query.Direction.DESCENDING); break;
            case "price_asc": query = query.orderBy("price", Query.Direction.ASCENDING); break;
            case "price_desc": query = query.orderBy("price", Query.Direction.DESCENDING); break;
            default: query = query.orderBy("createdAt", Query.Direction.DESCENDING); break;
        }

        query.addSnapshotListener((value, error) -> {
            if (error != null) {
                // If offline and no local data, this might stay null/empty
                return;
            }

            List<Product> productList = new ArrayList<>();
            List<ProductEntity> entities = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Product product = doc.toObject(Product.class);
                    product.setId(doc.getId());
                    productList.add(product);
                    entities.add(mapToEntity(product));
                }
                productsLiveData.setValue(productList);
                
                // Cache to local
                executor.execute(() -> {
                    productDao.deleteAll();
                    productDao.insertAll(entities);
                });
            }
        });

        return productsLiveData;
    }

    private Product mapToDomain(ProductEntity e) {
        Product p = new Product();
        p.setId(e.getId());
        p.setName(e.getName());
        p.setDescription(e.getDescription());
        p.setPrice(e.getPrice());
        p.setImageUrl(e.getImageUrl());
        p.setCategory(e.getCategory());
        p.setStock(e.getStock());
        p.setBrand(e.getBrand());
        p.setRating(e.getRating());
        p.setSoldCount(e.getSoldCount());
        p.setCreatedAt(e.getCreatedAt());
        return p;
    }

    private ProductEntity mapToEntity(Product p) {
        ProductEntity e = new ProductEntity();
        e.setId(p.getId());
        e.setName(p.getName());
        e.setDescription(p.getDescription());
        e.setPrice(p.getPrice());
        e.setImageUrl(p.getImageUrl());
        e.setCategory(p.getCategory());
        e.setStock(p.getStock());
        e.setBrand(p.getBrand());
        e.setRating(p.getRating());
        e.setSoldCount(p.getSoldCount());
        e.setCreatedAt(p.getCreatedAt());
        return e;
    }
}
