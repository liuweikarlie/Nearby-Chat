package top.xrondev.lab.nearbychat.ui.chat;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import top.xrondev.lab.nearbychat.R;
import top.xrondev.lab.nearbychat.adapter.MessageAdapter;
import top.xrondev.lab.nearbychat.models.Message;
import top.xrondev.lab.nearbychat.models.MessageType;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private MessageAdapter messageAdapter;
    private final List<Message> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        initializeMockMessages(); // Function to initialize mock messages
        messageAdapter = new MessageAdapter(messages);
        chatRecyclerView.setAdapter(messageAdapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initializeMockMessages() {
        messages.add(new Message("me", "Hello, World!", MessageType.TEXT));
        messages.add(new Message("other", "HELLO!", MessageType.TEXT));
        // Add more messages as needed
    }
}

