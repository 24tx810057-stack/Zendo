package com.zendo.apps.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zendo.apps.R;

import java.util.List;

public class AttributeAdapter extends RecyclerView.Adapter<AttributeAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDelete(Object item);
    }

    private List<?> items;
    private OnDeleteClickListener listener;

    public AttributeAdapter(List<?> items, OnDeleteClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateList(List<?> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attribute, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = items.get(position);
        if (item instanceof com.zendo.apps.data.models.Category) {
            holder.tvName.setText(((com.zendo.apps.data.models.Category) item).getName());
        } else if (item instanceof com.zendo.apps.data.models.Brand) {
            holder.tvName.setText(((com.zendo.apps.data.models.Brand) item).getName());
        }

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAttributeName);
            btnDelete = itemView.findViewById(R.id.btnDeleteAttribute);
        }
    }
}
