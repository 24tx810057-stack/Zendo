package com.zendo.apps.ui.activities;

import com.zendo.apps.utils.SharedPrefManager;

import com.zendo.apps.ui.adapters.CartAdapter;

import com.zendo.apps.data.models.CartItem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.zendo.apps.data.models.AuthResultState;
import com.zendo.apps.viewmodels.CartViewModel;
import com.zendo.apps.databinding.ActivityCartBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartItemChangeListener {

    private ActivityCartBinding binding;
    private CartAdapter adapter;
    private List<CartItem> cartItemList = new ArrayList<>();
    private CartViewModel viewModel;
    private String userEmail;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        userEmail = prefManager.getUserEmail();

        binding.rvCart.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(cartItemList, this);
        binding.rvCart.setAdapter(adapter);

        binding.btnBackCart.setOnClickListener(v -> finish());
        
        binding.tvEdit.setOnClickListener(v -> toggleEditMode());
        
        binding.cbSelectAll.setOnClickListener(v -> {
            boolean isChecked = binding.cbSelectAll.isChecked();
            for (CartItem item : cartItemList) {
                item.setSelected(isChecked);
            }
            adapter.notifyDataSetChanged();
            updateTotalPrice();
        });

        binding.btnCheckout.setOnClickListener(v -> handleCheckout());
        binding.btnDelete.setOnClickListener(v -> handleDeleteSelected());

        loadCartItems();
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        if (isEditMode) {
            binding.tvEdit.setText("Xong");
            binding.btnCheckout.setVisibility(View.GONE);
            binding.layoutCheckoutInfo.setVisibility(View.GONE);
            binding.btnDelete.setVisibility(View.VISIBLE);
        } else {
            binding.tvEdit.setText("Sửa");
            binding.btnCheckout.setVisibility(View.VISIBLE);
            binding.layoutCheckoutInfo.setVisibility(View.VISIBLE);
            binding.btnDelete.setVisibility(View.GONE);
        }
    }

    private void loadCartItems() {
        if (userEmail.isEmpty()) return;

        viewModel.getCartItems(userEmail).observe(this, items -> {
            if (items != null) {
                // Preserve selection state
                for (CartItem newItem : items) {
                    for (CartItem oldItem : cartItemList) {
                        if (oldItem.getId().equals(newItem.getId())) {
                            newItem.setSelected(oldItem.isSelected());
                            break;
                        }
                    }
                }
                cartItemList.clear();
                cartItemList.addAll(items);
                adapter.notifyDataSetChanged();
                onSelectionChange();

                if (cartItemList.isEmpty()) {
                    binding.tvEmptyCart.setVisibility(View.VISIBLE);
                    binding.tvEdit.setVisibility(View.GONE);
                } else {
                    binding.tvEmptyCart.setVisibility(View.GONE);
                    binding.tvEdit.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onQuantityChange(CartItem item, int newQuantity) {
        viewModel.updateQuantity(item.getId(), newQuantity);
    }

    @Override
    public void onSelectionChange() {
        updateTotalPrice();
        
        boolean allSelected = !cartItemList.isEmpty();
        for (CartItem item : cartItemList) {
            if (!item.isSelected()) {
                allSelected = false;
                break;
            }
        }
        binding.cbSelectAll.setChecked(allSelected);
    }

    private void updateTotalPrice() {
        double total = 0;
        int count = 0;
        for (CartItem item : cartItemList) {
            if (item.isSelected()) {
                total += item.getProductPrice() * item.getQuantity();
                count++;
            }
        }
        binding.tvTotalPrice.setText(formatter.format(total) + "đ");
        binding.btnCheckout.setText("Mua hàng (" + count + ")");
    }

    private void handleCheckout() {
        ArrayList<CartItem> selectedItems = new ArrayList<>();
        for (CartItem item : cartItemList) {
            if (item.isSelected()) selectedItems.add(item);
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("checkout_items", selectedItems);
        startActivity(intent);
    }

    private void handleDeleteSelected() {
        List<String> selectedIds = cartItemList.stream()
                .filter(CartItem::isSelected)
                .map(CartItem::getId)
                .collect(Collectors.toList());

        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn sản phẩm để xóa", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa " + selectedIds.size() + " sản phẩm đã chọn?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deleteItems(selectedIds).observe(this, state -> {
                        if (state.getStatus() == AuthResultState.Status.SUCCESS) {
                            Toast.makeText(CartActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}


