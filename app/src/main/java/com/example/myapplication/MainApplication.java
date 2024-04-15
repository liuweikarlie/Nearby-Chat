package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment; // Import Fragment class
import androidx.fragment.app.FragmentManager; // Import FragmentManager class

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainApplication extends AppCompatActivity {
    private static final int NAV_CHAT_ID = R.id.navChat;
    private static  String[] REQUIRED_PERMISSIONS ;
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    ChatApplication chat;
    private static final int NAV_SETTINGS_ID = R.id.navSettings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Initialize UI elements
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
//        if (!arePermissionsGranted()) {
//            requestPermissions();
//        }

        chat=new ChatApplication();
        replacement(chat);








        bottomNavigationView.setOnItemSelectedListener( item -> {
//                new BottomNavigationView.OnNavigationItemSelectedListener() {
//                    @Override
//                    public boolean onNavigationItemSelected(MenuItem item) {
                        // Handle item clicks here
                        switch (item.getItemId()) {
                            case R.id.navChat:
                                replacement(new ChatApplication());
                                // Handle item 1 click
                                break;
                            case R.id.navSettings:
                                replacement(new GameApplication());
                                // Handle item 2 click
                                break;
                            default:
                                return false;
                        }
                        return true;
                    }

        );
    }

    private void replacement(Fragment fragment) {
        // Here, you need to define what you want to do with the fragment
        // For example, if you want to replace the current fragment with the new one, you can do something like this:
        if (fragment instanceof GameApplication){
            if (chat !=null){
                chat.onDestroy();
            }

        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.frame_layout, fragment) // Assuming you have a container in your layout with the id "fragment_container"
                .commit();
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }
    private boolean arePermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            android.Manifest.permission.BLUETOOTH_SCAN,
                            android.Manifest.permission.BLUETOOTH_ADVERTISE,
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.ACCESS_WIFI_STATE,
                            android.Manifest.permission.CHANGE_WIFI_STATE,
                            android.Manifest.permission.NEARBY_WIFI_DEVICES,
                            android.Manifest.permission.NFC
                    };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            android.Manifest.permission.BLUETOOTH_SCAN,
                            android.Manifest.permission.BLUETOOTH_ADVERTISE,
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.ACCESS_WIFI_STATE,
                            android.Manifest.permission.CHANGE_WIFI_STATE,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.NFC
                    };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_ADMIN,
                            android.Manifest.permission.ACCESS_WIFI_STATE,
                            android.Manifest.permission.CHANGE_WIFI_STATE,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.NFC
                    };
        } else {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_ADMIN,
                            android.Manifest.permission.ACCESS_WIFI_STATE,
                            android.Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.NFC
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



}