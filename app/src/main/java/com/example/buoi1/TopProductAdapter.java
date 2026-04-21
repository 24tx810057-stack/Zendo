package com.example.buoi1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.text.DecimalFormat;
import java.util.List;

public class TopProductAdapter extends BaseAdapter {
    private Context context;
    private List<Product> productList;
    private double totalRevenue;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");

    public TopProductAdapter(Context context, List<Product> productList, double totalRevenue) {
        this.context = context;
        this.productList = productList;
        this.totalRevenue = totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    @Override
    public int getCount() { return productList.size(); }
    @Override
    public Object getItem(int position) { return productList.get(position); }
    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_top_product, parent, false);
        }

        Product product = productList.get(position);

        TextView tvRank = convertView.findViewById(R.id.tvRank);
        ImageView ivProductImage = convertView.findViewById(R.id.ivProductTop);
        TextView tvProductName = convertView.findViewById(R.id.tvProductNameTop);
        TextView tvSold = convertView.findViewById(R.id.tvSoldTop);
        TextView tvRevenue = convertView.findViewById(R.id.tvRevenueTop);
        TextView tvPercent = convertView.findViewById(R.id.tvPercentTop);
        ProgressBar pbShare = convertView.findViewById(R.id.pbRevenueShare);

        if (tvRank != null) tvRank.setText(String.valueOf(position + 1));
        if (tvProductName != null) tvProductName.setText(product.getName());
        if (tvSold != null) tvSold.setText("Số lượng đã bán: " + product.getSoldCount());
        
        double productRevenue = product.getPrice() * product.getSoldCount();
        if (tvRevenue != null) tvRevenue.setText(formatter.format(productRevenue) + "đ");

        if (totalRevenue > 0) {
            double percentValue = (productRevenue / totalRevenue) * 100;
            int percent = (int) Math.round(percentValue);
            if (tvPercent != null) tvPercent.setText(percent + "%");
            if (pbShare != null) pbShare.setProgress(percent);
        } else {
            if (tvPercent != null) tvPercent.setText("0%");
            if (pbShare != null) pbShare.setProgress(0);
        }

        if (ivProductImage != null) {
            String imgData = product.getImageUrl();
            if (imgData != null && !imgData.isEmpty()) {
                if (imgData.startsWith("http")) {
                    Glide.with(context).load(imgData).into(ivProductImage);
                } else {
                    try {
                        byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivProductImage.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        ivProductImage.setImageResource(R.drawable.ic_launcher_background);
                    }
                }
            } else {
                ivProductImage.setImageResource(R.drawable.ic_launcher_background);
            }
        }

        return convertView;
    }
}
