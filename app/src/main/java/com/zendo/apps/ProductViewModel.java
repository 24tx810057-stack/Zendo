package com.zendo.apps;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
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

    private static class FilterParams {
        String brand;
        String sortOrder;

        FilterParams(String brand, String sortOrder) {
            this.brand = brand;
            this.sortOrder = sortOrder;
        }
    }
}
