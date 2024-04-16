package top.xrondev.lab.nearbychat.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

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

public class NearbyConnectionHelper {
    private static NearbyConnectionHelper instance; // Singleton instance for manage connections across the app

    private static final String SERVICE_ID = "NearbyChat_Service";
    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    private final ConnectionsClient connectionsClient;
    private final PayloadCallback payloadCallback = new PayloadCallback() {

        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            // Payload received
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            // Payload transfer update
        }
    };
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
            // Auto accept connection on both sides, or else the PUBLIC channel will not work.
            // For private channels, you can show a dialog to accept or reject the conversation,
            // but the connection is actually already established.

            connectionsClient.acceptConnection(s, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
            if (connectionResolution.getStatus().isSuccess()) {
                // We're connected! Can now start sending and receiving data.
                onConnectionConnected(s);
            } else {
                // Connection attempt failed or was rejected by either side.
                onConnectionFailed(s);
            }
        }

        @Override
        public void onDisconnected(@NonNull String s) {
            onConnectionDisconnected(s);
        }
    };
    public final String localEndpointName; // Generated local identifier
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String s, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
            // An endpoint was found. request a connection to it.
            connectionsClient.requestConnection(localEndpointName, s, connectionLifecycleCallback);

        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            // A previously discovered endpoint has gone away.

        }
    };

    private NearbyConnectionHelper(Context context) {
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

    private void onConnectionConnected(String endpointId) {
        // Handle successful connection
        Log.i("NearbyService", "Connected to: " + endpointId);
    }

    private void onConnectionFailed(String endpointId) {
        // Handle failed connection
    }

    private void onConnectionDisconnected(String endpointId) {
        // Handle disconnection
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

    public int getClientsCount() {
        // Get the number of connected clients
        return 0;
    }

}

