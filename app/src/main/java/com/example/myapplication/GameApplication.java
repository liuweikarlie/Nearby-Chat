package com.example.myapplication;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GameApplication extends Fragment implements View.OnClickListener {

    private static final int MAX_CONNECTIONS = 2; // Maximum allowed connections


    private static final String TAG = "GameActivity";

    private ConnectionsClient connectionsClient;
    private String localEndpointName;
    private List<String> discoveredEndpoints;

    private static final int BOARD_SIZE = 5; // Size of the game board
    private static final int PLAYER_X = 0;
    private static final int PLAYER_O = 1;
    private static final String TAG_ROW = "tag_row";
    private static final String TAG_COL = "tag_col";

    private ImageView[][] board;
    private Button resetButton;
    private int currentPlayer;
    private boolean gameOver;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        LinearLayout boardLayout = view.findViewById(R.id.boardLayout);
        resetButton = view.findViewById(R.id.resetButton);
        connectionsClient = Nearby.getConnectionsClient(requireContext());
        discoveredEndpoints = new ArrayList<>();
//        currentPlayer = PLAYER_X;
        gameOver = false;

        startAdvertising();
        startDiscovery();

        initializeBoard(boardLayout);
        resetButton.setOnClickListener(this);


        return view;

    }

    private void startAdvertising() {
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build();

        connectionsClient.startAdvertising(
                        "GameApp",
                        requireContext().getPackageName(),
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

    public void onDestroy() {
        super.onDestroy();
        stopAdvertising();
        stopDiscovery();
    }

    private void stopAdvertising() {
        connectionsClient.stopAdvertising();
        Log.d(TAG, "Advertising stopped");
    }

    private void startDiscovery() {
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build();

        connectionsClient.startDiscovery(
                        requireContext().getPackageName(),
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

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.d(TAG, "Connection initiated: " + endpointId);

                    if (discoveredEndpoints.size() >= MAX_CONNECTIONS) {

                        connectionsClient.rejectConnection(endpointId); // Reject if max connections reached
                        Log.d(TAG, "Rejected connection from: " + endpointId);
                        return;
                    }
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        Log.d(TAG, "Connection successful: " + endpointId);
                        discoveredEndpoints.add(endpointId);

                        int connectedEndpointsCount=discoveredEndpoints.size();

                        if (connectedEndpointsCount == 1) {
                            appendToChat("Start --- Play X : ME, PLAY O : You");
                            currentPlayer=PLAYER_X;
//                            gameOver=false;

                        }
                    } else {
                        Log.d(TAG, "Connection failed: " + endpointId);
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.d(TAG, "Disconnected: " + endpointId);
                    discoveredEndpoints.remove(endpointId);
                    if (discoveredEndpoints.size()<1){

//                        gameOver=true;
                        resetGame();
                    }
                }
            };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    Log.d(TAG, "Endpoint discovered: " + endpointId);
                    connectionsClient.requestConnection("GameApp", endpointId, connectionLifecycleCallback)
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
                    if (payload.getType() == Payload.Type.BYTES) {
                        String message = new String(payload.asBytes(), StandardCharsets.UTF_8);

                        appendToChat(endpointId + ": " + message);

                        String[] parts = message.split(",");
                        int row = Integer.parseInt(parts[0]);
                        int col = Integer.parseInt(parts[1]);

                        // Update the board
                        if (currentPlayer == PLAYER_X) {
                            board[row][col].setImageResource(R.drawable.x);
                        } else {
                            board[row][col].setImageResource(R.drawable.o);
                        }
                        checkWinCondition(currentPlayer,row,col);

                        // Switch players
                        currentPlayer = (currentPlayer == PLAYER_X) ? PLAYER_O : PLAYER_X;

                        // Check for game over condition

                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    // No implementation needed for this example
                }
            };

    public void appendToChat(String message) {
        Log.d(TAG, message);

    }

    private void sendMessage(String message)  {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        Payload messagePayload = Payload.fromBytes(payload);
        connectionsClient.sendPayload(discoveredEndpoints, messagePayload);



        appendToChat("Me: " + message);
    }


    private void initializeBoard(LinearLayout boardLayout) {
        boardLayout.setVisibility(View.VISIBLE);
        board = new ImageView[BOARD_SIZE][BOARD_SIZE];
        Log.d("BROAD INITIATE","REACH");
        for (int i = 0; i < BOARD_SIZE; i++) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = new ImageView(requireContext());
                int cellWidth = ViewGroup.LayoutParams.MATCH_PARENT / BOARD_SIZE;

                board[i][j].setLayoutParams(new LinearLayout.LayoutParams(100,100));
//                board[i][j].setImageResource(R.drawable.empty); // Set empty image initially
                board[i][j].setBackgroundResource(R.drawable.cell_background);
                board[i][j].setOnClickListener(this);
                board[i][j].setTag(R.id.tag_row, i);
                board[i][j].setTag(R.id.tag_col, j);

                row.addView(board[i][j]);
            }

            boardLayout.addView(row);
        }


    }

    @Override
    public void onClick(View v) {
        if (v == resetButton) {
            resetGame();
        } else if (!gameOver ) {
            int row = (int) v.getTag(R.id.tag_row);
            int col = (int) v.getTag(R.id.tag_col);

            if (board[row][col].getDrawable() == null) { // Check if the cell is empty
                if (currentPlayer == PLAYER_X) {
                    board[row][col].setImageResource(R.drawable.x);
                    sendMessage(""+row+","+col);
                    currentPlayer = PLAYER_O;
                    Log.d(TAG,""+row+","+col);
                } else {
                    board[row][col].setImageResource(R.drawable.o);
                    sendMessage(""+row+","+col);
                    currentPlayer = PLAYER_X;
                    Log.d(TAG,""+row+","+col);
                }

                checkWinCondition(currentPlayer,row, col);
            }
        }
    }

    private void resetGame() {
        // Reset the game state and clear the board
        currentPlayer = PLAYER_X;
        gameOver = false;

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j].setImageDrawable(null);
                board[i][j].setBackgroundResource(R.drawable.cell_background);

            }
        }
    }

    private void checkWinCondition(int player, int lastRow, int lastCol) {
        // Check rows
        for (int i = 0; i < BOARD_SIZE; i++) {
            int count = 0;
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j].getDrawable() != null &&
                        ((BitmapDrawable) board[i][j].getDrawable()).getBitmap() ==
                                ((BitmapDrawable) ContextCompat.getDrawable(requireContext(), player == PLAYER_X ? R.drawable.x : R.drawable.o)).getBitmap()) {
                    count++;
                }
            }
            if (count == BOARD_SIZE) {
                // Player has won horizontally
                gameOver = true;
                Log.d(TAG, "Player " + (player == PLAYER_X ? "X" : "O") + " wins horizontally");
                return;
            }
        }

        // Check columns
        for (int j = 0; j < BOARD_SIZE; j++) {
            int count = 0;
            for (int i = 0; i < BOARD_SIZE; i++) {
                if (board[i][j].getDrawable() != null &&
                        ((BitmapDrawable) board[i][j].getDrawable()).getBitmap() ==
                                ((BitmapDrawable) ContextCompat.getDrawable(requireContext(), player == PLAYER_X ? R.drawable.x : R.drawable.o)).getBitmap()) {
                    count++;
                }
            }
            if (count == BOARD_SIZE) {
                // Player has won vertically
                gameOver = true;
                Log.d(TAG, "Player " + (player == PLAYER_X ? "X" : "O") + " wins vertically");
                return;
            }
        }

        // Check diagonals
        int countMainDiagonal = 0;
        int countAntiDiagonal = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (board[i][i].getDrawable() != null &&
                    ((BitmapDrawable) board[i][i].getDrawable()).getBitmap() ==
                            ((BitmapDrawable) ContextCompat.getDrawable(requireContext(), player == PLAYER_X ? R.drawable.x : R.drawable.o)).getBitmap()) {
                countMainDiagonal++;
            }
            if (board[i][BOARD_SIZE - 1 - i].getDrawable() != null &&
                    ((BitmapDrawable) board[i][BOARD_SIZE - 1 - i].getDrawable()).getBitmap() ==
                            ((BitmapDrawable) ContextCompat.getDrawable(requireContext(), player == PLAYER_X ? R.drawable.x : R.drawable.o)).getBitmap()) {
                countAntiDiagonal++;
            }
        }
        if (countMainDiagonal == BOARD_SIZE || countAntiDiagonal == BOARD_SIZE) {
            // Player has won diagonally
            gameOver = true;
            Log.d(TAG, "Player " + (player == PLAYER_X ? "X" : "O") + " wins diagonally");
        }

        // Check for a draw
        if (!gameOver && isBoardFull()) {
            // Game is a draw
            Log.d(TAG, "Game is a draw");
            gameOver = true;
        }
    }

    private boolean isBoardFull() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j].getDrawable() == null) {
                    return false; // There is an empty cell
                }
            }
        }
        return true; // All cells are filled
    }








}
