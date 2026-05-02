package com.example.buoi1;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class WarrantyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warranty_policy);

        ImageView btnBack = findViewById(R.id.btnBackPolicy);
        btnBack.setOnClickListener(v -> finish());
    }
}
