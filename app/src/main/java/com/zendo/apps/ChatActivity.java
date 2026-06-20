package com.zendo.apps;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.zendo.apps.databinding.ActivityChatBinding;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private MessageAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new MessageAdapter(messageList);
        binding.rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChatMessages.setAdapter(adapter);

        // Tin nhắn chào mừng mặc định
        messageList.add(new ChatMessage("Zendo chào bạn, vui lòng nói những gì bạn đang thắc mắc nhé!", "admin", System.currentTimeMillis()));
        adapter.notifyDataSetChanged();

        binding.btnBackChat.setOnClickListener(v -> finish());

        binding.btnSendMessage.setOnClickListener(v -> {
            String msg = binding.etChatMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                messageList.add(new ChatMessage(msg, "user", System.currentTimeMillis()));
                adapter.notifyItemInserted(messageList.size() - 1);
                binding.rvChatMessages.scrollToPosition(messageList.size() - 1);
                binding.etChatMessage.setText("");
            }
        });
    }
}
