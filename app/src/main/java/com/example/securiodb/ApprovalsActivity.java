package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.VisitorAdapter;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approvals);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvApprovals = findViewById(R.id.rvApprovals);
        tabLayout = findViewById(R.id.tabLayout);
        emptyState = findViewById(R.id.emptyState);

        rvApprovals.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VisitorAdapter(visitorList, true, this);
        rvApprovals.setAdapter(adapter);

        fetchOwnerFlatAndStartListener();
        setupTabs();
    }

    private void fetchOwnerFlatAndStartListener() {
        if (mAuth.getCurrentUser() == null) return;
        
        String uid = mAuth.getCurrentUser().getUid();
        // Use Realtime Database to fetch user profile, same as OwnerDashboardActivity
        FirebaseDatabase.getInstance().getReference("users").child(uid)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        flatNumber = snapshot.child("flatNumber").getValue(String.class);
                        listenForVisitors();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
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

    private void listenForVisitors() {
        if (visitorListener != null) visitorListener.remove();
        if (flatNumber == null) return;

        // Ensure we are using the correct field name "flatNumber" as used in VisitorEntryActivity
        Query query = db.collection("visitors")
                .whereEqualTo("flatNumber", flatNumber)
                .orderBy("timestamp", Query.Direction.DESCENDING);

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
    public void onOverride(Visitor visitor) {
        updateStatus(visitor, "Approved");
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
