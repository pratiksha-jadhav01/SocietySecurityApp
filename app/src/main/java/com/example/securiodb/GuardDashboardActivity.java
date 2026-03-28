package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GuardDashboardActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvGuardName, tvTotalCount, tvPendingCount, tvApprovedCount;
    private MaterialCardView cardAddVisitor, cardAddDelivery, cardViewStatus, cardMarkExit;
    private ImageView ivLogoutHeader;
    private Button btnFooterLogout;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener statsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guard_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Check Authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize Views
        initViews();

        // Fetch Guard Profile
        fetchGuardProfile(currentUser.getUid());

        // Setup Real-time Stats
        setupRealtimeStats(currentUser.getUid());

        // Setup Click Listeners
        setupClickListeners();
    }

    private void initViews() {
        tvGuardName = findViewById(R.id.tvGuardName);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);
        
        cardAddVisitor = findViewById(R.id.cardAddVisitor);
        cardAddDelivery = findViewById(R.id.cardAddDelivery);
        cardViewStatus = findViewById(R.id.cardViewStatus);
        cardMarkExit = findViewById(R.id.cardMarkExit);
        
        ivLogoutHeader = findViewById(R.id.ivLogoutHeader);
        btnFooterLogout = findViewById(R.id.btnFooterLogout);
        progressBar = findViewById(R.id.progressBar);
    }

    private void fetchGuardProfile(String uid) {
        // Fetching name from Realtime Database (since we migrated from Firestore)
        mDatabase.child("users").child(uid).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.getValue(String.class);
                    tvGuardName.setText("Welcome, " + name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silently fail or log
            }
        });
    }

    private void setupRealtimeStats(String guardId) {
        progressBar.setVisibility(View.VISIBLE);
        
        // Get current date string for filtering
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Listening to "visitors" node
        statsListener = mDatabase.child("visitors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int total = 0;
                int pending = 0;
                int approved = 0;

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    // Filter by Guard ID and Today's Date
                    String createdBy = postSnapshot.child("createdBy").getValue(String.class);
                    Long timestamp = postSnapshot.child("timestamp").getValue(Long.class);
                    String status = postSnapshot.child("status").getValue(String.class);

                    if (timestamp != null && guardId.equals(createdBy)) {
                        String entryDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(new Date(timestamp));

                        if (today.equals(entryDate)) {
                            total++;
                            if ("Pending".equalsIgnoreCase(status)) pending++;
                            else if ("Approved".equalsIgnoreCase(status)) approved++;
                        }
                    }
                }

                // Update UI
                tvTotalCount.setText(String.valueOf(total));
                tvPendingCount.setText(String.valueOf(pending));
                tvApprovedCount.setText(String.valueOf(approved));
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GuardDashboardActivity.this, "Error loading stats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        // Add Visitor
        cardAddVisitor.setOnClickListener(v -> {
            Intent intent = new Intent(this, VisitorEntryActivity.class);
            intent.putExtra("type", "Visitor");
            startActivity(intent);
        });

        // Add Delivery
        cardAddDelivery.setOnClickListener(v -> {
            Intent intent = new Intent(this, VisitorEntryActivity.class);
            intent.putExtra("type", "Delivery");
            startActivity(intent);
        });

        // View Status (Reusable Admin View or dedicated StatusListActivity)
        cardViewStatus.setOnClickListener(v -> {
            // Note: If StatusListActivity doesn't exist yet, you can use AdminDashboardActivity 
            // but filtered for guard's own entries.
            Toast.makeText(this, "Opening Status List...", Toast.LENGTH_SHORT).show();
            // startActivity(new Intent(this, StatusListActivity.class));
        });

        // Mark Exit
        cardMarkExit.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Mark Exit...", Toast.LENGTH_SHORT).show();
            // startActivity(new Intent(this, MarkExitActivity.class));
        });

        // Logout Actions
        View.OnClickListener logoutListener = v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        };

        ivLogoutHeader.setOnClickListener(logoutListener);
        btnFooterLogout.setOnClickListener(logoutListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener to prevent memory leaks
        if (mDatabase != null && statsListener != null) {
            mDatabase.child("visitors").removeEventListener(statsListener);
        }
    }
}
