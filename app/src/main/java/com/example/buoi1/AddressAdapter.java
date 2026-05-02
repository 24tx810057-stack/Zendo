package com.example.buoi1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {
    private List<UserAddress> addressList;
    private OnAddressClickListener listener;
    private int selectedPosition = -1;

    public interface OnAddressClickListener {
        void onEditClick(UserAddress address);
        void onAddressSelected(UserAddress address);
    }

    public AddressAdapter(List<UserAddress> addressList, OnAddressClickListener listener) {
        this.addressList = addressList;
        this.listener = listener;
        updateSelectedPosition();
    }

    public void setAddressList(List<UserAddress> newList) {
        this.addressList = newList;
        updateSelectedPosition();
        notifyDataSetChanged();
    }

    private void updateSelectedPosition() {
        if (addressList == null || addressList.isEmpty()) {
            selectedPosition = -1;
            return;
        }

        if (addressList.size() == 1) {
            selectedPosition = 0;
            return;
        }

        // Nếu có nhiều hơn 1 địa chỉ, tìm địa chỉ mặc định
        selectedPosition = -1;
        for (int i = 0; i < addressList.size(); i++) {
            if (addressList.get(i).isDefault()) {
                selectedPosition = i;
                break;
            }
        }
        
        // Nếu không có địa chỉ nào là mặc định, mặc định chọn cái đầu tiên
        if (selectedPosition == -1 && !addressList.isEmpty()) {
            selectedPosition = 0;
        }
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        UserAddress address = addressList.get(position);
        holder.tvName.setText(address.getFullName());
        holder.tvPhone.setText(address.getPhone());
        holder.tvDetail.setText(address.getDetailAddress());
        holder.tvArea.setText(address.getWard() + ", " + address.getDistrict() + ", " + address.getProvinceCity());
        
        // Chỉ hiển thị nhãn Mặc định
        holder.tvDefaultBadge.setVisibility(address.isDefault() ? View.VISIBLE : View.GONE);
        if (holder.tvPickupBadge != null) {
            holder.tvPickupBadge.setVisibility(View.GONE);
        }

        holder.rbSelect.setChecked(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (oldPos != selectedPosition) {
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
            }
            listener.onAddressSelected(address);
        });

        holder.tvEdit.setOnClickListener(v -> listener.onEditClick(address));
    }

    @Override
    public int getItemCount() {
        return addressList != null ? addressList.size() : 0;
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvDetail, tvArea, tvDefaultBadge, tvPickupBadge, tvEdit;
        RadioButton rbSelect;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvPhone = itemView.findViewById(R.id.tvItemPhone);
            tvDetail = itemView.findViewById(R.id.tvItemDetail);
            tvArea = itemView.findViewById(R.id.tvItemArea);
            tvDefaultBadge = itemView.findViewById(R.id.tvDefaultBadge);
            tvPickupBadge = itemView.findViewById(R.id.tvPickupBadge);
            tvEdit = itemView.findViewById(R.id.tvEditAddress);
            rbSelect = itemView.findViewById(R.id.rbSelectAddress);
        }
    }
}
