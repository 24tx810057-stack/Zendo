package com.example.buoi1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.TypedValue;
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
        
        // FIX LỖI 6đ: Dùng equalsIgnoreCase để không phân biệt PERCENT hay percent
        String valueStr = (voucher.getType() != null && voucher.getType().equalsIgnoreCase("PERCENT")) ? 
                (int)voucher.getValue() + "%" : formatter.format(voucher.getValue()) + "đ";
        
        holder.tvValue.setText(valueStr);
        holder.tvMinOrder.setText("Đơn tối thiểu: " + formatter.format(voucher.getMinOrder()) + "đ");
        
        long now = System.currentTimeMillis();
        boolean isExpired = voucher.getExpiryDate() > 0 && voucher.getExpiryDate() < now;
        String expiryText = voucher.getExpiryDate() > 0 ? "Hết hạn: " + sdf.format(new Date(voucher.getExpiryDate())) : "Không hết hạn";

        // Logic UI cho Voucher hết hạn
        if (isExpired) {
            if (holder.layoutLeft != null) holder.layoutLeft.setBackgroundColor(Color.parseColor("#BDBDBD")); // Màu xám
            holder.itemView.setAlpha(0.6f);
            holder.tvExpiry.setTextColor(Color.RED);
            holder.tvExpiry.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
            holder.tvExpiry.setText(expiryText + " (HẾT HẠN)");
        } else {
            if (holder.layoutLeft != null) holder.layoutLeft.setBackgroundColor(Color.parseColor("#2196F3")); // Màu xanh main
            holder.itemView.setAlpha(voucher.isActive() ? 1.0f : 0.6f);
            holder.tvExpiry.setTextColor(Color.parseColor("#9E9E9E"));
            holder.tvExpiry.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f); // Size mặc định
            holder.tvExpiry.setText(expiryText);
        }

        holder.swActive.setOnCheckedChangeListener(null);
        holder.swActive.setChecked(voucher.isActive());
        holder.swActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("vouchers").document(voucher.getId())
                    .update("active", isChecked)
                    .addOnSuccessListener(aVoid -> {
                        voucher.setActive(isChecked);
                        if (!isExpired) updateItemAlpha(holder.itemView, isChecked);
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
        View layoutLeft;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvVoucherTitleItem);
            tvCode = itemView.findViewById(R.id.tvVoucherCodeItem);
            tvValue = itemView.findViewById(R.id.tvVoucherValueItem);
            tvMinOrder = itemView.findViewById(R.id.tvVoucherMinOrderItem);
            tvExpiry = itemView.findViewById(R.id.tvVoucherExpiryItem);
            swActive = itemView.findViewById(R.id.swVoucherActive);
            btnDelete = itemView.findViewById(R.id.btnDeleteVoucher);
            layoutLeft = itemView.findViewById(R.id.layoutLeftVoucher);
        }
    }
}
