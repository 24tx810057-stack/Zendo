package com.example.buoi1;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etAccount;
    private Button btnReset;
    private ImageView btnBack;
    private TextView tvBackLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etAccount = findViewById(R.id.etForgotAccount);
        btnReset = findViewById(R.id.btnResetPassword);
        btnBack = findViewById(R.id.btnBackForgot);
        tvBackLogin = findViewById(R.id.tvBackToLogin);

        btnBack.setOnClickListener(v -> finish());
        tvBackLogin.setOnClickListener(v -> finish());

        btnReset.setOnClickListener(v -> {
            String account = etAccount.getText().toString().trim();
            if (TextUtils.isEmpty(account)) {
                Toast.makeText(this, "Vui lòng nhập Email hoặc Số điện thoại", Toast.LENGTH_SHORT).show();
            } else {
                // Giả lập gửi yêu cầu thành công
                Toast.makeText(this, "Yêu cầu đã được gửi! Vui lòng kiểm tra thông báo của bạn.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}
