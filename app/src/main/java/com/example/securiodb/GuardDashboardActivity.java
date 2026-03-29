package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GuardDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvVisitorsToday, tvDeliveriesToday, tvInside;
    private MaterialCardView cardAddVisitor, cardAddDelivery, cardMarkExit, cardLiveStatus, cardMyEntries;
    private BottomNavigationView bottomNav;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String guardUid;
    private ListenerRegistration visitorsListener, deliveriesListener, insideListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guard_dashboard);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        guardUid = mAuth.getCurrentUser().getUid();

        initViews();
        fetchGuardName();
        setupStatsListeners();
        setupClickListeners();
        setupBottomNav();
        updateFcmToken();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvGuardWelcome);
        tvVisitorsToday = findViewById(R.id.tvStatVisitors);
        tvDeliveriesToday = findViewById(R.id.tvStatDeliveries);
        tvInside = findViewById(R.id.tvStatInside);

        cardAddVisitor = findViewById(R.id.cardAddVisitor);
        cardAddDelivery = findViewById(R.id.cardAddDelivery);
        cardMarkExit = findViewById(R.id.cardMarkExit);
        cardLiveStatus = findViewById(R.id.cardLiveStatus);
        cardMyEntries = findViewById(R.id.cardMyEntries);

        bottomNav = findViewById(R.id.bottomNav);
    }

    private void fetchGuardName() {
        db.collection("users").document(guardUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                tvWelcome.setText("Hey, " + (name != null ? name : "Guard") + " 👋");
            }
        });
    }

    private void setupStatsListeners() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startOfDay = calendar.getTime();

        // Visitors Today Listener
        visitorsListener = db.collection("visitors")
                .whereEqualTo("createdBy", guardUid)
                .whereEqualTo("purpose", "Visitor")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) tvVisitorsToday.setText(String.valueOf(value.size()));
                });

        // Deliveries Today Listener
        deliveriesListener = db.collection("visitors")
                .whereEqualTo("createdBy", guardUid)
                .whereEqualTo("purpose", "Delivery")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) tvDeliveriesToday.setText(String.valueOf(value.size()));
                });

        // Currently Inside Listener (Active Inside)
        insideListener = db.collection("visitors")
                .whereEqualTo("createdBy", guardUid)
                .whereEqualTo("status", "Approved")
                .whereEqualTo("exitTime", null)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Error fetching inside stats", error);
                        return;
                    }
                    if (value != null) tvInside.setText(String.valueOf(value.size()));
                });
    }

    private void setupClickListeners() {
        cardAddVisitor.setOnClickListener(v -> {
            Intent intent = new Intent(this, VisitorEntryActivity.class);
            intent.putExtra("purpose", "Visitor");
            startActivity(intent);
        });

        cardAddDelivery.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddDeliveryActivity.class);
            startActivity(intent);
        });

        cardMarkExit.setOnClickListener(v -> startActivity(new Intent(this, MarkExitActivity.class)));
        cardLiveStatus.setOnClickListener(v -> startActivity(new Intent(this, LiveStatusActivity.class)));
        cardMyEntries.setOnClickListener(v -> startActivity(new Intent(this, StatusListActivity.class)));
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_add_entry) {
                startActivity(new Intent(this, VisitorEntryActivity.class));
                return true;
            } else if (id == R.id.nav_visitors) {
                startActivity(new Intent(this, StatusListActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, GuardProfileActivity.class));
                return true;
            }
            return true;
        });
    }

    private void updateFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String token = task.getResult();
                db.collection("users").document(guardUid).update("fcmToken", token);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visitorsListener != null) visitorsListener.remove();
        if (deliveriesListener != null) deliveriesListener.remove();
        if (insideListener != null) insideListener.remove();
    }
}
