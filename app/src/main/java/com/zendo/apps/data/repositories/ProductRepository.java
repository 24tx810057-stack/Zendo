package com.zendo.apps.data.repositories;

import com.zendo.apps.database.ProductDao;
import com.zendo.apps.database.ProductEntity;

import com.zendo.apps.database.AppDatabase;

import com.zendo.apps.data.models.Product;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.zendo.apps.data.models.AuthResultState;
import com.zendo.apps.data.models.Brand;
import com.zendo.apps.data.models.Category;
import com.zendo.apps.data.models.Review;
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
            case "discount": query = query.orderBy("discountPercent", Query.Direction.DESCENDING); break;
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

    public LiveData<Product> getProductById(String productId) {
        MutableLiveData<Product> productLiveData = new MutableLiveData<>();
        db.collection("products").document(productId).addSnapshotListener((value, error) -> {
            if (error != null || value == null || !value.exists()) {
                return;
            }
            Product product = value.toObject(Product.class);
            if (product != null) {
                product.setId(value.getId());
                productLiveData.setValue(product);
            }
        });
        return productLiveData;
    }

    public LiveData<AuthResultState<Void>> toggleFavorite(String productId, String userEmail, boolean currentlyLiked) {
        MutableLiveData<AuthResultState<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResultState.loading());

        if (currentlyLiked) {
            db.collection("products").document(productId)
                    .update("likedBy", FieldValue.arrayRemove(userEmail))
                    .addOnSuccessListener(aVoid -> result.setValue(AuthResultState.success(null)))
                    .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
        } else {
            db.collection("products").document(productId)
                    .update("likedBy", FieldValue.arrayUnion(userEmail))
                    .addOnSuccessListener(aVoid -> result.setValue(AuthResultState.success(null)))
                    .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
        }
        return result;
    }

    public LiveData<List<Review>> getReviews(String productId) {
        MutableLiveData<List<Review>> reviewsLiveData = new MutableLiveData<>();
        db.collection("reviews")
                .whereEqualTo("productId", productId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    List<Review> reviewList = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            reviewList.add(doc.toObject(Review.class));
                        }
                    }
                    reviewsLiveData.setValue(reviewList);
                });
        return reviewsLiveData;
    }

    public LiveData<List<Category>> getCategories() {
        MutableLiveData<List<Category>> categoriesLiveData = new MutableLiveData<>();
        db.collection("categories").orderBy("name").addSnapshotListener((value, error) -> {
            if (error != null) return;
            List<Category> list = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Category cat = doc.toObject(Category.class);
                    cat.setId(doc.getId());
                    list.add(cat);
                }
            }
            categoriesLiveData.setValue(list);
        });
        return categoriesLiveData;
    }

    public LiveData<AuthResultState<Void>> addCategory(String name) {
        MutableLiveData<AuthResultState<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResultState.loading());
        Category cat = new Category(null, name);
        db.collection("categories").add(cat)
                .addOnSuccessListener(documentReference -> result.setValue(AuthResultState.success(null)))
                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
        return result;
    }

    public LiveData<AuthResultState<Void>> deleteCategory(String id) {
        MutableLiveData<AuthResultState<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResultState.loading());
        db.collection("categories").document(id).delete()
                .addOnSuccessListener(aVoid -> result.setValue(AuthResultState.success(null)))
                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
        return result;
    }

    public LiveData<List<Brand>> getBrands() {
        MutableLiveData<List<Brand>> brandsLiveData = new MutableLiveData<>();
        db.collection("brands").orderBy("name").addSnapshotListener((value, error) -> {
            if (error != null) return;
            List<Brand> list = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Brand brand = doc.toObject(Brand.class);
                    brand.setId(doc.getId());
                    list.add(brand);
                }
            }
            brandsLiveData.setValue(list);
        });
        return brandsLiveData;
    }

    public LiveData<List<Brand>> getBrandsByCategory(String categoryId) {
        MutableLiveData<List<Brand>> brandsLiveData = new MutableLiveData<>();
        db.collection("brands")
                .whereEqualTo("categoryId", categoryId)
                .orderBy("name")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    List<Brand> list = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Brand brand = doc.toObject(Brand.class);
                            brand.setId(doc.getId());
                            list.add(brand);
                        }
                    }
                    brandsLiveData.setValue(list);
                });
        return brandsLiveData;
    }

    public LiveData<AuthResultState<Void>> addBrand(String name, String categoryId) {
        MutableLiveData<AuthResultState<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResultState.loading());
        Brand brand = new Brand(null, name, categoryId);
        db.collection("brands").add(brand)
                .addOnSuccessListener(documentReference -> result.setValue(AuthResultState.success(null)))
                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
        return result;
    }

    public LiveData<AuthResultState<Void>> deleteBrand(String id) {
        MutableLiveData<AuthResultState<Void>> result = new MutableLiveData<>();
        result.setValue(AuthResultState.loading());
        db.collection("brands").document(id).delete()
                .addOnSuccessListener(aVoid -> result.setValue(AuthResultState.success(null)))
                .addOnFailureListener(e -> result.setValue(AuthResultState.error(e.getMessage())));
        return result;
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




