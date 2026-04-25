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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerApprovalsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private List<Map<String, Object>> pendingList = new ArrayList<>();
    private VisitorRequestAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;
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

        adapter = new VisitorRequestAdapter(this, pendingList,
            (docId, position) -> updateVisitorStatus(docId, "Approved"),
            (docId, position) -> updateVisitorStatus(docId, "Rejected")
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchOwnerFlatAndListen();
    }

    private void fetchOwnerFlatAndListen() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String ownerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(ownerUid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    ownerFlatNo = doc.getString("flatNumber");
                    if (ownerFlatNo == null) ownerFlatNo = doc.getString("flatNo");

                    if (ownerFlatNo != null && !ownerFlatNo.isEmpty()) {
                        startRealTimeListener(ownerFlatNo);
                    } else {
                        Toast.makeText(this, "Flat number not set in your profile", Toast.LENGTH_LONG).show();
                    }
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void startRealTimeListener(String flatNo) {
        // Simplified query to avoid FAILED_PRECONDITION (requires composite index)
        listenerReg = db.collection("visitors")
            .whereEqualTo("flatNumber", flatNo)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e("OwnerApprovals", "Listener error: " + error);
                    return;
                }
                if (snapshots == null) return;

                pendingList.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        String status = (String) data.get("status");
                        // Accept both Visitor and Delivery requests if they are Pending
                        if ("Pending".equalsIgnoreCase(status)) {
                            data.put("docId", doc.getId());
                            pendingList.add(data);
                        }
                    }
                }

                // Sort by timestamp manually in Java (descending) to avoid index requirement
                Collections.sort(pendingList, (m1, m2) -> {
                    Object t1 = m1.get("timestamp");
                    Object t2 = m2.get("timestamp");
                    if (t1 instanceof com.google.firebase.Timestamp && t2 instanceof com.google.firebase.Timestamp) {
                        return ((com.google.firebase.Timestamp) t2).compareTo((com.google.firebase.Timestamp) t1);
                    }
                    return 0;
                });

                adapter.notifyDataSetChanged();

                if (pendingList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
    }

    private void updateVisitorStatus(String docId, String newStatus) {
        String ownerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> update = new HashMap<>();
        update.put("status", newStatus);
        update.put("approvedBy", ownerUid);
        update.put("approvedAt", FieldValue.serverTimestamp());

        db.collection("visitors").document(docId)
            .update(update)
            .addOnSuccessListener(v -> {
                String msg = newStatus.equals("Approved") ? "✅ Approved" : "❌ Rejected";
                Snackbar.make(recyclerView, msg, Snackbar.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerReg != null) listenerReg.remove();
    }
}
