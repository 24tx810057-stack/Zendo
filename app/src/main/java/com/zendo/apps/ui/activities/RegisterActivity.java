package com.zendo.apps.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.zendo.apps.data.models.User;
import com.zendo.apps.databinding.RegisterBinding;
import com.zendo.apps.viewmodels.UserViewModel;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private RegisterBinding binding;
    private UserViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = RegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(UserViewModel.class);

        binding.tvBackToLogin.setOnClickListener(v -> finish());
        binding.btnBackToLoginIcon.setOnClickListener(v -> finish());

        binding.btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String fullName = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String password = binding.etRegPassword.getText().toString().trim();
        String confirmPass = binding.etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "Mật khẩu không đủ mạnh (Xem hướng dẫn bên dưới)", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        User newUser = new User(null, fullName, email, password, phone, "user");

        viewModel.register(newUser, password).observe(this, state -> {
            switch (state.getStatus()) {
                case LOADING:
                    binding.btnRegister.setEnabled(false);
                    binding.btnRegister.setText("ĐANG ĐĂNG KÝ...");
                    break;
                case SUCCESS:
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case ERROR:
                    binding.btnRegister.setEnabled(true);
                    binding.btnRegister.setText("ĐĂNG KÝ");
                    Toast.makeText(RegisterActivity.this, "Lỗi: " + state.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private boolean isValidPassword(String password) {
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{5,}$";
        return Pattern.compile(passwordPattern).matcher(password).matches();
    }
}
