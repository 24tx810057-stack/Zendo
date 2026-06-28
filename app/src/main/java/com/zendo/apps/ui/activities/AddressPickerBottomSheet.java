package com.zendo.apps.ui.activities;

import com.zendo.apps.utils.SharedPrefManager;

import com.zendo.apps.ui.adapters.AddressAdapter;

import com.zendo.apps.data.models.UserAddress;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zendo.apps.databinding.BottomSheetAddressPickerBinding;
import java.util.ArrayList;
import java.util.List;

public class AddressPickerBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetAddressPickerBinding binding;
    private FirebaseFirestore db;
    private String userEmail;
    private List<UserAddress> addressList = new ArrayList<>();
    private AddressAdapter adapter;
    private UserAddress selectedAddress;
    private OnAddressSelectedListener listener;

    public interface OnAddressSelectedListener {
        void onAddressSelected(UserAddress address);
    }

    public static AddressPickerBottomSheet newInstance(String userEmail) {
        AddressPickerBottomSheet fragment = new AddressPickerBottomSheet();
        Bundle args = new Bundle();
        args.putString("user_email", userEmail);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnAddressSelectedListener(OnAddressSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAddressPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = FirebaseFirestore.getInstance();
        
        // CỐ ĐỊNH: Đảm bảo lấy email từ SharedPrefs nếu Arguments bị mất
        if (getArguments() != null) {
            userEmail = getArguments().getString("user_email");
        }
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = SharedPrefManager.getInstance(requireContext()).getUserEmail();
        }

        setupRecyclerView();
        
        // Show loading state initially
        binding.rvAddressList.setVisibility(View.INVISIBLE);
        binding.btnConfirmAddress.setEnabled(false);

        loadAddresses();

        binding.btnAddNewAddress.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddAddressActivity.class);
            startActivity(intent);
        });

        binding.btnConfirmAddress.setOnClickListener(v -> {
            if (selectedAddress != null && listener != null) {
                listener.onAddressSelected(selectedAddress);
                dismiss();
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn địa chỉ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAddresses();
    }

    private void setupRecyclerView() {
        adapter = new AddressAdapter(addressList, new AddressAdapter.OnAddressClickListener() {
            @Override
            public void onEditClick(UserAddress address) {
                Intent intent = new Intent(getContext(), AddAddressActivity.class);
                intent.putExtra("edit_address", address);
                startActivity(intent);
            }

            @Override
            public void onAddressSelected(UserAddress address) {
                selectedAddress = address;
            }
        });
        binding.rvAddressList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAddressList.setAdapter(adapter);
    }

    private void loadAddresses() {
        if (userEmail == null || userEmail.isEmpty()) return;

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
                    
                    // Sort locally to ensure consistency without complex indexes
                    java.util.Collections.sort(addressList, (a1, a2) -> {
                        // Default addresses first
                        if (a1.isDefault() && !a2.isDefault()) return -1;
                        if (!a1.isDefault() && a2.isDefault()) return 1;
                        // Then by creation date descending
                        return Long.compare(a2.getCreatedAt(), a1.getCreatedAt());
                    });

                    if (!addressList.isEmpty()) {
                        // Find default to select, or just pick first
                        selectedAddress = null;
                        for (UserAddress a : addressList) {
                            if (a.isDefault()) {
                                selectedAddress = a;
                                break;
                            }
                        }
                        if (selectedAddress == null) selectedAddress = addressList.get(0);
                    }

                    adapter.setAddressList(addressList);
                    binding.rvAddressList.setVisibility(View.VISIBLE);
                    binding.btnConfirmAddress.setEnabled(true);
                    
                    if (addressList.isEmpty()) {
                        Toast.makeText(getContext(), "Bạn chưa có địa chỉ nào. Vui lòng thêm mới.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.btnConfirmAddress.setEnabled(true);
                    Toast.makeText(getContext(), "Lỗi tải địa chỉ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


