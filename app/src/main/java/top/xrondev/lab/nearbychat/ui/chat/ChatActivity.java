package top.xrondev.lab.nearbychat.ui.chat;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

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
import top.xrondev.lab.nearbychat.ui.main.MainActivity;
import top.xrondev.lab.nearbychat.utils.NearbyConnectionHelper;

public class ChatActivity extends AppCompatActivity {
    private final List<Message> messages = new ArrayList<>();
    private final Handler handler = new Handler(); // To handle the recording duration, delay it if needed
    private RecyclerView chatRecyclerView;
    private MessageAdapter messageAdapter;
    private NearbyConnectionHelper connectionHelper;
    private EditText inputMessage;
    private Button sendButton;
    private Dialog recordingDialog;
    private ImageButton btnBack;
    private String endpointId;
    private ActivityResultLauncher<PickVisualMediaRequest> mediaResultLauncher;
    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private long startTime; // To calculate the recording duration

    public static void startActivity(Context context, String channelName) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("Channel_Name", channelName);
        context.startActivity(intent);
    }

    @SuppressLint("ClickableViewAccessibility")
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

        // ON RECEIVE:
        connectionHelper.setPayloadCallback(new NearbyConnectionHelper.customPayloadCallback() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                // Payload received
                Message message = null;

                switch (payload.getType()) {
                    case Payload.Type.BYTES:
                        // Handling byte payload
                        if (connectionHelper.isFilenameMessage(payload) == null) {
                            message = new Message(endpointId, payload, MessageType.TEXT);
                        } else {
                            // Handling filename message
                            Log.d("ChatActivity", "Filename message: " + connectionHelper.isFilenameMessage(payload));
                        }
                        break;
                    case Payload.Type.FILE:
                        // Handling file payload - adjust MessageType accordingly
                        String type = Objects.requireNonNull(payload.asFile()).asUri().toString();
                        if (type.endsWith(String.valueOf(MessageType.IMAGE))) {
                            message = new Message(endpointId, payload, MessageType.IMAGE);
                        } else if (type.endsWith(String.valueOf(MessageType.AUDIO))) {
                            message = new Message(endpointId, payload, MessageType.AUDIO);
                        } else {
                            message = new Message(endpointId, payload, MessageType.FILE);
                        }
                        break;
                    // TODO: STREAM AND UNKNOWN
                }

                // Update UI or handle the message appropriately
                if (message != null) {
                    // Assuming `messageAdapter` is a list adapter for handling messages
                    messageAdapter.addMessage(message);
                    messageAdapter.notifyDataSetChanged();
                    int targetPosition = messageAdapter.getItemCount() - 1;

                    chatRecyclerView.smoothScrollToPosition(targetPosition);
                }
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                // Transfer status update, like progress
                Log.d("ChatActivity", "Payload transfer update: " + " To: " + " " + endpointId + " " + update.getStatus() + " " + update.getBytesTransferred() + " " + update.getTotalBytes() + " " + update.getPayloadId());
            }

        });
        // Send button click listener
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            Intent returnIntent = new Intent(ChatActivity.this, MainActivity.class);

            setResult(RESULT_OK, returnIntent);
            finish();
        });


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
                int targetPosition = messageAdapter.getItemCount() - 1;
                chatRecyclerView.smoothScrollToPosition(targetPosition);
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
        chatRecyclerView.setOnTouchListener((v, event) -> {
            if (menuLayout.getVisibility() == View.VISIBLE) {
                menuLayout.setVisibility(View.GONE);
            }
            return false;
        });


        mediaResultLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            // Handle the URI result here
            Log.i("ChatActivity", "Media URI: " + uri);
            if (uri != null) {
                try {
                    File file = uriToFile(uri, uri.getLastPathSegment());
                    Payload filePayload = Payload.fromFile(file);
                    filePayload.setFileName(uri.getLastPathSegment() + MessageType.IMAGE);

                    Message message = new Message("me", filePayload, MessageType.IMAGE);
                    Log.d("ChatActivity", "Sending file: " + filePayload.asFile().getSize() + filePayload.asFile());
                    connectionHelper.sendPayload(endpointId, filePayload);
                    messageAdapter.addMessage(message);
                    int targetPosition = messageAdapter.getItemCount() - 1;
                    chatRecyclerView.smoothScrollToPosition(targetPosition);

                } catch (FileNotFoundException e) {
                    Toast.makeText(ChatActivity.this, "File not found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button btnMedias = findViewById(R.id.btnMedias);
        btnMedias.setOnClickListener(v -> selectMedia());


        Button btnAudio = findViewById(R.id.btnAudio);
        btnAudio.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Start recording
                    startTime = SystemClock.elapsedRealtime();
                    startRecording();
                    return true;  // Consume the event to handle the touch

                case MotionEvent.ACTION_UP:
                    // Stop recording
                    long endTime = SystemClock.elapsedRealtime();
                    if (endTime - startTime < 1000) {
                        // Delay the stopRecording call to ensure at least 1 second of recording
                        handler.postDelayed(() -> {
                            stopRecording();
                            try {
                                sendAudioMessage();
                            } catch (FileNotFoundException e) {
                                // Handle the FileNotFoundException here
                            }
                        }, 1000 - (endTime - startTime));
                    } else {
                        stopRecording();
                        try {
                            sendAudioMessage();
                        } catch (FileNotFoundException e) {
                            // Handle the FileNotFoundException here
                        }
                    }
                    v.performClick();  // Perform the click action for accessibility
                    return true;  // Consume the event to handle the touch
            }
            return false;
        });

    }


    private void selectMedia() {
        mediaResultLauncher.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }

    public File uriToFile(Uri contentUri, String fileName) {
        Context context = this;
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

    // Audio
    private void startRecording() {
        // Create a new MediaRecorder instance
        mediaRecorder = new MediaRecorder();

        // Set the audio source and output format
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        // Create a temporary audio file to store the recorded audio
        audioFilePath = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/" + System.nanoTime() + "_temp_audio.3gp";
        mediaRecorder.setOutputFile(audioFilePath);

        // Set the audio encoder
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        // Start recording
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            showRecordingDialog();
        } catch (IOException e) {
            dismissRecordingDialog();
            Log.e("NBC MediaRecorder", "MediaRecorder prepare() failed");
        }
    }

    private void stopRecording() {
        if (isRecording) {
            // Stop recording
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            dismissRecordingDialog();
        }
    }

    private void showRecordingDialog() {
        if (recordingDialog == null) {
            recordingDialog = new Dialog(this);
            recordingDialog.setContentView(R.layout.audio_recording_dialog); // Use if custom layout
            recordingDialog.setCancelable(false);
            recordingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        recordingDialog.show();
    }

    private void dismissRecordingDialog() {
        if (recordingDialog != null && recordingDialog.isShowing()) {
            recordingDialog.dismiss();
        }
    }

    private void sendAudioMessage() throws FileNotFoundException {
        if (audioFilePath != null) {
            // Create a payload from the recorded audio file
            File audioFile = new File(audioFilePath);
            if (audioFile.exists()) {
                Payload audioPayload = Payload.fromFile(audioFile);
                audioPayload.setFileName(audioFile.getName() + MessageType.AUDIO);

                // Send the audio payload to the remote endpoint
                connectionHelper.sendPayload(endpointId, audioPayload);

                // Add the audio message to the UI
                Message message = new Message("me", audioPayload, MessageType.AUDIO);
                messageAdapter.addMessage(message);
                int targetPosition = messageAdapter.getItemCount() - 1;
                chatRecyclerView.smoothScrollToPosition(targetPosition);
                messageAdapter.notifyDataSetChanged();
            }
        }
    }

}

