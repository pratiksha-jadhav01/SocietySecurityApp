package com.example.securiodb;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.securiodb.ui.ApprovalsFragment;
import com.example.securiodb.ui.DailyHelperFragment;
import com.example.securiodb.ui.HomeDashboardFragment;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class OwnerDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FirebaseFirestore db;
    private String ownerFlat;
    private ListenerRegistration pendingListener;

    // Realtime Database for Notifications
    private DatabaseReference mDatabase;
    private ChildEventListener mChildEventListener;
    private boolean isInitialLoad = true;

    // Permission Launcher for Android 13+
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("NotificationPermission", "Granted");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        checkNotificationPermission();

        bottomNav = findViewById(R.id.bottomNav);
        setupBottomNav();

        // Fetch owner flat number
        db.collection("users").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    ownerFlat = doc.getString("flatNumber");
                    if (ownerFlat == null) ownerFlat = doc.getString("flatNo");
                    
                    if (ownerFlat != null && !ownerFlat.isEmpty()) {
                        startRealTimeVisitorListener();
                        startPendingBadgeListener();
                    }
                }
                
                if (savedInstanceState == null) {
                    loadFragment(new HomeDashboardFragment());
                    bottomNav.setSelectedItemId(R.id.navHome);
                }
            })
            .addOnFailureListener(e -> {
                if (savedInstanceState == null) {
                    loadFragment(new HomeDashboardFragment());
                    bottomNav.setSelectedItemId(R.id.navHome);
                }
            });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void startRealTimeVisitorListener() {
        if (ownerFlat == null) return;

        mDatabase = FirebaseDatabase.getInstance().getReference("visitors");

        // First, fetch existing count to know when new data arrives
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isInitialLoad = false;
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (isInitialLoad) return; // Skip existing data

                String vFlat = snapshot.child("flatNumber").getValue(String.class);
                if (vFlat == null) vFlat = snapshot.child("flatNo").getValue(String.class);

                if (ownerFlat.equalsIgnoreCase(vFlat)) {
                    String name = snapshot.child("name").getValue(String.class);
                    String purpose = snapshot.child("purpose").getValue(String.class);
                    
                    String msg = (name != null ? name : "Visitor") + " arrived at the gate";
                    if (purpose != null) msg += " for " + purpose;
                    
                    showNotification("New Visitor Arrival", msg);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        mDatabase.addChildEventListener(mChildEventListener);
    }

    private void showNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "visitor_notifications";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Visitor Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.secu_ground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            if (id == R.id.navHome) fragment = new HomeDashboardFragment();
            else if (id == R.id.navRequests) {
                fragment = new ApprovalsFragment();
                Bundle b = new Bundle();
                b.putString("flatNo", ownerFlat);
                b.putString("purpose", "Visitor");
                fragment.setArguments(b);
            }
            else if (id == R.id.navHistory) {
                startActivity(new Intent(this, VisitorHistoryActivity.class));
                return false; 
            } else if (id == R.id.navHelpers) fragment = new DailyHelperFragment();
            else if (id == R.id.navProfile) {
                startActivity(new Intent(this, OwnerProfileActivity.class));
                return false;
            }
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    public void loadFragment(Fragment fragment) {
        if (fragment.getArguments() == null) {
            Bundle args = new Bundle();
            args.putString("flatNo", ownerFlat != null ? ownerFlat : "");
            fragment.setArguments(args);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
    }

    private void startPendingBadgeListener() {
        if (ownerFlat == null || ownerFlat.isEmpty()) return;
        if (pendingListener != null) pendingListener.remove();
        
        // Badge tracks pending visitor requests
        pendingListener = db.collection("visitors")
            .whereEqualTo("flatNumber", ownerFlat)
            .whereEqualTo("status", "Pending")
            .addSnapshotListener((snap, e) -> {
                if (snap == null || isFinishing()) return;
                int count = snap.size();
                BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.navRequests);
                if (count > 0) {
                    badge.setVisible(true);
                    badge.setNumber(count);
                    badge.setBackgroundColor(ContextCompat.getColor(this, R.color.badge_red));
                } else {
                    badge.setVisible(false);
                }
            });
    }

    @Override 
    protected void onDestroy() {
        super.onDestroy();
        if (pendingListener != null) pendingListener.remove();
        if (mDatabase != null && mChildEventListener != null) {
            mDatabase.removeEventListener(mChildEventListener);
        }
    }
}
