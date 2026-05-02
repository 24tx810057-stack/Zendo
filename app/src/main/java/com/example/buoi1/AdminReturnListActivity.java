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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class AdminReturnListActivity extends AppCompatActivity {

    private RecyclerView rvReturns;
    private View layoutEmpty;
    private FirebaseFirestore db;
    private List<ReturnRequest> returnList = new ArrayList<>();
    private ReturnAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_return_list);

        db = FirebaseFirestore.getInstance();
        rvReturns = findViewById(R.id.rvAdminReturns);
        layoutEmpty = findViewById(R.id.layoutEmptyReturn);

        findViewById(R.id.btnBackReturnList).setOnClickListener(v -> finish());

        adapter = new ReturnAdapter();
        rvReturns.setLayoutManager(new LinearLayoutManager(this));
        rvReturns.setAdapter(adapter);

        loadReturnRequests();
    }

    private void loadReturnRequests() {
        db.collection("return_requests")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    returnList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        ReturnRequest request = doc.toObject(ReturnRequest.class);
                        request.setId(doc.getId());
                        returnList.add(request);
                    }
                    adapter.notifyDataSetChanged();
                    layoutEmpty.setVisibility(returnList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    class ReturnAdapter extends RecyclerView.Adapter<ReturnAdapter.ViewHolder> {
        private DecimalFormat formatter = new DecimalFormat("###,###,###");

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_return, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReturnRequest item = returnList.get(position);
            holder.tvOrderId.setText("Mã đơn: " + item.getOrderId());
            holder.tvReason.setText("Lý do: " + item.getReason());
            holder.tvUser.setText("Người gửi: " + item.getUserEmail());
            holder.tvAmount.setText("Hoàn tiền: " + formatter.format(item.getRefundAmount()) + "đ");

            String status = item.getStatus();
            holder.tvStatus.setText(status.equals("pending") ? "ĐANG CHỜ" : (status.equals("approved") ? "ĐÃ DUYỆT" : "TỪ CHỐI"));
            
            if (status.equals("pending")) holder.tvStatus.setBackgroundResource(R.drawable.bg_role_chip); // Vàng/Xanh tùy drawable
            else if (status.equals("approved")) holder.tvStatus.setBackgroundResource(R.drawable.bg_circle_red_badge); // Đổi màu sau

            if (item.getEvidenceImages() != null && !item.getEvidenceImages().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(item.getEvidenceImages().get(0), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivEvidence.setImageBitmap(bitmap);
                } catch (Exception e) {}
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(AdminReturnListActivity.this, AdminReturnDetailActivity.class);
                intent.putExtra("return_data", item);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return returnList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvReason, tvUser, tvAmount, tvStatus;
            ImageView ivEvidence;
            ViewHolder(View v) {
                super(v);
                tvOrderId = v.findViewById(R.id.tvAdminReturnOrderId);
                tvReason = v.findViewById(R.id.tvAdminReturnReason);
                tvUser = v.findViewById(R.id.tvAdminReturnUser);
                tvAmount = v.findViewById(R.id.tvAdminReturnAmount);
                tvStatus = v.findViewById(R.id.tvAdminReturnStatus);
                ivEvidence = v.findViewById(R.id.ivAdminReturnEvidence);
            }
        }
    }
}
