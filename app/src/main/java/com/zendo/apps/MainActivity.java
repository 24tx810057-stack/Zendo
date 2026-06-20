package com.zendo.apps;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.zendo.apps.databinding.ActivityMainBinding;
import com.zendo.apps.utils.MigrationTool;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
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

        binding.button.setEnabled(false);
        binding.button.setText("ĐANG ĐĂNG NHẬP...");

        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            // Đăng nhập bằng email trực tiếp
            performFirebaseLogin(input, password);
        } else if (isValidPhoneNumber(input)) {
            // Tìm email từ số điện thoại trước khi đăng nhập
            findEmailByPhoneAndLogin(input, password);
        } else {
            resetLoginButton();
            Toast.makeText(this, "Email hoặc số điện thoại không đúng định dạng", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidPhoneNumber(String phone) {
        // Kiểm tra số điện thoại cơ bản (chỉ số, từ 9-11 ký tự)
        return Pattern.compile("^[0-9]{9,11}$").matcher(phone).matches();
    }

    private void findEmailByPhoneAndLogin(String phone, String password) {
        binding.button.setText("ĐANG KIỂM TRA...");
        // Tìm kiếm linh hoạt: Kiểm tra cả trường 'phone' và trường 'email' (phòng trường hợp lưu nhầm số điện thoại vào field email)
        db.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        processEmailFound(querySnapshot.getDocuments().get(0), password);
                    } else {
                        // Thử tìm trong trường email xem có lưu số điện thoại ở đó không (như trong ảnh bạn gửi)
                        db.collection("users")
                                .whereEqualTo("email", phone)
                                .get()
                                .addOnSuccessListener(querySnapshot2 -> {
                                    if (!querySnapshot2.isEmpty()) {
                                        processEmailFound(querySnapshot2.getDocuments().get(0), password);
                                    } else {
                                        // Thử tìm trong trường e-mail (có dấu gạch ngang)
                                        db.collection("users")
                                                .whereEqualTo("e-mail", phone)
                                                .get()
                                                .addOnSuccessListener(querySnapshot3 -> {
                                                    if (!querySnapshot3.isEmpty()) {
                                                        processEmailFound(querySnapshot3.getDocuments().get(0), password);
                                                    } else {
                                                        resetLoginButton();
                                                        Toast.makeText(this, "Số điện thoại chưa được đăng ký!", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    resetLoginButton();
                                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    resetLoginButton();
                                    Toast.makeText(this, "Lỗi kiểm tra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    resetLoginButton();
                    Toast.makeText(this, "Lỗi kết nối Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void processEmailFound(DocumentSnapshot document, String password) {
        // Kiểm tra tất cả các trường có thể chứa email
        String email = document.getString("email");
        if (email == null) email = document.getString("e-mail");
        
        // Nếu vẫn null, thử lấy từ ID của tài liệu (phòng trường hợp ID là email)
        if (email == null && document.getId().contains("@")) {
            email = document.getId();
        }
        
        if (email != null) {
            performFirebaseLogin(email, password);
        } else {
            resetLoginButton();
            Toast.makeText(this, "Không tìm thấy địa chỉ Email trong dữ liệu!", Toast.LENGTH_SHORT).show();
        }
    }

    private void performFirebaseLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Tìm kiếm user trong Firestore bằng Query để linh hoạt hơn (hỗ trợ cả field 'email' và 'e-mail')
                        db.collection("users")
                                .whereIn("email", java.util.Arrays.asList(email, email.toLowerCase()))
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        processLoginSuccess(querySnapshot.getDocuments().get(0), email);
                                    } else {
                                        // Thử tìm với field 'e-mail' (dành cho admin cũ trong ảnh của bạn)
                                        db.collection("users")
                                                .whereEqualTo("e-mail", email)
                                                .get()
                                                .addOnSuccessListener(querySnapshotOld -> {
                                                    if (!querySnapshotOld.isEmpty()) {
                                                        processLoginSuccess(querySnapshotOld.getDocuments().get(0), email);
                                                    } else {
                                                        resetLoginButton();
                                                        Toast.makeText(this, "Dữ liệu người dùng không tồn tại trong Firestore!", Toast.LENGTH_LONG).show();
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    resetLoginButton();
                                                    Toast.makeText(this, "Lỗi truy vấn Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    resetLoginButton();
                                    Toast.makeText(this, "Lỗi Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        resetLoginButton();
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Sai tài khoản hoặc mật khẩu";
                        if (errorMsg != null && (errorMsg.contains("password is invalid") || errorMsg.contains("no user record") || errorMsg.contains("found no user"))) {
                            errorMsg = "Tài khoản hoặc mật khẩu không chính xác";
                        }
                        Toast.makeText(this, "Đăng nhập thất bại: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void processLoginSuccess(DocumentSnapshot documentSnapshot, String email) {
        User user = documentSnapshot.toObject(User.class);
        if (user != null) {
            // Đảm bảo lấy được Email và Role kể cả khi tên trường trong DB bị lệch (email vs e-mail)
            String role = documentSnapshot.getString("role");
            if (role == null) role = "user";

            SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
            prefManager.saveUserRole(role);
            prefManager.saveUserEmail(email);

            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ListActivity.class));
            finish();
        } else {
            resetLoginButton();
            Toast.makeText(this, "Lỗi: Không thể đọc dữ liệu người dùng!", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetLoginButton() {
        binding.button.setEnabled(true);
        binding.button.setText("ĐĂNG NHẬP");
    }
}
