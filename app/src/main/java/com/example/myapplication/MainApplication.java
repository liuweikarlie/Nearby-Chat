package com.example.myapplication;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
public class MainApplication extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.ACCESS_FINE_LOCATION,

    };

    private EditText messageEditText;
    private Button sendButton;
    private TextView chatTextView;

    private ConnectionsClient connectionsClient;
    private String localEndpointName;
    private List<String> discoveredEndpoints;

    private ListView chatListView;
    private ArrayAdapter<String> chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatListView = findViewById(R.id.chatListView);
        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        chatListView.setAdapter(chatAdapter);        //chatRecyclerView = findViewById(R.id.chatRecyclerView);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString();
                if (!message.isEmpty()) {
                    sendMessage(message);
                    messageEditText.setText("");
                }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAdvertising();
        stopDiscovery();
    }

    private boolean arePermissionsGranted() {
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

    private void sendMessage(String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        connectionsClient.sendPayload(discoveredEndpoints, Payload.fromBytes(payload));
        appendToChat("Me: " + message);
    }

    private void appendToChat(String message) {
        chatAdapter.add(message);
        chatListView.setSelection(chatAdapter.getCount() - 1);
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

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    String message = new String(payload.asBytes(), StandardCharsets.UTF_8);
                    appendToChat(endpointId + ": " + message);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    // No implementation needed for this example
                }
            };
}