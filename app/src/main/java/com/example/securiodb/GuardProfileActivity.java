package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.Date;

public class GuardProfileActivity extends AppCompatActivity {

    private TextView tvName, tvTotalEntries, btnLogout, btnChangePassword, btnNotifications;
    private BottomNavigationView bottomNav;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String guardUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guard_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        guardUid = mAuth.getUid();

        initViews();
        fetchProfileData();
        fetchMonthlyStats();
        setupBottomNav();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvGuardName);
        tvTotalEntries = findViewById(R.id.tvTotalEntries);
        btnLogout = findViewById(R.id.btnLogout);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnNotifications = findViewById(R.id.btnNotifications);
        bottomNav = findViewById(R.id.bottomNav);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnChangePassword.setOnClickListener(v -> {
            Toast.makeText(this, "Redirecting to Change Password...", Toast.LENGTH_SHORT).show();
            // Implement password change logic or activity
        });

        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });
    }

    private void fetchProfileData() {
        db.collection("users").document(guardUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvName.setText(documentSnapshot.getString("name"));
            }
        });
    }

    private void fetchMonthlyStats() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        Date startOfMonth = calendar.getTime();

        db.collection("visitors")
                .whereEqualTo("createdBy", guardUid)
                .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvTotalEntries.setText(String.valueOf(queryDocumentSnapshots.size()));
                });
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, GuardDashboardActivity.class));
                return true;
            } else if (id == R.id.nav_add_entry) {
                startActivity(new Intent(this, VisitorEntryActivity.class));
                return true;
            } else if (id == R.id.nav_visitors) {
                startActivity(new Intent(this, StatusListActivity.class));
                return true;
            }
            return true;
        });
    }
}
