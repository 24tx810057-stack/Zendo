package com.example.buoi1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminWarrantyListActivity extends AppCompatActivity {

    private RecyclerView rvWarranties;
    private View layoutEmpty;
    private FirebaseFirestore db;
    private List<WarrantyRequest> warrantyList = new ArrayList<>();
    private WarrantyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_warranty_list);

        db = FirebaseFirestore.getInstance();
        rvWarranties = findViewById(R.id.rvAdminWarranties);
        layoutEmpty = findViewById(R.id.layoutEmptyWarranty);

        findViewById(R.id.btnBackWarrantyList).setOnClickListener(v -> finish());

        adapter = new WarrantyAdapter();
        rvWarranties.setLayoutManager(new LinearLayoutManager(this));
        rvWarranties.setAdapter(adapter);

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
                    layoutEmpty.setVisibility(warrantyList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    class WarrantyAdapter extends RecyclerView.Adapter<WarrantyAdapter.ViewHolder> {
        private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_warranty, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WarrantyRequest item = warrantyList.get(position);
            holder.tvOrderId.setText("Mã đơn: " + item.getOrderId());
            holder.tvError.setText("Lỗi: " + item.getErrorType());
            holder.tvUser.setText("Khách hàng: " + item.getUserEmail());
            holder.tvTime.setText("Gửi lúc: " + sdf.format(new Date(item.getTimestamp())));

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

            holder.tvStatus.setText(statusText);
            holder.tvStatus.getBackground().setTint(color);

            if (item.getEvidenceImages() != null && !item.getEvidenceImages().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(item.getEvidenceImages().get(0), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivEvidence.setImageBitmap(bitmap);
                } catch (Exception e) {}
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
            TextView tvOrderId, tvError, tvUser, tvTime, tvStatus;
            ImageView ivEvidence;
            ViewHolder(View v) {
                super(v);
                tvOrderId = v.findViewById(R.id.tvAdminWarrantyOrderId);
                tvError = v.findViewById(R.id.tvAdminWarrantyError);
                tvUser = v.findViewById(R.id.tvAdminWarrantyUser);
                tvTime = v.findViewById(R.id.tvAdminWarrantyTime);
                tvStatus = v.findViewById(R.id.tvAdminWarrantyStatus);
                ivEvidence = v.findViewById(R.id.ivAdminWarrantyEvidence);
            }
        }
    }
}
