package com.zendo.apps;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.zendo.apps.databinding.ActivityCartBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartItemChangeListener {

    private ActivityCartBinding binding;
    private CartAdapter adapter;
    private List<CartItem> cartItemList = new ArrayList<>();
    private FirebaseFirestore db;
    private String userEmail;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
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

        db.collection("cart")
                .whereEqualTo("userEmail", userEmail)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        List<CartItem> newList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            CartItem item = doc.toObject(CartItem.class);
                            item.setId(doc.getId());
                            for(CartItem oldItem : cartItemList) {
                                if(oldItem.getId().equals(item.getId())) {
                                    item.setSelected(oldItem.isSelected());
                                    break;
                                }
                            }
                            newList.add(item);
                        }
                        cartItemList.clear();
                        cartItemList.addAll(newList);
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
        db.collection("cart").document(item.getId())
                .update("quantity", newQuantity);
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
        List<CartItem> selectedItems = new ArrayList<>();
        for (CartItem item : cartItemList) {
            if (item.isSelected()) selectedItems.add(item);
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn sản phẩm để xóa", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa " + selectedItems.size() + " sản phẩm đã chọn?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    WriteBatch batch = db.batch();
                    for (CartItem item : selectedItems) {
                        batch.delete(db.collection("cart").document(item.getId()));
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(CartActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
