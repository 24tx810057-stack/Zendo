package com.zendo.apps.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.zendo.apps.data.models.AuthResultState;
import com.zendo.apps.data.models.User;
import com.zendo.apps.data.repositories.UserRepository;

public class UserViewModel extends ViewModel {
    private UserRepository repository = new UserRepository();
    private MutableLiveData<String> userEmail = new MutableLiveData<>();

    public LiveData<User> user = Transformations.switchMap(userEmail, email ->
            repository.getUser(email)
    );

    public void setUserEmail(String email) {
        userEmail.setValue(email);
    }

    public LiveData<AuthResultState<User>> login(String email, String password) {
        return repository.login(email, password);
    }

    public LiveData<AuthResultState<User>> register(User user, String password) {
        return repository.register(user, password);
    }

    public LiveData<AuthResultState<String>> findEmailByPhone(String phone) {
        return repository.findEmailByPhone(phone);
    }

    public void updateProfile(User user, UserRepository.OnCompleteListener listener) {
        repository.updateUser(user, listener);
    }
}
