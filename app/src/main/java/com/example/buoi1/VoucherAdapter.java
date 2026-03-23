package com.example.buoi1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DecimalFormat;
import java.util.List;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private Context context;
    private List<Voucher> voucherList;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
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
        
        String valueStr = voucher.getType().equals("percent") ? 
                (int)voucher.getValue() + "%" : formatter.format(voucher.getValue()) + "đ";
        holder.tvValue.setText("Giảm: " + valueStr);
        holder.tvMinOrder.setText("Đơn tối thiểu: " + formatter.format(voucher.getMinOrder()) + "đ");
        
        holder.swActive.setChecked(voucher.isActive());
        holder.swActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("vouchers").document(voucher.getId()).update("active", isChecked);
        });

        holder.btnDelete.setOnClickListener(v -> {
            db.collection("vouchers").document(voucher.getId()).delete();
        });
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    static class VoucherViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCode, tvValue, tvMinOrder;
        Switch swActive;
        ImageView btnDelete;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvVoucherTitleItem);
            tvCode = itemView.findViewById(R.id.tvVoucherCodeItem);
            tvValue = itemView.findViewById(R.id.tvVoucherValueItem);
            tvMinOrder = itemView.findViewById(R.id.tvVoucherMinOrderItem);
            swActive = itemView.findViewById(R.id.swVoucherActive);
            btnDelete = itemView.findViewById(R.id.btnDeleteVoucher);
        }
    }
}
