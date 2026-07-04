package com.zendo.apps.ui.activities;

import com.zendo.apps.ui.adapters.MessageAdapter;

import com.zendo.apps.data.models.ChatMessage;

import com.zendo.apps.data.models.User;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.zendo.apps.databinding.ActivityChatBinding;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.android.material.chip.Chip;
import com.zendo.apps.utils.AutoResponseEngine;
import com.zendo.apps.utils.SharedPrefManager;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private MessageAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private String shopId, shopName;
    private boolean isBot;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        shopId = getIntent().getStringExtra("shop_id");
        shopName = getIntent().getStringExtra("shop_name");
        isBot = getIntent().getBooleanExtra("is_bot", false);
        userEmail = SharedPrefManager.getInstance(this).getUserEmail();

        adapter = new MessageAdapter(messageList);
        binding.rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChatMessages.setAdapter(adapter);

        if (shopName != null && !shopName.isEmpty()) {
            binding.tvChatTitle.setText(shopName + (isBot ? " (Bot)" : ""));
        }

        setupQuickReplies();

        // Tin nhắn chào mừng mặc định
        String welcomeMsg = (shopName != null ? shopName : "Shop") + 
                (isBot ? " Bot chào bạn! Mình có thể giúp gì cho bạn?" : " chào bạn, chúng tôi có thể giúp gì cho bạn?");
        
        addMessage(new ChatMessage(welcomeMsg, "admin", System.currentTimeMillis(), true));

        binding.btnBackChat.setOnClickListener(v -> finish());

        binding.btnSendMessage.setOnClickListener(v -> {
            String msg = binding.etChatMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                handleUserMessage(msg);
            }
        });
    }

    private void setupQuickReplies() {
        String[] queries = AutoResponseEngine.getQuickReplies();
        for (String query : queries) {
            Chip chip = new Chip(this);
            chip.setText(query);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColorResource(android.R.color.darker_gray);
            chip.setOnClickListener(v -> handleUserMessage(query));
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            binding.rvQuickReplies.addView(chip);
        }
    }

    private void handleUserMessage(String msg) {
        ChatMessage userMsg = new ChatMessage(msg, "user", System.currentTimeMillis());
        addMessage(userMsg);
        binding.etChatMessage.setText("");

        // Giả lập trả lời tự động sau 1 giây
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
}


