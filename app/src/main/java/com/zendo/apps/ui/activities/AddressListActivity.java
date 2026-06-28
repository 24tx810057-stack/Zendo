package com.zendo.apps.ui.activities;

import com.zendo.apps.R;

import com.zendo.apps.utils.SharedPrefManager;

import com.zendo.apps.ui.adapters.AddressAdapter;

import com.zendo.apps.data.models.UserAddress;

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
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_list);

        db = FirebaseFirestore.getInstance();
        userEmail = SharedPrefManager.getInstance(this).getUserEmail();

        initViews();
        setupRecyclerView();
        setupListeners();
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
            Intent intent = new Intent(AddressListActivity.this, AddAddressActivity.class);
            startActivity(intent);
        });
    }

    private void loadAddresses() {
        if (userEmail.isEmpty() || isLoading) return;
        isLoading = true;

        db.collection("addresses")
                .whereEqualTo("userEmail", userEmail)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserAddress> newList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        UserAddress addr = doc.toObject(UserAddress.class);
                        addr.setId(doc.getId());
                        newList.add(addr);
                    }
                    
                    // Ưu tiên đưa địa chỉ Mặc định lên đầu
                    java.util.Collections.sort(newList, (a1, a2) -> {
                        if (a1.isDefault()) return -1;
                        if (a2.isDefault()) return 1;
                        return Long.compare(a2.getCreatedAt(), a1.getCreatedAt());
                    });

                    addressList = newList;
                    adapter.setAddressList(addressList);
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    // Fallback
                    db.collection("addresses")
                            .whereEqualTo("userEmail", userEmail)
                            .get()
                            .addOnSuccessListener(snapshots -> {
                                List<UserAddress> newList = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : snapshots) {
                                    UserAddress addr = doc.toObject(UserAddress.class);
                                    addr.setId(doc.getId());
                                    newList.add(addr);
                                }
                                java.util.Collections.sort(newList, (a1, a2) -> Long.compare(a2.getCreatedAt(), a1.getCreatedAt()));
                                addressList = newList;
                                adapter.setAddressList(addressList);
                                isLoading = false;
                            })
                            .addOnFailureListener(e2 -> {
                                isLoading = false;
                                Toast.makeText(this, "Lỗi tải địa chỉ: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses();
    }
}



