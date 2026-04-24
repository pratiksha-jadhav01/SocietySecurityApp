package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.models.Visitor;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class ApprovalsActivity extends AppCompatActivity implements VisitorAdapter.OnVisitorActionListener {

    private RecyclerView rvApprovals;
    private TabLayout tabLayout;
    private LinearLayout emptyState;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String flatNumber;
    
    private List<Visitor> visitorList = new ArrayList<>();
    private VisitorAdapter adapter;
    private ListenerRegistration visitorListener;
    private String currentStatusFilter = "Pending";
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approvals);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        String initialFilter = getIntent().getStringExtra("FILTER_STATUS");
        if (initialFilter != null) {
            currentStatusFilter = initialFilter;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (isAdmin) {
            toolbar.setTitle("System Approvals");
        }

        rvApprovals = findViewById(R.id.rvApprovals);
        tabLayout = findViewById(R.id.tabLayout);
        emptyState = findViewById(R.id.emptyState);

        rvApprovals.setLayoutManager(new LinearLayoutManager(this));
        // role "admin" allows seeing all, "owner" filters by flat
        adapter = new VisitorAdapter(visitorList, isAdmin ? "admin" : "owner", this);
        rvApprovals.setAdapter(adapter);

        if (isAdmin) {
            setupAdminTabs();
            listenForVisitors();
        } else {
            fetchOwnerFlatAndStartListener();
            setupTabs();
        }
    }

    private void fetchOwnerFlatAndStartListener() {
        if (mAuth.getCurrentUser() == null) return;
        
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    flatNumber = documentSnapshot.getString("flatNumber");
                    listenForVisitors();
                }
            });
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentStatusFilter = tab.getPosition() == 0 ? "Pending" : "All";
                listenForVisitors();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupAdminTabs() {
        if (tabLayout.getTabCount() > 0) {
            if (currentStatusFilter.equals("All")) {
                tabLayout.getTabAt(1).select();
            } else {
                tabLayout.getTabAt(0).select();
            }
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentStatusFilter = tab.getPosition() == 0 ? "Pending" : "All";
                listenForVisitors();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void listenForVisitors() {
        if (visitorListener != null) visitorListener.remove();

        Query query = db.collection("visitors")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (!isAdmin) {
            if (flatNumber == null) return;
            query = query.whereEqualTo("flatNumber", flatNumber);
        }

        if (currentStatusFilter.equals("Pending")) {
            query = query.whereEqualTo("status", "Pending");
        }

        visitorListener = query.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            
            visitorList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                Visitor visitor = doc.toObject(Visitor.class);
                if (visitor != null) {
                    visitor.setVisitorId(doc.getId());
                    visitorList.add(visitor);
                }
            }
            adapter.notifyDataSetChanged();
            
            if (emptyState != null) {
                emptyState.setVisibility(visitorList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onApprove(Visitor visitor) {
        updateStatus(visitor, "Approved");
    }

    @Override
    public void onReject(Visitor visitor) {
        updateStatus(visitor, "Rejected");
    }

    @Override
    public void onMarkExit(Visitor visitor) {
        // Implementation for marking exit if needed
    }

    private void updateStatus(Visitor visitor, String status) {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("visitors").document(visitor.getVisitorId()).update(
                "status", status,
                "approvedBy", mAuth.getCurrentUser().getUid(),
                "approvedAt", FieldValue.serverTimestamp()
        ).addOnSuccessListener(aVoid -> {
            String msg = status.equals("Approved") ? "✅ Visitor Approved" : "❌ Visitor Rejected";
            Snackbar.make(rvApprovals, msg, Snackbar.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visitorListener != null) visitorListener.remove();
    }
}
