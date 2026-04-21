package com.example.buoi1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private Context context;
    private List<Voucher> voucherList;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public VoucherAdapter(Context context, List<Voucher> voucherList) {
        this.context = context;
        this.voucherList = voucherList;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = voucherList.get(position);

        holder.tvTitle.setText(voucher.getTitle());
        holder.tvCode.setText("Mã: " + voucher.getCode());
        
        String valueStr = "percent".equals(voucher.getType()) ? 
                (int)voucher.getValue() + "%" : formatter.format(voucher.getValue()) + "đ";
        holder.tvValue.setText("Giảm: " + valueStr);
        holder.tvMinOrder.setText("Đơn tối thiểu: " + formatter.format(voucher.getMinOrder()) + "đ");
        
        if (voucher.getExpiryDate() > 0) {
            holder.tvExpiry.setText("Hết hạn: " + sdf.format(new Date(voucher.getExpiryDate())));
        } else {
            holder.tvExpiry.setText("Không hết hạn");
        }

        // UI hiệu ứng bật/tắt
        updateItemAlpha(holder.itemView, voucher.isActive());

        holder.swActive.setOnCheckedChangeListener(null);
        holder.swActive.setChecked(voucher.isActive());
        holder.swActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("vouchers").document(voucher.getId())
                    .update("active", isChecked)
                    .addOnSuccessListener(aVoid -> {
                        voucher.setActive(isChecked);
                        updateItemAlpha(holder.itemView, isChecked);
                    })
                    .addOnFailureListener(e -> {
                        holder.swActive.setChecked(!isChecked);
                        Toast.makeText(context, "Lỗi cập nhật", Toast.LENGTH_SHORT).show();
                    });
        });

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa voucher này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        db.collection("vouchers").document(voucher.getId()).delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Bấm vào item để sửa (Truyền object Voucher sang AddVoucherActivity)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddVoucherActivity.class);
            intent.putExtra("VOUCHER_DATA", voucher);
            context.startActivity(intent);
        });
    }

    private void updateItemAlpha(View itemView, boolean isActive) {
        itemView.setAlpha(isActive ? 1.0f : 0.6f);
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    static class VoucherViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCode, tvValue, tvMinOrder, tvExpiry;
        Switch swActive;
        ImageView btnDelete;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvVoucherTitleItem);
            tvCode = itemView.findViewById(R.id.tvVoucherCodeItem);
            tvValue = itemView.findViewById(R.id.tvVoucherValueItem);
            tvMinOrder = itemView.findViewById(R.id.tvVoucherMinOrderItem);
            tvExpiry = itemView.findViewById(R.id.tvVoucherExpiryItem);
            swActive = itemView.findViewById(R.id.swVoucherActive);
            btnDelete = itemView.findViewById(R.id.btnDeleteVoucher);
        }
    }
}