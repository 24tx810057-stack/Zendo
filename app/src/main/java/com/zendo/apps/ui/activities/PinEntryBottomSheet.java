package com.zendo.apps.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.zendo.apps.databinding.BottomSheetPinEntryBinding;

public class PinEntryBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetPinEntryBinding binding;
    private OnPinVerifiedListener listener;
    // For demo/test purpose on emulator, PIN is hardcoded to 123456
    private static final String DEFAULT_PIN = "123456";

    public interface OnPinVerifiedListener {
        void onPinVerified();
    }

    public void setOnPinVerifiedListener(OnPinVerifiedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetPinEntryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnConfirmPin.setOnClickListener(v -> {
            String pin = binding.etPin.getText().toString().trim();
            if (pin.equals(DEFAULT_PIN)) {
                if (listener != null) {
                    listener.onPinVerified();
                    dismiss();
                }
            } else {
                Toast.makeText(getContext(), "Mã PIN không chính xác (Thử 123456)", Toast.LENGTH_SHORT).show();
                binding.etPin.setText("");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
