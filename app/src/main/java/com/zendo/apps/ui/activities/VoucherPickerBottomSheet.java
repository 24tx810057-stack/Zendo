package com.zendo.apps.ui.activities;

import com.zendo.apps.ui.adapters.VoucherSelectionAdapter;

import com.zendo.apps.data.models.Voucher;

import com.zendo.apps.data.models.Order;

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
import com.zendo.apps.databinding.BottomSheetVoucherPickerBinding;
import java.util.ArrayList;
import java.util.List;

public class VoucherPickerBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetVoucherPickerBinding binding;
    private FirebaseFirestore db;
    private double subtotal;
    private List<Voucher> voucherList = new ArrayList<>();
    private VoucherSelectionAdapter adapter;
    private Voucher selectedVoucher;
    private OnVoucherSelectedListener listener;

    public interface OnVoucherSelectedListener {
        void onVoucherSelected(Voucher voucher);
    }

    public static VoucherPickerBottomSheet newInstance(double subtotal) {
        VoucherPickerBottomSheet fragment = new VoucherPickerBottomSheet();
        Bundle args = new Bundle();
        args.putDouble("subtotal", subtotal);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnVoucherSelectedListener(OnVoucherSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetVoucherPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            subtotal = getArguments().getDouble("subtotal");
        }

        // MẶC ĐỊNH: Chỉ hiện ProgressBar, ẩn tất cả các thành phần khác
        binding.pbLoadingVoucher.setVisibility(View.VISIBLE);
        binding.rvVoucherList.setVisibility(View.INVISIBLE);
        binding.llEmptyVoucher.setVisibility(View.GONE);
        binding.btnConfirmVoucher.setVisibility(View.GONE);

        setupRecyclerView();
        loadVouchers();

        binding.btnConfirmVoucher.setOnClickListener(v -> {
            if (selectedVoucher != null && listener != null) {
                listener.onVoucherSelected(selectedVoucher);
                dismiss();
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn voucher", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new VoucherSelectionAdapter(voucherList, voucher -> selectedVoucher = voucher);
        binding.rvVoucherList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvVoucherList.setAdapter(adapter);
    }

    private void loadVouchers() {
        long currentTime = System.currentTimeMillis();
        db.collection("vouchers").whereEqualTo("active", true).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Tránh crash nếu fragment đã thoát
                    if (!isAdded() || binding == null) return;
                    
                    voucherList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Voucher v = doc.toObject(Voucher.class);
                            if (v == null) continue;
                            v.setId(doc.getId());
                            // Check expiry and min order
                            if ((v.getExpiryDate() == 0 || v.getExpiryDate() > currentTime) && (subtotal >= v.getMinOrder())) {
                                voucherList.add(v);
                            }
                        } catch (Exception e) {}
                    }
                    adapter.notifyDataSetChanged();
                    
                    binding.pbLoadingVoucher.setVisibility(View.GONE);
                    
                    if (voucherList.isEmpty()) {
                        binding.rvVoucherList.setVisibility(View.GONE);
                        binding.llEmptyVoucher.setVisibility(View.VISIBLE);
                        binding.btnConfirmVoucher.setVisibility(View.GONE);
                    } else {
                        binding.rvVoucherList.setVisibility(View.VISIBLE);
                        binding.llEmptyVoucher.setVisibility(View.GONE);
                        binding.btnConfirmVoucher.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    binding.pbLoadingVoucher.setVisibility(View.GONE);
                    binding.llEmptyVoucher.setVisibility(View.VISIBLE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


