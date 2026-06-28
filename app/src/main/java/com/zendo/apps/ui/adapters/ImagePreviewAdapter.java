package com.zendo.apps.ui.adapters;

import com.zendo.apps.R;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.zendo.apps.databinding.ItemImagePreviewBinding;
import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {
    private List<?> imageList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(int position);
    }

    public ImagePreviewAdapter(List<?> imageList, OnItemClickListener listener) {
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemImagePreviewBinding binding = ItemImagePreviewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object imageData = imageList.get(position);
        
        if (imageData instanceof String) {
            String data = (String) imageData;
            if (data.startsWith("http")) {
                Glide.with(holder.itemView.getContext())
                        .load(data)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .centerCrop()
                        .into(holder.binding.ivPreview);
            } else {
                // Base64 handling
                try {
                    byte[] decodedString = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
                    Glide.with(holder.itemView.getContext())
                            .load(decodedString)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .centerCrop()
                            .into(holder.binding.ivPreview);
                } catch (Exception e) {
                    holder.binding.ivPreview.setImageResource(android.R.drawable.ic_menu_report_image);
                }
            }
        } else {
            // Uri handling for new picks
            Glide.with(holder.itemView.getContext())
                    .load(imageData)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .into(holder.binding.ivPreview);
        }

        holder.binding.btnDeletePreview.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemImagePreviewBinding binding;
        public ViewHolder(@NonNull ItemImagePreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
