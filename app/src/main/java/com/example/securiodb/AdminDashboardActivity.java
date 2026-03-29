package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvVisitorsToday, tvDeliveriesToday, tvActiveInside, tvPendingApprovals;
    private MaterialCardView cardManageUsers, cardVisitorLogs, cardDeliveryLogs, cardApprovals, cardReports;
    private MaterialCardView cardStatVisitors, cardStatDeliveries, cardStatActive, cardStatPending;
    private ImageView ivLogout;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNav;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private ValueEventListener statsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Security check
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupBottomNav();
        setupStatsListeners();
        setupQuickActions();

        ivLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void initViews() {
        tvVisitorsToday = findViewById(R.id.tvStatVisitorsToday);
        tvDeliveriesToday = findViewById(R.id.tvStatDeliveries);
        tvActiveInside = findViewById(R.id.tvStatActive);
        tvPendingApprovals = findViewById(R.id.tvStatPending);

        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardVisitorLogs = findViewById(R.id.cardVisitorLogs);
        cardDeliveryLogs = findViewById(R.id.cardDeliveryLogs);
        cardApprovals = findViewById(R.id.cardApprovals);
        cardReports = findViewById(R.id.cardReports);

        cardStatVisitors = findViewById(R.id.cardStatVisitors);
        cardStatDeliveries = findViewById(R.id.cardStatDeliveries);
        cardStatActive = findViewById(R.id.cardStatActive);
        cardStatPending = findViewById(R.id.cardStatPending);

        ivLogout = findViewById(R.id.ivLogout);
        progressBar = findViewById(R.id.progressBar);
        bottomNav = findViewById(R.id.adminBottomNav);
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_visitors) {
                startActivity(new Intent(this, VisitorLogsActivity.class));
                return true;
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(this, ManageUsersActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, AdminProfileActivity.class));
                return true;
            }
            return true;
        });
    }

    private void setupStatsListeners() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        statsListener = mDatabase.child("visitors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int visitorsToday = 0;
                int deliveriesToday = 0;
                int activeInside = 0;
                int pendingApprovals = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Long timestamp = ds.child("timestamp").getValue(Long.class);
                    String purpose = ds.child("purpose").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);
                    Object exitTime = ds.child("exitTime").getValue();

                    if (timestamp != null) {
                        String entryDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timestamp));
                        if (today.equals(entryDate)) {
                            if ("Visitor".equalsIgnoreCase(purpose)) visitorsToday++;
                            else if ("Delivery".equalsIgnoreCase(purpose)) deliveriesToday++;
                        }
                    }

                    if ("Approved".equalsIgnoreCase(status) && exitTime == null) {
                        activeInside++;
                    }

                    if ("Pending".equalsIgnoreCase(status)) {
                        pendingApprovals++;
                    }
                }

                if (tvVisitorsToday != null) tvVisitorsToday.setText(String.valueOf(visitorsToday));
                if (tvDeliveriesToday != null) tvDeliveriesToday.setText(String.valueOf(deliveriesToday));
                if (tvActiveInside != null) tvActiveInside.setText(String.valueOf(activeInside));
                if (tvPendingApprovals != null) tvPendingApprovals.setText(String.valueOf(pendingApprovals));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupQuickActions() {
        if (cardManageUsers != null) cardManageUsers.setOnClickListener(v -> startActivity(new Intent(this, ManageUsersActivity.class)));
        if (cardVisitorLogs != null) cardVisitorLogs.setOnClickListener(v -> startActivity(new Intent(this, VisitorLogsActivity.class)));
        if (cardDeliveryLogs != null) cardDeliveryLogs.setOnClickListener(v -> startActivity(new Intent(this, DeliveryLogsActivity.class)));
        if (cardApprovals != null) cardApprovals.setOnClickListener(v -> startActivity(new Intent(this, ApprovalsActivity.class)));
        if (cardReports != null) cardReports.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));

        // Stat cards click listeners
        if (cardStatVisitors != null) cardStatVisitors.setOnClickListener(v -> startActivity(new Intent(this, VisitorLogsActivity.class)));
        if (cardStatDeliveries != null) cardStatDeliveries.setOnClickListener(v -> startActivity(new Intent(this, DeliveryLogsActivity.class)));
        if (cardStatActive != null) cardStatActive.setOnClickListener(v -> startActivity(new Intent(this, VisitorLogsActivity.class)));
        if (cardStatPending != null) cardStatPending.setOnClickListener(v -> startActivity(new Intent(this, ApprovalsActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDatabase != null && statsListener != null) {
            mDatabase.child("visitors").removeEventListener(statsListener);
        }
    }
}
