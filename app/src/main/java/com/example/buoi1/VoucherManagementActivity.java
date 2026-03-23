package com.example.buoi1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class VoucherManagementActivity extends AppCompatActivity {

    private RecyclerView rvVouchers;
    private VoucherAdapter adapter;
    private List<Voucher> voucherList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher_management);

        db = FirebaseFirestore.getInstance();
        rvVouchers = findViewById(R.id.rvVouchers);
        ImageView btnBack = findViewById(R.id.btnBackVoucher);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddVoucher);

        btnBack.setOnClickListener(v -> finish());
        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AddVoucherActivity.class)));

        rvVouchers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VoucherAdapter(this, voucherList);
        rvVouchers.setAdapter(adapter);

        loadVouchers();
    }

    private void loadVouchers() {
        db.collection("vouchers")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        voucherList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Voucher voucher = doc.toObject(Voucher.class);
                            voucher.setId(doc.getId());
                            voucherList.add(voucher);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
