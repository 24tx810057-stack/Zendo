package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartItemChangeListener {

    private RecyclerView rvCart;
    private CartAdapter adapter;
    private List<CartItem> cartItemList = new ArrayList<>();
    private TextView tvTotalPrice, tvEdit;
    private View tvEmptyCart; // Đổi từ TextView sang View (vì XML mới là LinearLayout)
    private Button btnCheckout, btnDelete;
    private CheckBox cbSelectAll;
    private LinearLayout layoutCheckoutInfo;
    private ImageButton btnBack;
    private FirebaseFirestore db;
    private String userEmail;
    private DecimalFormat formatter = new DecimalFormat("###,###,###");
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");

        rvCart = findViewById(R.id.rvCart);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvEmptyCart = findViewById(R.id.tvEmptyCart);
        tvEdit = findViewById(R.id.tvEdit);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnDelete = findViewById(R.id.btnDelete);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        layoutCheckoutInfo = findViewById(R.id.layoutCheckoutInfo);
        btnBack = findViewById(R.id.btnBackCart);

        rvCart.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(cartItemList, this);
        rvCart.setAdapter(adapter);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        
        if (tvEdit != null) tvEdit.setOnClickListener(v -> toggleEditMode());
        
        if (cbSelectAll != null) {
            cbSelectAll.setOnClickListener(v -> {
                boolean isChecked = cbSelectAll.isChecked();
                for (CartItem item : cartItemList) {
                    item.setSelected(isChecked);
                }
                adapter.notifyDataSetChanged();
                updateTotalPrice();
            });
        }

        if (btnCheckout != null) btnCheckout.setOnClickListener(v -> handleCheckout());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> handleDeleteSelected());

        loadCartItems();
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        if (isEditMode) {
            if (tvEdit != null) tvEdit.setText("Xong");
            if (btnCheckout != null) btnCheckout.setVisibility(View.GONE);
            if (layoutCheckoutInfo != null) layoutCheckoutInfo.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
        } else {
            if (tvEdit != null) tvEdit.setText("Sửa");
            if (btnCheckout != null) btnCheckout.setVisibility(View.VISIBLE);
            if (layoutCheckoutInfo != null) layoutCheckoutInfo.setVisibility(View.VISIBLE);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
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
                            if (tvEmptyCart != null) tvEmptyCart.setVisibility(View.VISIBLE);
                            if (tvEdit != null) tvEdit.setVisibility(View.GONE);
                        } else {
                            if (tvEmptyCart != null) tvEmptyCart.setVisibility(View.GONE);
                            if (tvEdit != null) tvEdit.setVisibility(View.VISIBLE);
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
        if (cbSelectAll != null) cbSelectAll.setChecked(allSelected);
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
        if (tvTotalPrice != null) tvTotalPrice.setText(formatter.format(total) + "đ");
        if (btnCheckout != null) btnCheckout.setText("Mua hàng (" + count + ")");
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
