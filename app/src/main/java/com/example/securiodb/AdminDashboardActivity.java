package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.Date;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";
    private TextView tvVisitorsToday, tvDeliveriesToday, tvActiveInside, tvPendingApprovals;
    private TextView tvAdminNameHeader;
    private ImageView ivAdminAvatarHeader;
    private View searchBar;
    private MaterialCardView cardManageUsers, cardVisitorLogs, cardDeliveryLogs, cardApprovals, cardReports, cardViewComplaints, cardCreateBill, cardBillHistory, cardNoticeBoard;
    private MaterialCardView cardStatVisitors, cardStatDeliveries, cardStatActive, cardStatPending;
    private View ivLogout;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNav;

    private FirebaseFirestore mFirestore;
    private FirebaseAuth mAuth;
    private ListenerRegistration statsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupBottomNav();
        setupStatsListeners();
        setupQuickActions();
        loadAdminHeader();

        if (ivLogout != null) {
            ivLogout.setOnClickListener(v -> {
                mAuth.signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void initViews() {
        tvVisitorsToday = findViewById(R.id.tvStatVisitorsToday);
        tvDeliveriesToday = findViewById(R.id.tvStatDeliveries);
        tvActiveInside = findViewById(R.id.tvStatActive);
        tvPendingApprovals = findViewById(R.id.tvStatPending);
        tvAdminNameHeader = findViewById(R.id.tvAdminNameHeader);
        ivAdminAvatarHeader = findViewById(R.id.ivAdminAvatarHeader);
        searchBar = findViewById(R.id.searchBar);

        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardVisitorLogs = findViewById(R.id.cardVisitorLogs);
        cardDeliveryLogs = findViewById(R.id.cardDeliveryLogs);
        cardApprovals = findViewById(R.id.cardApprovals);
        cardReports = findViewById(R.id.cardReports);
        cardViewComplaints = findViewById(R.id.cardViewComplaints);
        cardCreateBill = findViewById(R.id.cardCreateBill);
        cardBillHistory = findViewById(R.id.cardBillHistory);
        cardNoticeBoard = findViewById(R.id.cardNoticeBoard);

        cardStatVisitors = findViewById(R.id.cardStatVisitors);
        cardStatDeliveries = findViewById(R.id.cardStatDeliveries);
        cardStatActive = findViewById(R.id.cardStatActive);
        cardStatPending = findViewById(R.id.cardStatPending);

        ivLogout = findViewById(R.id.ivLogoutContainer);
        if (ivLogout == null) {
            ivLogout = findViewById(R.id.ivLogout);
        }

        progressBar = findViewById(R.id.progressBar);
        bottomNav = findViewById(R.id.adminBottomNav);
    }

    private void setupBottomNav() {
        if (bottomNav == null) return;
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

    private void loadAdminHeader() {
        String uid = mAuth.getUid();
        if (uid == null) return;
        mFirestore.collection("users").document(uid).addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) {
                String name = doc.getString("name");
                String img = doc.getString("profileImageUrl");
                if (tvAdminNameHeader != null) tvAdminNameHeader.setText(name);
                if (ivAdminAvatarHeader != null && img != null && !img.isEmpty()) {
                    Glide.with(this).load(img).placeholder(android.R.drawable.ic_menu_gallery).into(ivAdminAvatarHeader);
                }
            }
        });
    }

    private void setupStatsListeners() {
        statsListener = mFirestore.collection("visitors")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed", e);
                        return;
                    }

                    if (snapshots != null) {
                        int visitorsToday = 0;
                        int deliveriesToday = 0;
                        int activeInside = 0;
                        int pendingApprovals = 0;

                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); 
                        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
                        Date startOfToday = cal.getTime();

                        for (DocumentSnapshot ds : snapshots.getDocuments()) {
                            Timestamp ts = ds.getTimestamp("timestamp");
                            String purpose = ds.getString("purpose");
                            String status = ds.getString("status");
                            Timestamp exitTime = ds.getTimestamp("exitTime");

                            if (ts != null) {
                                Date entryDate = ts.toDate();
                                if (entryDate.after(startOfToday) || entryDate.equals(startOfToday)) {
                                    if ("Visitor".equalsIgnoreCase(purpose)) visitorsToday++;
                                    else if ("Delivery".equalsIgnoreCase(purpose)) deliveriesToday++;
                                    
                                    // Count as active inside ONLY if they entered TODAY and haven't left
                                    if ("Approved".equalsIgnoreCase(status) && exitTime == null) {
                                        activeInside++;
                                    }

                                    // Count as pending ONLY if requested TODAY
                                    if ("Pending".equalsIgnoreCase(status)) {
                                        pendingApprovals++;
                                    }
                                }
                            }
                        }

                        if (tvVisitorsToday != null) tvVisitorsToday.setText(String.valueOf(visitorsToday));
                        if (tvDeliveriesToday != null) tvDeliveriesToday.setText(String.valueOf(deliveriesToday));
                        if (tvActiveInside != null) tvActiveInside.setText(String.valueOf(activeInside));
                        if (tvPendingApprovals != null) tvPendingApprovals.setText(String.valueOf(pendingApprovals));
                    }
                });
    }

    private void setupQuickActions() {
        if (searchBar != null) searchBar.setOnClickListener(v -> startActivity(new Intent(this, VisitorLogsActivity.class)));
        if (cardManageUsers != null) cardManageUsers.setOnClickListener(v -> startActivity(new Intent(this, ManageUsersActivity.class)));
        if (cardVisitorLogs != null) cardVisitorLogs.setOnClickListener(v -> startActivity(new Intent(this, VisitorLogsActivity.class)));
        if (cardDeliveryLogs != null) cardDeliveryLogs.setOnClickListener(v -> startActivity(new Intent(this, DeliveryLogsActivity.class)));
        
        View.OnClickListener approvalsClick = v -> {
            Intent intent = new Intent(this, ApprovalsActivity.class);
            intent.putExtra("IS_ADMIN", true);
            intent.putExtra("FILTER_STATUS", "All");
            startActivity(intent);
        };
        if (cardApprovals != null) cardApprovals.setOnClickListener(approvalsClick);
        
        if (cardReports != null) cardReports.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));
        if (cardViewComplaints != null) cardViewComplaints.setOnClickListener(v -> startActivity(new Intent(this, AdminComplaintsActivity.class)));
        
        if (cardCreateBill != null) cardCreateBill.setOnClickListener(v -> startActivity(new Intent(this, AdminCreateBillActivity.class)));
        if (cardBillHistory != null) cardBillHistory.setOnClickListener(v -> startActivity(new Intent(this, AdminMaintenanceActivity.class)));

        if (cardNoticeBoard != null) {
            // Updated to open AdminNoticeBoardActivity instead of direct Post Form
            cardNoticeBoard.setOnClickListener(v -> startActivity(new Intent(this, AdminNoticeBoardActivity.class)));
        }

        if (cardStatVisitors != null) cardStatVisitors.setOnClickListener(v -> startActivity(new Intent(this, VisitorLogsActivity.class)));
        if (cardStatDeliveries != null) cardStatDeliveries.setOnClickListener(v -> startActivity(new Intent(this, DeliveryLogsActivity.class)));
        if (cardStatActive != null) cardStatActive.setOnClickListener(v -> startActivity(new Intent(this, VisitorLogsActivity.class)));
        if (cardStatPending != null) {
            cardStatPending.setOnClickListener(v -> {
                Intent intent = new Intent(this, ApprovalsActivity.class);
                intent.putExtra("IS_ADMIN", true);
                intent.putExtra("FILTER_STATUS", "Pending");
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statsListener != null) {
            statsListener.remove();
        }
    }
}
