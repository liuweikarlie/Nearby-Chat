package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
public class MainApplication extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static  String[] REQUIRED_PERMISSIONS ;


//            = {
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.BLUETOOTH_SCAN,
//            Manifest.permission.BLUETOOTH_ADVERTISE,
//            Manifest.permission.BLUETOOTH_CONNECT,
//            Manifest.permission.ACCESS_WIFI_STATE,
//            Manifest.permission.CHANGE_WIFI_STATE,
//            Manifest.permission.NEARBY_WIFI_DEVICES,
//            Manifest.permission.ACCESS_FINE_LOCATION,
//
//    };




    private EditText messageEditText;
    private Button sendButton;
    private Button imageButton;
    private TextView chatTextView;
    private ImageView imageView;

    private ConnectionsClient connectionsClient;
    private String localEndpointName;
    private List<String> discoveredEndpoints;

    private ListView chatListView;
    private ChatsAdapter chatAdapter ;
    private ActivityResultLauncher<PickVisualMediaRequest> attachmentLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        chatAdapter = new ChatsAdapter(this, new ArrayList<ChatMessage>());

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        imageButton = findViewById(R.id.attachButton);
        chatListView = findViewById(R.id.chatListView);
        imageView=findViewById(R.id.imageView);
        List<ChatMessage> chatMessages = new ArrayList<>();


        chatListView.setAdapter(chatAdapter);        //chatRecyclerView = findViewById(R.id.chatRecyclerView);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString();
                Drawable imageDrawable = imageView.getDrawable();


                if (!message.isEmpty() || imageDrawable != null) {
                    try {
                        sendMessage(message,imageDrawable);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    messageEditText.setText("");
                    imageView.setImageDrawable(null);


                }
            }
        });



        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent iGallery= new Intent(Intent.ACTION_PICK);

                openAttachmentPicker();
            }
        });

        attachmentLauncher =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    // Callback is invoked after the user selects a media item or closes the
                    // photo picker.
                    if (uri != null) {
                        Context context = this; // Replace 'this' with your actual context
                        ContentResolver  contentResolver = context.getContentResolver();
//                        Drawable imageDrawable = getDrawableFromUri(uri);
//                        imageDrawable = imageView.getDrawable();
                        ImageDecoder.Source source = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            source = ImageDecoder.createSource(contentResolver, uri);
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            try {
                                Bitmap imageBitmap = ImageDecoder.decodeBitmap(source);
                                imageView.setImageBitmap(imageBitmap);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        Log.d("PhotoPicker", "Selected URI: " + uri);
                    } else {
                        Log.d("PhotoPicker", "No media selected");
                    }
                });




        connectionsClient = Nearby.getConnectionsClient(this);
        discoveredEndpoints = new ArrayList<>();

        if (!arePermissionsGranted()) {
            requestPermissions();
        } else {
            startAdvertising();
            startDiscovery();
        }
    }

    private Drawable getDrawableFromUri(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            return new BitmapDrawable(getResources(), bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }






    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAdvertising();
        stopDiscovery();
    }

    private boolean arePermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.NEARBY_WIFI_DEVICES,
                    };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    };
        } else {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                    };
        }



        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }








    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                startAdvertising();
                startDiscovery();
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startAdvertising() {
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build();

        connectionsClient.startAdvertising(
                        "ChatApp",
                        getPackageName(),
                        connectionLifecycleCallback,
                        advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Log.d(TAG, "Advertising started");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Log.e(TAG, "Advertising failed: " + e.getMessage());
                        });
    }

    private void stopAdvertising() {
        connectionsClient.stopAdvertising();
        Log.d(TAG, "Advertising stopped");
    }

    private void startDiscovery() {
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build();

        connectionsClient.startDiscovery(
                        getPackageName(),
                        endpointDiscoveryCallback,
                        discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Log.d(TAG, "Discovery started");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Log.e(TAG, "Discovery failed: " + e.getMessage());
                        });
    }

    private void stopDiscovery() {
        connectionsClient.stopDiscovery();
        Log.d(TAG, "Discovery stopped");
    }

    private void sendMessage(String message,  Drawable imageDrawable) throws FileNotFoundException {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        Payload messagePayload = Payload.fromBytes(payload);
        connectionsClient.sendPayload(discoveredEndpoints, messagePayload);

        if (imageDrawable != null) {
            File imageFile = createImageFile(imageDrawable);
            if (imageFile != null) {
                Payload imagePayload = Payload.fromFile(imageFile);
                connectionsClient.sendPayload(discoveredEndpoints, imagePayload);
            }
        }

        if (imageDrawable!=null) {

            appendToChat("Me: " + message, ((BitmapDrawable) imageDrawable).getBitmap());
        }
        else{
            appendToChat("Me: " + message, null);

        }
    }
    private File createImageFile(Drawable imageDrawable) {
        try {
            Bitmap imageBitmap = ((BitmapDrawable) imageDrawable).getBitmap();
            File cacheDir = getCacheDir();
            File imageFile = new File(cacheDir, "image.png");
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            return imageFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    private void appendToChat(String message,Bitmap image) {
        ChatMessage chat=new ChatMessage(message,image);

        chatAdapter.add(chat);
        chatListView.setSelection(chatAdapter.getCount() - 1);
    }


    private void openAttachmentPicker() {
        // Add your code here to open the attachment picker
        // For example, you can use an Intent to open a file picker or camera
        // and handle the selected/captured attachment accordingly.
        // Example code for opening a file picker:
        attachmentLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.d(TAG, "Connection initiated: " + endpointId);
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        Log.d(TAG, "Connection successful: " + endpointId);
                        discoveredEndpoints.add(endpointId);
                    } else {
                        Log.d(TAG, "Connection failed: " + endpointId);
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.d(TAG, "Disconnected: " + endpointId);
                    discoveredEndpoints.remove(endpointId);
                }
            };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    Log.d(TAG, "Endpoint discovered: " + endpointId);
                    connectionsClient.requestConnection("ChatApp", endpointId, connectionLifecycleCallback)
                            .addOnSuccessListener(
                                    (Void unused) -> {
                                        Log.d(TAG, "Connection requested: " + endpointId);
                                    })
                            .addOnFailureListener(
                                    (Exception e) -> {
                                        Log.e(TAG, "Connection request failed: " + e.getMessage());
                                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    Log.d(TAG, "Endpoint lost: " + endpointId);
                    discoveredEndpoints.remove(endpointId);
                }
            };

    private Drawable loadImageFromFile(File file) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            return new BitmapDrawable(getResources(), bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    if (payload.getType() == Payload.Type.BYTES) {
                        String message = new String(payload.asBytes(), StandardCharsets.UTF_8);
                        appendToChat(endpointId + ": " + message,null);
                    } else if (payload.getType() == Payload.Type.FILE) {
//                        File fromPayload = payload.asFile().asJavaFile();
//                        Uri uri = Uri.fromFile(fromPayload);
                        Uri uri=payload.asFile().asUri();
                        InputStream inputStream = null;
                        try {
                            inputStream = getApplicationContext().getContentResolver().openInputStream(uri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            appendToChat(null,bitmap);
                        } catch (FileNotFoundException e) {
                            Log.d("Image Receive Problem", "Image Fail at Receive");
                            throw new RuntimeException(e);
                        }



                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    // No implementation needed for this example
                }
            };
}