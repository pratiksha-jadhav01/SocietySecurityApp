package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Calendar;
import java.util.Date;

public class GuardDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvVisitorsToday, tvDeliveriesToday, tvInside;
    private MaterialCardView cardAddVisitor, cardAddDelivery, cardLiveStatus, cardMyEntries, cardDailyHelpers;
    private View searchBar, ivLogoutContainer;
    private BottomNavigationView bottomNav;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String guardUid;
    private ListenerRegistration visitorsListener, deliveriesListener, insideListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guard_dashboard);

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
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvGuardWelcome);
        tvVisitorsToday = findViewById(R.id.tvStatVisitors);
        tvDeliveriesToday = findViewById(R.id.tvStatDeliveries);
        tvInside = findViewById(R.id.tvStatInside);

        cardAddVisitor = findViewById(R.id.cardAddVisitor);
        cardAddDelivery = findViewById(R.id.cardAddDelivery);
        cardLiveStatus = findViewById(R.id.cardLiveStatus);
        cardMyEntries = findViewById(R.id.cardMyEntries);
        cardDailyHelpers = findViewById(R.id.cardDailyHelpers);
        
        searchBar = findViewById(R.id.searchBar);
        ivLogoutContainer = findViewById(R.id.ivLogoutContainer);
        bottomNav = findViewById(R.id.bottomNav);
    }

    private void fetchGuardName() {
        db.collection("users").document(guardUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && !isFinishing()) {
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
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        // 1. Visitors Today (Count all today's entries where purpose is "Visitor")
        visitorsListener = db.collection("visitors")
                .whereEqualTo("purpose", "Visitor")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .addSnapshotListener((value, error) -> {
                    if (error != null || isFinishing()) return;
                    if (value != null) tvVisitorsToday.setText(String.valueOf(value.size()));
                });

        // 2. Deliveries Today (Count all today's entries where purpose is "Delivery")
        deliveriesListener = db.collection("visitors")
                .whereEqualTo("purpose", "Delivery")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .addSnapshotListener((value, error) -> {
                    if (error != null || isFinishing()) return;
                    if (value != null) tvDeliveriesToday.setText(String.valueOf(value.size()));
                });

        // 3. Currently Inside (Count entries that haven't marked exit yet)
        insideListener = db.collection("visitors")
                .whereEqualTo("exitTime", null)
                .addSnapshotListener((value, error) -> {
                    if (error != null || isFinishing()) {
                        Log.e("GuardDashboard", "Error fetching inside stats", error);
                        return;
                    }
                    if (value != null) {
                        tvInside.setText(String.valueOf(value.size()));
                    }
                });
    }

    private void setupClickListeners() {
        cardAddVisitor.setOnClickListener(v -> startActivity(new Intent(this, VisitorEntryActivity.class)));
        cardAddDelivery.setOnClickListener(v -> startActivity(new Intent(this, AddDeliveryActivity.class)));
        cardLiveStatus.setOnClickListener(v -> startActivity(new Intent(this, LiveStatusActivity.class)));
        cardMyEntries.setOnClickListener(v -> startActivity(new Intent(this, StatusListActivity.class)));
        cardDailyHelpers.setOnClickListener(v -> startActivity(new Intent(this, GuardHelperListActivity.class)));
        
        searchBar.setOnClickListener(v -> {
            Intent intent = new Intent(this, StatusListActivity.class);
            intent.putExtra("isSearch", true);
            startActivity(intent);
        });

        if (ivLogoutContainer != null) {
            ivLogoutContainer.setOnClickListener(v -> {
                mAuth.signOut();
                Intent intent = new Intent(GuardDashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            });
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visitorsListener != null) visitorsListener.remove();
        if (deliveriesListener != null) deliveriesListener.remove();
        if (insideListener != null) insideListener.remove();
    }
}
