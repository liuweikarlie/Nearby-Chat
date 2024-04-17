package top.xrondev.lab.nearbychat.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.util.ArrayList;
import java.util.List;

import top.xrondev.lab.nearbychat.R;
import top.xrondev.lab.nearbychat.adapter.MessageAdapter;
import top.xrondev.lab.nearbychat.models.Message;
import top.xrondev.lab.nearbychat.models.MessageType;
import top.xrondev.lab.nearbychat.utils.NearbyConnectionHelper;

public class ChatActivity extends AppCompatActivity {
    private final List<Message> messages = new ArrayList<>();
    private RecyclerView chatRecyclerView;
    private MessageAdapter messageAdapter;
    private NearbyConnectionHelper connectionHelper;
    private EditText inputMessage;
    private Button sendButton;
    private String endpointId;

    public static void startActivity(Context context, String channelName) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("Channel_Name", channelName);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // get the channel name
        endpointId = getIntent().getStringExtra("Channel_Name");
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        // message adapter
        messageAdapter = new MessageAdapter(messages);
        chatRecyclerView.setAdapter(messageAdapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Nearby Connection helper class
        connectionHelper = NearbyConnectionHelper.getInstance(this);
        connectionHelper.setPayloadCallback(new NearbyConnectionHelper.customPayloadCallback() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                // Payload received
                Message message = null;

                switch (payload.getType()) {
                    case Payload.Type.BYTES:
                        // Handling byte payload
                        message = new Message(endpointId, payload, MessageType.TEXT);
                        break;
                    case Payload.Type.FILE:
                        // Handling file payload - adjust MessageType accordingly
                        message = new Message(endpointId, payload, MessageType.FILE);
                        break;
                    // TODO: STREAM AND UNKNOWN
                }

                // Update UI or handle the message appropriately
                if (message != null) {
                    // Assuming `messageAdapter` is a list adapter for handling messages
                    messageAdapter.addMessage(message);
                    messageAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                // Transfer status update, like progress
            }

        });
        // Send button click listener
        inputMessage = findViewById(R.id.inputMessage);
        sendButton = findViewById(R.id.btnSend);
        sendButton.setOnClickListener(v -> {
            Log.i("ChatActivity", "Send button clicked"+endpointId+" "+inputMessage.getText().toString());
            String text = inputMessage.getText().toString();
            if (!text.isEmpty()) {
                // Convert string text to bytes
                Payload payload = Payload.fromBytes(text.getBytes());
                connectionHelper.sendPayload(endpointId, payload);

                // Add message to UI
                Message message = new Message("me", payload, MessageType.TEXT);
                messageAdapter.addMessage(message);
                messageAdapter.notifyDataSetChanged();
                inputMessage.setText(""); // Clear the input field
            }
        });

    }
}

