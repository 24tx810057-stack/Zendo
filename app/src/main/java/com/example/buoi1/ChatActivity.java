package com.example.buoi1;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChatMessages;
    private MessageAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private EditText etChatMessage;
    private ImageView btnSendMessage, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rvChatMessages = findViewById(R.id.rvChatMessages);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnBack = findViewById(R.id.btnBackChat);

        adapter = new MessageAdapter(messageList);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        rvChatMessages.setAdapter(adapter);

        // Tin nhắn chào mừng mặc định
        messageList.add(new ChatMessage("Zendo chào bạn, vui lòng nói những gì bạn đang thắc mắc nhé!", "admin", System.currentTimeMillis()));
        adapter.notifyDataSetChanged();

        btnBack.setOnClickListener(v -> finish());

        btnSendMessage.setOnClickListener(v -> {
            String msg = etChatMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                messageList.add(new ChatMessage(msg, "user", System.currentTimeMillis()));
                adapter.notifyItemInserted(messageList.size() - 1);
                rvChatMessages.scrollToPosition(messageList.size() - 1);
                etChatMessage.setText("");
            }
        });
    }
}
