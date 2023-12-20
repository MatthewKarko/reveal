package com.example.revealapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.revealapp.fragments.HomeFragment;
import com.example.revealapp.fragments.ProfileFragment;
import com.example.revealapp.fragments.SearchFragment;
import com.example.revealapp.fragments.SettingsFragment;
import com.example.revealapp.fragments.UploadFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.ArrayList;

public class NavigationActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private FirebaseAuth.AuthStateListener authStateListener;

    public static boolean isActive = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // Load the HomeFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
        }

        FirebaseInAppMessaging.getInstance().setAutomaticDataCollectionEnabled(true);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            } else if (itemId == R.id.nav_upload) {
                getUploadPermissions();
                selectedFragment = new UploadFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            }

            return true;
        });
        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseInstallations.getInstance().getId()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful()) {
                            Log.d("Installations", "Installation ID: " + task.getResult());
                        } else {
                            Log.e("Installations", "Unable to get Installation ID");
                        }
                    }
                });
        firebaseAuth = FirebaseAuth.getInstance();


    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseInAppMessaging.getInstance().triggerEvent("navigation_activity");
        BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };
        IntentFilter intentFilter = new IntentFilter("com.example.notificationapp.NOTIFICATION");
        registerReceiver(notificationReceiver, intentFilter);
    }

    private void getUploadPermissions() {
        String[] requiredPermissions = new String[] { android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.MANAGE_MEDIA };

        ArrayList<String> permissionsNotGranted = new ArrayList<String>();
        for (String requiredPermission : requiredPermissions) {
            if (checkSelfPermission(requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNotGranted.add(requiredPermission);
            }
        }

        if (!permissionsNotGranted.isEmpty()) {
            String[] permissionsArray = new String[permissionsNotGranted.size()];
            permissionsNotGranted.toArray(permissionsArray);
            requestPermissions(permissionsArray, 101);
        }
    }

}