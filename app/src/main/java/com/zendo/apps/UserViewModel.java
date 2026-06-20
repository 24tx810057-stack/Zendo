package com.zendo.apps;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;

public class UserViewModel extends ViewModel {
    private UserRepository repository = new UserRepository();
    private MutableLiveData<String> userEmail = new MutableLiveData<>();

    public LiveData<User> user = Transformations.switchMap(userEmail, email -> 
        repository.getUser(email)
    );

    public void setUserEmail(String email) {
        userEmail.setValue(email);
    }

    public void updateProfile(User user, UserRepository.OnCompleteListener listener) {
        repository.updateUser(user, listener);
    }
}
