package com.example.securiodb;

import android.os.Bundle;
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
import java.util.List;
import java.util.Map;

public class LiveStatusActivity extends AppCompatActivity implements StatusAdapter.OnExitClickListener {

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
        // Query for everyone currently inside. 
        // We now include "Pending" status so they appear immediately when the guard sends the entry.
        registration = db.collection("visitors")
                .whereEqualTo("exitTime", null)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        List<Map<String, Object>> entries = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                String status = (String) data.get("status");
                                // Show if it's already Approved OR if it's still Pending (just sent)
                                if ("Approved".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status)) {
                                    data.put("id", doc.getId());
                                    entries.add(data);
                                }
                            }
                        }
                        adapter.updateList(entries);
                        tvLiveCount.setText("Active: " + entries.size());
                        tvEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    public void onExitClick(String docId) {
        db.collection("visitors").document(docId)
                .update("exitTime", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Exit marked", Toast.LENGTH_SHORT).show());
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
