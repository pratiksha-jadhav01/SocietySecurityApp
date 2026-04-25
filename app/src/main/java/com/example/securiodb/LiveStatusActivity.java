package com.example.securiodb;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.StatusAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LiveStatusActivity extends AppCompatActivity implements StatusAdapter.OnEntryActionListener {

    private RecyclerView rvLiveStatus;
    private TextView tvLiveCount, tvEmpty;
    private StatusAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_status);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupRealtimeListener();
    }

    private void initViews() {
        rvLiveStatus = findViewById(R.id.rvLiveStatus);
        tvLiveCount = findViewById(R.id.tvLiveCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new StatusAdapter(new ArrayList<>(), this);
        rvLiveStatus.setLayoutManager(new LinearLayoutManager(this));
        rvLiveStatus.setAdapter(adapter);
    }

    private void setupRealtimeListener() {
        // Real-time listener for Approved visitors/deliveries currently inside
        // Criteria: status == "Approved" AND exitTime == null
        registration = db.collection("visitors")
                .whereEqualTo("status", "Approved")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("LiveStatus", "Error fetching live status", error);
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        List<Map<String, Object>> entries = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                // Double check exitTime is null (Filtering in Java to avoid composite index requirement)
                                if (data.get("exitTime") == null) {
                                    data.put("id", doc.getId());
                                    entries.add(data);
                                }
                            }
                        }

                        // Manually sort by entryTime/timestamp descending (newest first)
                        Collections.sort(entries, (m1, m2) -> {
                            Object t1 = m1.get("timestamp");
                            Object t2 = m2.get("timestamp");
                            if (t1 == null) t1 = m1.get("entryTime");
                            if (t2 == null) t2 = m2.get("entryTime");

                            if (t1 instanceof com.google.firebase.Timestamp && t2 instanceof com.google.firebase.Timestamp) {
                                return ((com.google.firebase.Timestamp) t2).compareTo((com.google.firebase.Timestamp) t1);
                            }
                            return 0;
                        });

                        adapter.updateList(entries);
                        tvLiveCount.setText("Active Inside: " + entries.size());
                        tvEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    public void onExitClick(String docId) {
        db.collection("visitors").document(docId)
                .update("exitTime", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Exit marked successfully", Toast.LENGTH_SHORT).show();
                    // The realtime listener will automatically remove the item since exitTime is no longer null
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to mark exit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDeleteClick(String docId) {
        db.collection("visitors").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
