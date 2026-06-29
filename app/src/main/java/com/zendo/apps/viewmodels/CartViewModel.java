package com.zendo.apps.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.zendo.apps.data.models.AuthResultState;
import com.zendo.apps.data.models.CartItem;
import com.zendo.apps.data.repositories.CartRepository;

import java.util.List;

public class CartViewModel extends AndroidViewModel {
    private final CartRepository repository;
    private LiveData<List<CartItem>> cartItemsLiveData;
    private String lastEmail;

    public CartViewModel(@NonNull Application application) {
        super(application);
        repository = new CartRepository();
    }

    public LiveData<AuthResultState<Void>> addToCart(CartItem item) {
        return repository.addToCart(item);
    }

    public LiveData<List<CartItem>> getCartItems(String email) {
        if (cartItemsLiveData == null || !email.equals(lastEmail)) {
            lastEmail = email;
            cartItemsLiveData = repository.getCartItems(email);
        }
        return cartItemsLiveData;
    }

    public void updateQuantity(String itemId, int quantity) {
        repository.updateQuantity(itemId, quantity);
    }

    public LiveData<AuthResultState<Void>> deleteItems(List<String> itemIds) {
        return repository.deleteItems(itemIds);
    }
}
