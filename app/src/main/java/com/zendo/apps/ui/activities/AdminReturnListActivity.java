package com.zendo.apps.ui.activities;

import com.zendo.apps.R;

import com.zendo.apps.data.models.ReturnRequest;

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
import com.zendo.apps.databinding.ActivityAdminReturnListBinding;
import com.zendo.apps.databinding.ItemAdminReturnBinding;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class AdminReturnListActivity extends AppCompatActivity {

    private ActivityAdminReturnListBinding binding;
    private FirebaseFirestore db;
    private List<ReturnRequest> returnList = new ArrayList<>();
    private ReturnAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReturnListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        binding.btnBackReturnList.setOnClickListener(v -> finish());

        adapter = new ReturnAdapter();
        binding.rvAdminReturns.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAdminReturns.setAdapter(adapter);

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
                    binding.layoutEmptyReturn.setVisibility(returnList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    class ReturnAdapter extends RecyclerView.Adapter<ReturnAdapter.ViewHolder> {
        private final DecimalFormat formatter = new DecimalFormat("###,###,###");

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAdminReturnBinding itemBinding = ItemAdminReturnBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReturnRequest item = returnList.get(position);
            ItemAdminReturnBinding itemBinding = holder.itemBinding;

            itemBinding.tvAdminReturnOrderId.setText("Mã đơn: " + item.getOrderId());
            itemBinding.tvAdminReturnReason.setText("Lý do: " + item.getReason());
            itemBinding.tvAdminReturnUser.setText("Người gửi: " + item.getUserEmail());
            itemBinding.tvAdminReturnAmount.setText("Hoàn tiền: " + formatter.format(item.getRefundAmount()) + "đ");

            String status = item.getStatus();
            itemBinding.tvAdminReturnStatus.setText(status.equals("pending") ? "ĐANG CHỜ" : (status.equals("approved") ? "ĐÃ DUYỆT" : "TỪ CHỐI"));
            
            if (status.equals("pending")) itemBinding.tvAdminReturnStatus.setBackgroundResource(R.drawable.bg_role_chip); 
            else if (status.equals("approved")) itemBinding.tvAdminReturnStatus.setBackgroundResource(R.drawable.bg_circle_red_badge); 

            if (item.getEvidenceImages() != null && !item.getEvidenceImages().isEmpty()) {
                String imgData = item.getEvidenceImages().get(0);
                if (imgData.startsWith("http")) {
                    Glide.with(AdminReturnListActivity.this).load(imgData).into(itemBinding.ivAdminReturnEvidence);
                } else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        Glide.with(AdminReturnListActivity.this).load(decodedString).into(itemBinding.ivAdminReturnEvidence);
                    } catch (Exception e) {}
                }
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
            final ItemAdminReturnBinding itemBinding;
            ViewHolder(ItemAdminReturnBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }
    }
}



