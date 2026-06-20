package com.zendo.apps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DecimalFormat;
import java.util.List;

public class VoucherSelectionAdapter extends RecyclerView.Adapter<VoucherSelectionAdapter.ViewHolder> {

    private List<Voucher> voucherList;
    private OnVoucherClickListener listener;
    private int selectedPosition = -1;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    public interface OnVoucherClickListener {
        void onVoucherSelected(Voucher voucher);
    }

    public VoucherSelectionAdapter(List<Voucher> voucherList, OnVoucherClickListener listener) {
        this.voucherList = voucherList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Voucher voucher = voucherList.get(position);
        
        holder.tvTitle.setText(voucher.getTitle());
        holder.tvCode.setText(voucher.getCode());
        
        String valueStr = (voucher.getType() != null && voucher.getType().equalsIgnoreCase("PERCENT")) ? 
                (int)voucher.getValue() + "%" : formatter.format(voucher.getValue()) + "đ";
        holder.tvValue.setText("Giảm " + valueStr);
        
        holder.rbSelect.setChecked(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onVoucherSelected(voucher);
        });
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCode, tvValue;
        RadioButton rbSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvVoucherTitle);
            tvCode = itemView.findViewById(R.id.tvVoucherCode);
            tvValue = itemView.findViewById(R.id.tvVoucherValue);
            rbSelect = itemView.findViewById(R.id.rbSelectVoucher);
        }
    }
}
