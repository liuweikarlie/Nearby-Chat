package top.xrondev.lab.nearbychat.ui.chat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    private ActivityResultLauncher<PickVisualMediaRequest> mediaResultLauncher;

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
                        // TODO: distinguish between image, video, audio, and other file types
                        message = new Message(endpointId, payload, MessageType.IMAGE, "image.jpg");
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
                Log.d("ChatActivity", "Payload transfer update: " + update.getStatus() + " " + update.getBytesTransferred() + " " + update.getTotalBytes() + " " + update.getPayloadId());
            }

        });
        // Send button click listener
        inputMessage = findViewById(R.id.inputMessage);
        sendButton = findViewById(R.id.btnSend);
        sendButton.setOnClickListener(v -> {
            Log.i("ChatActivity", "Send button clicked" + endpointId + " " + inputMessage.getText().toString());
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


        // Plus button click listener
        ImageButton plusButton = findViewById(R.id.btnPlus);
        LinearLayout menuLayout = findViewById(R.id.menuLayout);
        plusButton.setOnClickListener(v -> {
            if (menuLayout.getVisibility() == View.GONE) {
                menuLayout.setVisibility(View.VISIBLE);
            } else {
                menuLayout.setVisibility(View.GONE);
            }
        });

        // close menu layout when clicked outside
        // TODO: not working
        chatRecyclerView.setOnClickListener(v -> {
            if (menuLayout.getVisibility() == View.VISIBLE) {
                menuLayout.setVisibility(View.GONE);
            }
        });


        mediaResultLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    // Handle the URI result here (e.g., display or upload it)
                    Log.i("ChatActivity", "Media URI: " + uri);
                    if (uri != null) {
                        try {
                            File file = uriToFile(this, uri, "image.jpg");
                            Payload filePayload = Payload.fromFile(file);
                            Message message = new Message("me", filePayload, MessageType.IMAGE);
                            Log.d("ChatActivity", "Sending file: " +filePayload.asFile().getSize() + filePayload.asFile());
                            connectionHelper.sendPayload(endpointId, message.getContent());
                            messageAdapter.addMessage(message);
                        } catch (FileNotFoundException e) {
                            Toast.makeText(ChatActivity.this, "File not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        Button btnMedias = findViewById(R.id.btnMedias);
        btnMedias.setOnClickListener(v -> selectMedia());

    }

    private void selectMedia() {
        mediaResultLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    public File uriToFile(Context context, Uri contentUri, String fileName) {
        context = this;
        File file = null;
        ContentResolver resolver = context.getContentResolver();

        // Define an input stream to read data from the URI
        try (InputStream inputStream = resolver.openInputStream(contentUri)) {
            // Create a new file in the app's cache directory
            file = new File(context.getCacheDir(), fileName);

            // Define an output stream to write data into the file
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                if (inputStream != null) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file;
    }

}

