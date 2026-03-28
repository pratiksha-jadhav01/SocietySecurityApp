package com.example.securiodb;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvStatTotalUsers, tvStatVisitorsToday, tvStatPending, tvStatRejectedToday;
    private MaterialCardView cardManageUsers, cardAllLogs, cardExportLogs;
    private ImageView ivLogout;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener statsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 1. Security Check: Authentication
        if (mAuth.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        initViews();
        
        // 2. Security Check: Role Verification
        verifyAdminRole();

        // 3. Setup Stats and Listeners
        setupStatsRealtime();
        setupClickListeners();
    }

    private void initViews() {
        tvStatTotalUsers = findViewById(R.id.tvStatTotalUsers);
        tvStatVisitorsToday = findViewById(R.id.tvStatVisitorsToday);
        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatRejectedToday = findViewById(R.id.tvStatRejectedToday);
        
        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardAllLogs = findViewById(R.id.cardAllLogs);
        cardExportLogs = findViewById(R.id.cardExportLogs);
        
        ivLogout = findViewById(R.id.ivLogout);
        progressBar = findViewById(R.id.progressBar);
    }

    private void verifyAdminRole() {
        String uid = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(uid).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                if (!"admin".equals(role)) {
                    Toast.makeText(AdminDashboardActivity.this, "Access Denied", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    navigateToLogin();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                navigateToLogin();
            }
        });
    }

    private void setupStatsRealtime() {
        progressBar.setVisibility(View.VISIBLE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Count Users
                long totalUsers = snapshot.child("users").getChildrenCount();
                tvStatTotalUsers.setText(String.valueOf(totalUsers));

                // Count Visitors
                int visitorsToday = 0;
                int pending = 0;
                int rejectedToday = 0;

                for (DataSnapshot vDoc : snapshot.child("visitors").getChildren()) {
                    Long timestamp = vDoc.child("timestamp").getValue(Long.class);
                    String status = vDoc.child("status").getValue(String.class);
                    
                    if (timestamp != null) {
                        String entryDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timestamp));
                        
                        if (today.equals(entryDate)) {
                            visitorsToday++;
                            if ("Rejected".equalsIgnoreCase(status)) rejectedToday++;
                        }
                    }
                    
                    if ("Pending".equalsIgnoreCase(status)) {
                        pending++;
                    }
                }

                tvStatVisitorsToday.setText(String.valueOf(visitorsToday));
                tvStatPending.setText(String.valueOf(pending));
                tvStatRejectedToday.setText(String.valueOf(rejectedToday));
                
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        };

        mDatabase.addValueEventListener(statsListener);
    }

    private void setupClickListeners() {
        cardManageUsers.setOnClickListener(v -> {
            Toast.makeText(this, "Opening User Management...", Toast.LENGTH_SHORT).show();
            // startActivity(new Intent(this, UserManagementActivity.class));
        });

        cardAllLogs.setOnClickListener(v -> {
            Toast.makeText(this, "Opening All Logs...", Toast.LENGTH_SHORT).show();
            // startActivity(new Intent(this, AllLogsActivity.class));
        });

        cardExportLogs.setOnClickListener(v -> exportLogsToCSV());

        ivLogout.setOnClickListener(v -> {
            mAuth.signOut();
            navigateToLogin();
        });
    }

    private void exportLogsToCSV() {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("visitors").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder csvData = new StringBuilder();
                csvData.append("Name,Phone,Flat,Purpose,Status,Time\n");

                for (DataSnapshot vDoc : snapshot.getChildren()) {
                    csvData.append(vDoc.child("name").getValue()).append(",")
                           .append(vDoc.child("phone").getValue()).append(",")
                           .append(vDoc.child("flatNumber").getValue()).append(",")
                           .append(vDoc.child("purpose").getValue()).append(",")
                           .append(vDoc.child("status").getValue()).append(",")
                           .append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                   .format(new Date(vDoc.child("timestamp").getValue(Long.class))))
                           .append("\n");
                }

                saveCSVFile(csvData.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void saveCSVFile(String data) {
        String fileName = "VisitorLogs_" + System.currentTimeMillis() + ".csv";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

        try {
            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                outputStream.write(data.getBytes());
                outputStream.close();
                progressBar.setVisibility(View.GONE);
                Snackbar.make(cardExportLogs, "Logs exported to Downloads", Snackbar.LENGTH_LONG)
                        .setAction("Open", view -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, "text/csv");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        }).show();
            }
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDatabase != null && statsListener != null) {
            mDatabase.removeEventListener(statsListener);
        }
    }
}
