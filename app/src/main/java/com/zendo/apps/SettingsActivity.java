package com.zendo.apps;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
        binding.swOrderNotif.setOnCheckedChangeListener((v, isChecked) -> 
            sharedPref.edit().putBoolean("order_notif", isChecked).apply());

        binding.swPromoNotif.setOnCheckedChangeListener((v, isChecked) -> 
            sharedPref.edit().putBoolean("promo_notif", isChecked).apply());

        binding.swDarkMode.setOnCheckedChangeListener((v, isChecked) -> {
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply();
            Toast.makeText(this, "Tính năng này sẽ sớm được hoàn thiện!", Toast.LENGTH_SHORT).show();
        });

        binding.btnAbout.setOnClickListener(v -> 
            Toast.makeText(this, "Zendo v1.0.0 - Dự án Buổi 1", Toast.LENGTH_SHORT).show());

        binding.btnPolicy.setOnClickListener(v ->
            Toast.makeText(this, "Chính sách bảo mật đang được cập nhật", Toast.LENGTH_SHORT).show());
    }
}
