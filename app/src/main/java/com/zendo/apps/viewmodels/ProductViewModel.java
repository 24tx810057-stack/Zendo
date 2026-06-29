package com.zendo.apps.viewmodels;

import com.zendo.apps.data.repositories.ProductRepository;

import com.zendo.apps.data.models.Product;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.zendo.apps.data.models.AuthResultState;
import com.zendo.apps.data.models.Brand;
import com.zendo.apps.data.models.Category;
import com.zendo.apps.data.models.Review;
import java.util.List;

public class ProductViewModel extends AndroidViewModel {
    private ProductRepository repository;
    
    private MutableLiveData<FilterParams> filterParams = new MutableLiveData<>(new FilterParams("Tất cả", "default"));

    public ProductViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(application);
    }

    public LiveData<List<Product>> products = Transformations.switchMap(filterParams, params -> 
        repository.getProducts(params.brand, params.sortOrder)
    );

    public void setFilter(String brand, String sortOrder) {
        filterParams.setValue(new FilterParams(brand, sortOrder));
    }

    public LiveData<Product> getProductDetail(String productId) {
        return repository.getProductById(productId);
    }

    public LiveData<List<Product>> getSimilarProducts(String category, String currentProductId) {
        return repository.getSimilarProducts(category, currentProductId);
    }

    public LiveData<AuthResultState<Void>> toggleFavorite(String productId, String userEmail, boolean isLiked) {
        return repository.toggleFavorite(productId, userEmail, isLiked);
    }

    public LiveData<List<Review>> getReviews(String productId) {
        return repository.getReviews(productId);
    }

    public LiveData<List<Category>> getCategories() {
        return repository.getCategories();
    }

    public LiveData<AuthResultState<Void>> addCategory(String name) {
        return repository.addCategory(name);
    }

    public LiveData<AuthResultState<Void>> deleteCategory(String id) {
        return repository.deleteCategory(id);
    }

    public LiveData<List<Brand>> getBrands() {
        return repository.getBrands();
    }

    public LiveData<List<Brand>> getBrandsByCategory(String categoryId) {
        return repository.getBrandsByCategory(categoryId);
    }

    public LiveData<AuthResultState<Void>> addBrand(String name, String categoryId) {
        return repository.addBrand(name, categoryId);
    }

    public LiveData<AuthResultState<Void>> deleteBrand(String id) {
        return repository.deleteBrand(id);
    }

    public LiveData<AuthResultState<Void>> deleteProduct(String productId) {
        return repository.deleteProduct(productId);
    }

    private static class FilterParams {
        String brand;
        String sortOrder;

        FilterParams(String brand, String sortOrder) {
            this.brand = brand;
            this.sortOrder = sortOrder;
        }
    }
}



