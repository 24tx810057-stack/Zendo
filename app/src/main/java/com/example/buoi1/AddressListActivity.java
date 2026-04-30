package com.example.buoi1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AddressListActivity extends AppCompatActivity {

    private RecyclerView rvAddressList;
    private AddressAdapter adapter;
    private List<UserAddress> addressList;
    private ImageView btnBack;
    private LinearLayout btnAddAddress;
    private FirebaseFirestore db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_list);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userEmail = sharedPref.getString("user_email", "");

        initViews();
        setupRecyclerView();
        setupListeners();
        loadAddresses();
    }

    private void initViews() {
        rvAddressList = findViewById(R.id.rvAddressList);
        btnBack = findViewById(R.id.btnBackAddressList);
        btnAddAddress = findViewById(R.id.btnAddAddress);
    }

    private void setupRecyclerView() {
        addressList = new ArrayList<>();
        adapter = new AddressAdapter(addressList, new AddressAdapter.OnAddressClickListener() {
            @Override
            public void onEditClick(UserAddress address) {
                // KHI BẤM SỬA: Gửi đối tượng address sang trang AddAddress
                Intent intent = new Intent(AddressListActivity.this, AddAddressActivity.class);
                intent.putExtra("edit_address", address);
                startActivity(intent);
            }

            @Override
            public void onAddressSelected(UserAddress address) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_address", address);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
        rvAddressList.setLayoutManager(new LinearLayoutManager(this));
        rvAddressList.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnAddAddress.setOnClickListener(v -> {
            // KHI BẤM THÊM MỚI: Không gửi gì cả, AddAddressActivity sẽ tự để trắng các ô
            Intent intent = new Intent(AddressListActivity.this, AddAddressActivity.class);
            startActivity(intent);
        });
    }

    private void loadAddresses() {
        if (userEmail.isEmpty()) return;

        // 1. Lấy danh sách từ collection "addresses" của user này
        db.collection("addresses")
                .whereEqualTo("userEmail", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    addressList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        UserAddress addr = doc.toObject(UserAddress.class);
                        addr.setId(doc.getId());
                        addressList.add(addr);
                    }
                    
                    // Nếu danh sách trống, có thể lấy từ thông tin User mặc định
                    if (addressList.isEmpty()) {
                        loadDefaultUserAddress();
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void loadDefaultUserAddress() {
        db.collection("users").whereEqualTo("email", userEmail).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        if (user != null && user.getAddress() != null && !user.getAddress().isEmpty()) {
                            UserAddress defaultAddr = new UserAddress();
                            defaultAddr.setFullName(user.getFullName());
                            defaultAddr.setPhone(user.getPhone());
                            defaultAddr.setDetailAddress(user.getAddress());
                            defaultAddr.setWard("Chưa cập nhật");
                            defaultAddr.setDistrict("");
                            defaultAddr.setProvinceCity("");
                            defaultAddr.setDefault(true);
                            defaultAddr.setType("Nhà riêng");
                            
                            addressList.add(defaultAddr);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses(); // Làm mới danh sách khi quay lại từ trang thêm/sửa
    }
}
