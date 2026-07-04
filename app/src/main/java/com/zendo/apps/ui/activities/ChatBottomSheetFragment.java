package com.zendo.apps.ui.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.zendo.apps.data.models.ChatMessage;
import com.zendo.apps.data.models.User;
import com.zendo.apps.databinding.BottomSheetChatBinding;
import com.zendo.apps.ui.adapters.MessageAdapter;
import com.zendo.apps.utils.AutoResponseEngine;
import com.zendo.apps.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;

public class ChatBottomSheetFragment extends BottomSheetDialogFragment {

    private BottomSheetChatBinding binding;
    private MessageAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private String shopId, shopName;
    private boolean isBot;
    private String userEmail, fullName;

    public static ChatBottomSheetFragment newInstance(String shopId, String shopName, boolean isBot) {
        ChatBottomSheetFragment fragment = new ChatBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("shop_id", shopId);
        args.putString("shop_name", shopName);
        args.putBoolean("is_bot", isBot);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            shopId = getArguments().getString("shop_id");
            shopName = getArguments().getString("shop_name");
            isBot = getArguments().getBoolean("is_bot");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Thiết lập chiều cao khoảng 2/3 màn hình
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.7);
                behavior.setPeekHeight(height);
                bottomSheet.getLayoutParams().height = height;
            }
        }

        userEmail = SharedPrefManager.getInstance(requireContext()).getUserEmail();
        
        // Lấy thông tin user để chào cá nhân hóa
        new com.zendo.apps.data.repositories.UserRepository().getUser(userEmail).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                fullName = user.getFullName();
            }
            if (messageList.isEmpty()) {
                sendWelcomeMessage();
            }
        });

        adapter = new MessageAdapter(messageList);
        binding.rvChatMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChatMessages.setAdapter(adapter);

        binding.tvChatTitle.setText(shopName != null ? shopName : "Zendo Store");
        if (isBot) binding.tvChatTitle.append(" (Bot)");

        setupQuickReplies();

        binding.btnCloseChat.setOnClickListener(v -> dismiss());

        binding.btnSendMessage.setOnClickListener(v -> {
            String msg = binding.etChatMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                handleUserMessage(msg);
            }
        });
    }

    private void sendWelcomeMessage() {
        String userName = (fullName != null && !fullName.isEmpty()) ? fullName : "bạn";
        String welcomeMsg = "Chào " + userName + "! " + (shopName != null ? shopName : "Shop") + 
                (isBot ? " Bot chào bạn! Mình có thể giúp gì cho bạn?" : " chào bạn, chúng tôi có thể giúp gì cho bạn?");
        
        addMessage(new ChatMessage(welcomeMsg, "admin", System.currentTimeMillis(), true));
    }

    private void setupQuickReplies() {
        String[] queries = AutoResponseEngine.getQuickReplies();
        binding.layoutQuickReplies.removeAllViews(); 
        
        for (String query : queries) {
            Chip chip = new Chip(requireContext());
            chip.setText(query);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeWidth(2f);
            chip.setChipStrokeColorResource(android.R.color.darker_gray);
            chip.setOnClickListener(v -> handleUserMessage(query));
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            binding.layoutQuickReplies.addView(chip);
        }
    }

    private void handleUserMessage(String msg) {
        ChatMessage userMsg = new ChatMessage(msg, "user", System.currentTimeMillis());
        addMessage(userMsg);
        binding.etChatMessage.setText("");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String response = AutoResponseEngine.getResponse(msg);
            if (response != null) {
                addMessage(new ChatMessage(response, "admin", System.currentTimeMillis(), true));
            }
        }, 1000);
    }

    private void addMessage(ChatMessage message) {
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        binding.rvChatMessages.scrollToPosition(messageList.size() - 1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
