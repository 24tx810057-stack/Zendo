package com.zendo.apps.ui.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.zendo.apps.R;
import com.zendo.apps.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);

        binding.btnBackSettings.setOnClickListener(v -> finish());

        // Load saved states
        binding.swOrderNotif.setChecked(sharedPref.getBoolean("order_notif", true));
        binding.swPromoNotif.setChecked(sharedPref.getBoolean("promo_notif", true));
        binding.swDarkMode.setChecked(sharedPref.getBoolean("dark_mode", false));

        // Save listeners
        binding.swOrderNotif.setOnCheckedChangeListener((v, isChecked) -> {
            sharedPref.edit().putBoolean("order_notif", isChecked).apply();
            String msg = isChecked ? "Đã bật thông báo đơn hàng" : "Đã tắt thông báo đơn hàng";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        binding.swPromoNotif.setOnCheckedChangeListener((v, isChecked) -> {
            sharedPref.edit().putBoolean("promo_notif", isChecked).apply();
            String msg = isChecked ? "Đã bật thông báo khuyến mãi" : "Đã tắt thông báo khuyến mãi";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        binding.swDarkMode.setOnCheckedChangeListener((v, isChecked) -> {
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply();
            String msg = isChecked ? "Đã chuyển sang chế độ tối" : "Đã chuyển sang chế độ sáng";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        binding.btnAbout.setOnClickListener(v -> showInfoDialog("Về Zendo", 
            "Zendo là ứng dụng mua sắm hàng đầu với trải nghiệm mượt mà và hiện đại.\n\n" +
            "Phiên bản: 1.0.0\n" +
            "Phát triển bởi: Zendo Team\n" +
            "© 2024 Zendo Apps"));

        binding.btnPolicy.setOnClickListener(v -> showInfoDialog("Chính sách bảo mật",
            "Chúng tôi cam kết bảo mật thông tin cá nhân của bạn.\n\n" +
            "1. Thông tin thu thập: Tên, số điện thoại, địa chỉ để giao hàng.\n" +
            "2. Mục đích: Xử lý đơn hàng và hỗ trợ khách hàng.\n" +
            "3. Bảo mật: Dữ liệu được mã hóa và bảo vệ bởi Firebase."));
    }

    private void showInfoDialog(String title, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvInfoTitle);
        TextView tvContent = dialogView.findViewById(R.id.tvInfoContent);
        
        tvTitle.setText(title);
        tvContent.setText(content);

        dialogView.findViewById(R.id.btnCloseInfo).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
