package com.zendo.apps;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zendo.apps.databinding.ActivityVoucherManagementBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VoucherManagementActivity extends AppCompatActivity {

    private ActivityVoucherManagementBinding binding;
    private VoucherAdapter adapter;
    private List<Voucher> voucherList = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration voucherListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoucherManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        binding.btnBackVoucher.setOnClickListener(v -> finish());
        binding.fabAddVoucher.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddVoucherActivity.class);
            startActivity(intent);
        });

        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VoucherAdapter(this, voucherList);
        binding.rvVouchers.setAdapter(adapter);

        loadVouchers();
    }

    private void loadVouchers() {
        if (voucherListener != null) voucherListener.remove();
        
        // Sắp xếp voucher: Cái nào mới tạo hoặc còn hạn hiện lên trên
        voucherListener = db.collection("vouchers")
                .orderBy("expiryDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        voucherList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Voucher voucher = doc.toObject(Voucher.class);
                            voucher.setId(doc.getId());
                            voucherList.add(voucher);
                        }
                        
                        // Sắp xếp thêm logic: Voucher nào hết hạn thì cho xuống cuối cùng
                        long now = System.currentTimeMillis();
                        Collections.sort(voucherList, (v1, v2) -> {
                            boolean isExpired1 = v1.getExpiryDate() < now;
                            boolean isExpired2 = v2.getExpiryDate() < now;
                            if (isExpired1 && !isExpired2) return 1;
                            if (!isExpired1 && isExpired2) return -1;
                            return Long.compare(v2.getExpiryDate(), v1.getExpiryDate());
                        });

                        adapter.notifyDataSetChanged();
                        
                        if (voucherList.isEmpty()) {
                            binding.llEmptyVoucher.setVisibility(View.VISIBLE);
                            binding.rvVouchers.setVisibility(View.GONE);
                        } else {
                            binding.llEmptyVoucher.setVisibility(View.GONE);
                            binding.rvVouchers.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voucherListener != null) voucherListener.remove();
    }
}
