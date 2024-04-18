package top.xrondev.lab.nearbychat.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NearbyConnectionHelper {

    private static final String SERVICE_ID = "NearbyChat_Service";
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static NearbyConnectionHelper instance; // Singleton instance for manage connections across the app
    public final String localEndpointName; // Generated local identifier
    private final ConnectionsClient connectionsClient;
    public final ArrayList<String> connectedEndpoints = new ArrayList<String>(Arrays.asList("Public Channel"));
    private final Context context;
    private customDiscoveryCallback customDiscoveryCallback;
    private customConnectionCallback customConnectionCallback;
    private customPayloadCallback customPayloadCallback;

    public Object isFilenameMessage(Payload payload) {
        // Check if it is a FILENAME specific payload
        String message = new String(payload.asBytes(), StandardCharsets.UTF_8);
        if (message.startsWith("_METADATA_FILENAME:")) {
            return message.substring(19);
        }else {
            return null;
        }
    }

    /**
     * Extracts the payloadId and filename from the message and stores it in the
     * filePayloadFilenames map. The format is payloadId:filename.
     */
    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            // Payload received
            Log.i("ChatActivity", "Payload received from: " + s + ":" + payload.getType());

            if (customPayloadCallback != null) {
                customPayloadCallback.onPayloadReceived(s, payload);
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            // Payload transfer update
            if (customPayloadCallback != null) {
                customPayloadCallback.onPayloadTransferUpdate(s, payloadTransferUpdate);
            }
        }
    };
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
            // Auto accept connection on both sides, or else the PUBLIC channel will not work.
            // For private channels, you can show a dialog to accept or reject the conversation,
            // but the connection is actually already established.

            if (customConnectionCallback != null) {
                customConnectionCallback.onConnectionInitiated(s, connectionInfo);
            }
            Log.d("NearbyService", "Connection initiated: " + connectionInfo.getEndpointName());
            connectionsClient.acceptConnection(s, payloadCallback).addOnSuccessListener(aVoid -> {
                // Connection accepted
                Log.d("NearbyService CONN", "Connection accepted");
            }).addOnFailureListener(e -> {
                // Connection failed to accept
                Log.e("NearbyService CONN", "Connection failed to accept: " + e.getMessage());
            });
        }

        @Override
        public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
            boolean isSuccess = connectionResolution.getStatus().isSuccess();
            if (isSuccess) {
                Log.i("NearbyService", "Connected to: " + s);
                connectedEndpoints.add(s);
            }
            if (customConnectionCallback != null) {
                customConnectionCallback.onConnectionResult(s, isSuccess);
            }
        }

        @Override
        public void onDisconnected(@NonNull String s) {
            Log.i("NearbyService DISCONN", "Disconnected from: " + s);
            if (customConnectionCallback != null) {
                customConnectionCallback.onDisconnected(s);
                connectedEndpoints.remove(s);
            }
        }
    };
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String s, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
            // An endpoint was found. request a connection to it.
            connectionsClient.requestConnection(localEndpointName, s, connectionLifecycleCallback).addOnFailureListener(e -> {
                // Connection request failed
                Log.e("NearbyService FAILED", "Connection request failed: " + e.getMessage());
            }).addOnSuccessListener(aVoid -> {
                // Connection request succeeded
                Log.d("NearbyService", "Connection request succeeded");
            });
            Log.d("NearbyService", "Endpoint found: " + discoveredEndpointInfo.getEndpointName());

            if (customDiscoveryCallback != null) {
                customDiscoveryCallback.onEndpointFound(s, discoveredEndpointInfo.getEndpointName());
            }
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            // A previously discovered endpoint has gone away.
            connectedEndpoints.remove(endpointId);
            if (customDiscoveryCallback != null) {
                customDiscoveryCallback.onEndpointLost(endpointId);
            }
        }
    };
    private NearbyConnectionHelper(Context context) {
        this.context = context;
        this.connectionsClient = Nearby.getConnectionsClient(context);
        this.localEndpointName = "Device_" + android.os.Build.MODEL + "_" + System.currentTimeMillis();
    }

    // singleton pattern~ QWQ
    public static synchronized NearbyConnectionHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NearbyConnectionHelper(context);
        }
        return instance;
    }

    public void startAdvertising() {
        connectionsClient.startAdvertising(
                        localEndpointName, SERVICE_ID, connectionLifecycleCallback,
                        new AdvertisingOptions.Builder().setStrategy(STRATEGY).build())
                .addOnSuccessListener(unused -> {
                    // Successfully started advertising
                    Log.d("NearbyService", "Start Advertising");
                    onAdvertisingStarted();
                })
                .addOnFailureListener(e -> {
                    // Failed to start advertising
                    Log.e("NearbyService", "Failed to start advertising" + e.getMessage());
                    onAdvertisingFailed();
                });
    }

    public void startDiscovery() {
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();

        connectionsClient.startDiscovery(
                        SERVICE_ID, endpointDiscoveryCallback,
                        discoveryOptions)
                .addOnSuccessListener(unused -> {
                    // Successfully started discovery
                    Log.d("NearbyService", "Start Discovery");
                    onDiscoveryStarted();
                })
                .addOnFailureListener(e -> {
                    // Failed to start discovery
                    Log.e("NearbyService", "Failed to start discovery" + e.getMessage());
                    onDiscoveryFailed();
                });
    }

    public void stopAllEndpoints() {
        connectionsClient.stopAllEndpoints();
    }

    private void onDiscoveryStarted() {
        // Discovery started
    }

    private void onDiscoveryFailed() {
        // Discovery failed
    }

    private void onAdvertisingStarted() {
        // Advertising started
    }

    private void onAdvertisingFailed() {
        // Advertising failed
    }

    public void setDiscoveryCallback(customDiscoveryCallback discoveryCallback) {
        this.customDiscoveryCallback = discoveryCallback;
    }

    public void setConnectionCallback(customConnectionCallback connectionCallback) {
        this.customConnectionCallback = connectionCallback;
    }

    public void setPayloadCallback(customPayloadCallback payloadCallback) {
        this.customPayloadCallback = payloadCallback;
    }

    public void sendPayload(String endpointId, Payload payload) {
        if (endpointId.equals("Public Channel")) {
            Log.i("NearbyService PUBLIC", "TRY SEND");
            List<String> receiverEndpoints = new ArrayList<>(this.connectedEndpoints);
            receiverEndpoints.remove(0);
            connectionsClient.sendPayload(receiverEndpoints, payload).addOnSuccessListener(aVoid -> {
                // Payload sent successfully
                Log.i("NearbyService PUBLIC", "sent" + payload.getType());
            }).addOnFailureListener(e -> {
                // Payload failed to send
                Log.i("NearbyService PUBLIC", "ERROR" + e.getMessage());

            });
        } else {
            connectionsClient.sendPayload(endpointId, payload).addOnSuccessListener(aVoid -> {
                // Payload sent successfully
                Log.i("NearbyService", "sent");
            }).addOnFailureListener(e -> {
                // Payload failed to send
            });
        }
    }


    public interface customDiscoveryCallback {
        void onEndpointFound(String endpointId, String endpointName);

        void onEndpointLost(String endpointId);
    }

    public interface customConnectionCallback {
        void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo);

        void onConnectionResult(String endpointId, boolean isSuccess);

        void onDisconnected(String endpointId);
    }

    public interface customPayloadCallback {
        void onPayloadReceived(String endpointId, Payload payload);

        void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update);
    }
}

