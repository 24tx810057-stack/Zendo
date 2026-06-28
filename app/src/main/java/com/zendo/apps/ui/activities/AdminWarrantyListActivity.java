package com.zendo.apps.ui.activities;

import com.zendo.apps.data.models.WarrantyRequest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zendo.apps.databinding.ActivityAdminWarrantyListBinding;
import com.zendo.apps.databinding.ItemAdminWarrantyBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminWarrantyListActivity extends AppCompatActivity {

    private ActivityAdminWarrantyListBinding binding;
    private FirebaseFirestore db;
    private List<WarrantyRequest> warrantyList = new ArrayList<>();
    private WarrantyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminWarrantyListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        binding.btnBackWarrantyList.setOnClickListener(v -> finish());

        adapter = new WarrantyAdapter();
        binding.rvAdminWarranties.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAdminWarranties.setAdapter(adapter);

        loadWarrantyRequests();
    }

    private void loadWarrantyRequests() {
        db.collection("warranty_requests")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    warrantyList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        WarrantyRequest request = doc.toObject(WarrantyRequest.class);
                        request.setId(doc.getId());
                        warrantyList.add(request);
                    }
                    adapter.notifyDataSetChanged();
                    binding.layoutEmptyWarranty.setVisibility(warrantyList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    class WarrantyAdapter extends RecyclerView.Adapter<WarrantyAdapter.ViewHolder> {
        private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAdminWarrantyBinding itemBinding = ItemAdminWarrantyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WarrantyRequest item = warrantyList.get(position);
            ItemAdminWarrantyBinding itemBinding = holder.itemBinding;

            itemBinding.tvAdminWarrantyOrderId.setText("Mã đơn: " + item.getOrderId());
            itemBinding.tvAdminWarrantyError.setText("Lỗi: " + item.getErrorType());
            itemBinding.tvAdminWarrantyUser.setText("Khách hàng: " + item.getUserEmail());
            itemBinding.tvAdminWarrantyTime.setText("Gửi lúc: " + sdf.format(new Date(item.getTimestamp())));

            String status = item.getStatus();
            String statusText = "CHỜ SỬA";
            int color = 0xFFFFA000; // Orange

            if ("repairing".equals(status)) {
                statusText = "ĐANG SỬA";
                color = 0xFF2196F3; // Blue
            } else if ("repaired".equals(status)) {
                statusText = "ĐÃ XONG";
                color = 0xFF4CAF50; // Green
            } else if ("rejected".equals(status)) {
                statusText = "TỪ CHỐI";
                color = 0xFFF44336; // Red
            }

            itemBinding.tvAdminWarrantyStatus.setText(statusText);
            itemBinding.tvAdminWarrantyStatus.getBackground().setTint(color);

            if (item.getEvidenceImages() != null && !item.getEvidenceImages().isEmpty()) {
                String imgData = item.getEvidenceImages().get(0);
                if (imgData.startsWith("http")) {
                    Glide.with(AdminWarrantyListActivity.this).load(imgData).into(itemBinding.ivAdminWarrantyEvidence);
                } else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        Glide.with(AdminWarrantyListActivity.this).load(decodedString).into(itemBinding.ivAdminWarrantyEvidence);
                    } catch (Exception e) {}
                }
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(AdminWarrantyListActivity.this, AdminWarrantyDetailActivity.class);
                intent.putExtra("warranty_data", item);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return warrantyList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ItemAdminWarrantyBinding itemBinding;
            ViewHolder(ItemAdminWarrantyBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }
    }
}


