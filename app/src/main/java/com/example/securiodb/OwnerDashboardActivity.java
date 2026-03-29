package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.VisitorAdapter;
import com.example.securiodb.models.Visitor;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class OwnerDashboardActivity extends AppCompatActivity {

    private TextView tvFlatHeader, tvOwnerNameHeader, tvStatVisitorsToday, tvStatDeliveriesToday, tvStatPendingCount;
    private MaterialCardView cardPendingStats;
    private RecyclerView rvPendingPreview;
    private BottomNavigationView bottomNav;
    private View btnSeeAll;
    private MaterialCardView btnLiveStatus, btnHistory, btnDeliveries, btnContactGuard;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String ownerUid, flatNumber;
    
    private List<Visitor> pendingPreviewList = new ArrayList<>();
    private VisitorAdapter adapter;
    private ListenerRegistration statsListener, pendingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        ownerUid = mAuth.getCurrentUser().getUid();

        initViews();
        fetchOwnerData();
        setupBottomNav();
    }

    private void initViews() {
        tvFlatHeader = findViewById(R.id.tvFlatHeader);
        tvOwnerNameHeader = findViewById(R.id.tvOwnerNameHeader);
        tvStatVisitorsToday = findViewById(R.id.tvStatVisitorsToday);
        tvStatDeliveriesToday = findViewById(R.id.tvStatDeliveriesToday);
        tvStatPendingCount = findViewById(R.id.tvStatPendingRequests);
        cardPendingStats = findViewById(R.id.cardPendingStats);
        rvPendingPreview = findViewById(R.id.rvPendingPreview);
        bottomNav = findViewById(R.id.bottomNav);
        btnSeeAll = findViewById(R.id.btnSeeAll);
        
        btnLiveStatus = findViewById(R.id.btnLiveStatus);
        btnHistory = findViewById(R.id.btnHistory);
        btnDeliveries = findViewById(R.id.btnDeliveries);
        btnContactGuard = findViewById(R.id.btnContactGuard);

        rvPendingPreview.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VisitorAdapter(pendingPreviewList);
        rvPendingPreview.setAdapter(adapter);
    }

    private void fetchOwnerData() {
        FirebaseDatabase.getInstance().getReference("users").child(ownerUid)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        flatNumber = snapshot.child("flatNumber").getValue(String.class);
                        String name = snapshot.child("name").getValue(String.class);
                        
                        tvFlatHeader.setText("Flat " + flatNumber + " 🏠");
                        tvOwnerNameHeader.setText(name);
                        
                        if (flatNumber != null) {
                            startStatsListeners();
                            startPendingPreviewListener();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(OwnerDashboardActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void startStatsListeners() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startOfDay = cal.getTime();

        statsListener = db.collection("visitors")
                .whereEqualTo("flatNumber", flatNumber)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    int visitors = 0, deliveries = 0;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String purpose = doc.getString("purpose");
                        if ("Visitor".equalsIgnoreCase(purpose)) visitors++;
                        else if ("Delivery".equalsIgnoreCase(purpose)) deliveries++;
                    }
                    tvStatVisitorsToday.setText(String.valueOf(visitors));
                    tvStatDeliveriesToday.setText(String.valueOf(deliveries));
                });
    }

    private void startPendingPreviewListener() {
        pendingListener = db.collection("visitors")
                .whereEqualTo("flatNumber", flatNumber)
                .whereEqualTo("status", "Pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    int count = value.size();
                    tvStatPendingCount.setText(String.valueOf(count));
                    
                    if (count > 0) {
                        cardPendingStats.setCardBackgroundColor(ContextCompat.getColor(this, R.color.amber_pending));
                        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_requests);
                        badge.setNumber(count);
                        badge.setVisible(true);
                    } else {
                        cardPendingStats.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_light_green));
                        bottomNav.removeBadge(R.id.nav_requests);
                    }

                    pendingPreviewList.clear();
                    List<DocumentSnapshot> docs = value.getDocuments();
                    for (int i = 0; i < Math.min(docs.size(), 3); i++) {
                        pendingPreviewList.add(docs.get(i).toObject(Visitor.class));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_requests) {
                startActivity(new Intent(this, ApprovalsActivity.class));
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, VisitorHistoryActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, OwnerProfileActivity.class));
            }
            return true;
        });

        btnSeeAll.setOnClickListener(v -> startActivity(new Intent(this, ApprovalsActivity.class)));
        btnLiveStatus.setOnClickListener(v -> startActivity(new Intent(this, LiveVisitorsActivity.class)));
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, VisitorHistoryActivity.class)));
        btnDeliveries.setOnClickListener(v -> startActivity(new Intent(this, DeliveryManagementActivity.class)));
        btnContactGuard.setOnClickListener(v -> startActivity(new Intent(this, ContactGuardActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statsListener != null) statsListener.remove();
        if (pendingListener != null) pendingListener.remove();
    }
}
