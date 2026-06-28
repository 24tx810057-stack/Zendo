package com.zendo.apps.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.zendo.apps.data.models.AuthResultState;
import com.zendo.apps.data.models.User;
import com.zendo.apps.databinding.ActivityMainBinding;
import com.zendo.apps.utils.MigrationTool;
import com.zendo.apps.utils.SharedPrefManager;
import com.zendo.apps.viewmodels.UserViewModel;

import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private UserViewModel viewModel;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(UserViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        // One-time migration
        new MigrationTool(this).startMigration();

        // Check if user is already logged in
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, ListActivity.class));
            finish();
            return;
        }

        binding.button.setOnClickListener(v -> loginUser());
        binding.tvSignUp.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void loginUser() {
        String input = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(input) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đủ tài khoản và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            performLogin(input, password);
        } else if (isValidPhoneNumber(input)) {
            findEmailAndLogin(input, password);
        } else {
            Toast.makeText(this, "Email hoặc số điện thoại không đúng định dạng", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidPhoneNumber(String phone) {
        return Pattern.compile("^[0-9]{9,11}$").matcher(phone).matches();
    }

    private void findEmailAndLogin(String phone, String password) {
        viewModel.findEmailByPhone(phone).observe(this, state -> {
            switch (state.getStatus()) {
                case LOADING:
                    binding.button.setEnabled(false);
                    binding.button.setText("ĐANG KIỂM TRA...");
                    break;
                case SUCCESS:
                    performLogin(state.getData(), password);
                    break;
                case ERROR:
                    resetLoginButton();
                    Toast.makeText(this, state.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void performLogin(String email, String password) {
        viewModel.login(email, password).observe(this, state -> {
            switch (state.getStatus()) {
                case LOADING:
                    binding.button.setEnabled(false);
                    binding.button.setText("ĐANG ĐĂNG NHẬP...");
                    break;
                case SUCCESS:
                    onLoginSuccess(state.getData());
                    break;
                case ERROR:
                    resetLoginButton();
                    Toast.makeText(this, state.getMessage(), Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void onLoginSuccess(User user) {
        if (user != null) {
            SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
            prefManager.saveUserRole(user.getRole() != null ? user.getRole() : "user");
            prefManager.saveUserEmail(user.getEmail());
            prefManager.saveUserId(user.getId());

            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ListActivity.class));
            finish();
        }
    }

    private void resetLoginButton() {
        binding.button.setEnabled(true);
        binding.button.setText("ĐĂNG NHẬP");
    }
}
