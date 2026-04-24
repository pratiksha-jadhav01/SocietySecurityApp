package com.example.securiodb;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.VisitorRequestAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerApprovalsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private List<Map<String, Object>> pendingList = new ArrayList<>();
    private VisitorRequestAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg; // for cleanup
    private String ownerFlatNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_approvals);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerPending);
        layoutEmpty  = findViewById(R.id.layoutEmpty);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView
        adapter = new VisitorRequestAdapter(this, pendingList,
            // Approve callback
            (docId, position) -> updateVisitorStatus(docId, "Approved"),
            // Reject callback
            (docId, position) -> updateVisitorStatus(docId, "Rejected")
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Step 1: Get owner's flat number, then start listener
        fetchOwnerFlatAndListen();
    }

    // ─── Fetch owner profile → then start real-time listener ───
    private void fetchOwnerFlatAndListen() {
        String ownerUid = FirebaseAuth.getInstance()
                              .getCurrentUser().getUid();

        db.collection("users").document(ownerUid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    // Changed from "flatNo" to "flatNumber" to match common schema if needed, 
                    // but following your provided snippet which used "flatNo"
                    ownerFlatNo = doc.getString("flatNumber");
                    if (ownerFlatNo == null) ownerFlatNo = doc.getString("flatNo");

                    if (ownerFlatNo != null && !ownerFlatNo.isEmpty()) {
                        startRealTimeListener(ownerFlatNo);
                    } else {
                        Toast.makeText(this,
                            "Flat number not set in your profile",
                            Toast.LENGTH_LONG).show();
                    }
                }
            })
            .addOnFailureListener(e ->
                Toast.makeText(this,
                    "Error fetching profile: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show());
    }

    // ─── REAL-TIME LISTENER — fires instantly on new entries ───
    private void startRealTimeListener(String flatNo) {
        listenerReg = db.collection("visitors")
            .whereIn("flatNumber", java.util.Arrays.asList(flatNo, flatNo.toUpperCase(), flatNo.toLowerCase()))   // Support different cases
            .whereEqualTo("status",  "Pending") // only pending requests
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {

                if (error != null) {
                    Log.e("OwnerApprovals", "Listener error: " + error);
                    return;
                }
                if (snapshots == null) return;

                // Rebuild list on every change
                pendingList.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("docId", doc.getId()); // needed for update
                        pendingList.add(data);
                    }
                }

                // Update RecyclerView instantly
                adapter.notifyDataSetChanged();

                // Show/hide empty state
                if (pendingList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
    }

    // ─── Approve or Reject ──────────────────────────────────────
    private void updateVisitorStatus(String docId, String newStatus) {
        String ownerUid = FirebaseAuth.getInstance()
                              .getCurrentUser().getUid();

        Map<String, Object> update = new HashMap<>();
        update.put("status",     newStatus);
        update.put("approvedBy", ownerUid);
        update.put("approvedAt", FieldValue.serverTimestamp());

        db.collection("visitors").document(docId)
            .update(update)
            .addOnSuccessListener(v -> {
                // Snapshot listener auto-removes card — no manual list edit needed
                String msg = newStatus.equals("Approved")
                    ? "✅ Visitor Approved" : "❌ Visitor Rejected";
                Snackbar.make(recyclerView, msg, Snackbar.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e ->
                Toast.makeText(this,
                    "Update failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show());
    }

    // ─── ALWAYS remove listener to prevent memory leaks ────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerReg != null) listenerReg.remove();
    }
}
