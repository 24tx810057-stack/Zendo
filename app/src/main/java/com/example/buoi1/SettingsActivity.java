package com.example.buoi1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat swOrderNotif, swPromoNotif, swDarkMode;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);

        swOrderNotif = findViewById(R.id.swOrderNotif);
        swPromoNotif = findViewById(R.id.swPromoNotif);
        swDarkMode = findViewById(R.id.swDarkMode);

        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());

        // Load saved states
        swOrderNotif.setChecked(sharedPref.getBoolean("order_notif", true));
        swPromoNotif.setChecked(sharedPref.getBoolean("promo_notif", true));
        swDarkMode.setChecked(sharedPref.getBoolean("dark_mode", false));

        // Save listeners
        swOrderNotif.setOnCheckedChangeListener((v, isChecked) -> 
            sharedPref.edit().putBoolean("order_notif", isChecked).apply());

        swPromoNotif.setOnCheckedChangeListener((v, isChecked) -> 
            sharedPref.edit().putBoolean("promo_notif", isChecked).apply());

        swDarkMode.setOnCheckedChangeListener((v, isChecked) -> {
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply();
            Toast.makeText(this, "Tính năng này sẽ sớm được hoàn thiện!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnAbout).setOnClickListener(v -> 
            Toast.makeText(this, "Zendo v1.0.0 - Dự án Buổi 1", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnPolicy).setOnClickListener(v -> 
            Toast.makeText(this, "Chính sách bảo mật đang được cập nhật", Toast.LENGTH_SHORT).show());
    }
}
