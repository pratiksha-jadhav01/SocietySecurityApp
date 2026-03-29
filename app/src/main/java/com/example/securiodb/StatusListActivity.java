package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.StatusAdapter;
import com.google.android.material.chip.ChipGroup;
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

public class StatusListActivity extends AppCompatActivity implements StatusAdapter.OnExitClickListener {

    private RecyclerView rvEntries;
    private ChipGroup chipGroupFilter;
    private TextView tvEmpty;
    private StatusAdapter adapter;
    private List<Map<String, Object>> allEntries = new ArrayList<>();
    
    private FirebaseFirestore db;
    private String guardUid;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status_list);

        db = FirebaseFirestore.getInstance();
        guardUid = FirebaseAuth.getInstance().getUid();

        initViews();
        setupRecyclerView();
        setupRealtimeListener();
        setupFilter();
    }

    private void initViews() {
        rvEntries = findViewById(R.id.rvEntries);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        tvEmpty = findViewById(R.id.tvEmpty);
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new StatusAdapter(new ArrayList<>(), this);
        rvEntries.setLayoutManager(new LinearLayoutManager(this));
        rvEntries.setAdapter(adapter);
    }

    private void setupRealtimeListener() {
        registration = db.collection("visitors")
                .whereEqualTo("createdBy", guardUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        allEntries.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("id", doc.getId());
                                allEntries.add(data);
                            }
                        }
                        applyFilter();
                    }
                });
    }

    private void setupFilter() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilter());
    }

    private void applyFilter() {
        int checkedId = chipGroupFilter.getCheckedChipId();
        List<Map<String, Object>> filteredList = new ArrayList<>();

        String filterStatus = "";
        if (checkedId == R.id.chipPending) filterStatus = "Pending";
        else if (checkedId == R.id.chipApproved) filterStatus = "Approved";
        else if (checkedId == R.id.chipRejected) filterStatus = "Rejected";

        if (filterStatus.isEmpty()) {
            filteredList.addAll(allEntries);
        } else {
            for (Map<String, Object> entry : allEntries) {
                if (filterStatus.equalsIgnoreCase((String) entry.get("status"))) {
                    filteredList.add(entry);
                }
            }
        }

        adapter.updateList(filteredList);
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onExitClick(String docId) {
        db.collection("visitors").document(docId)
                .update("exitTime", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Exit marked", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to mark exit", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
