package com.example.securiodb;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.StatusAdapter;
import com.example.securiodb.models.Visitor;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class StatusListActivity extends AppCompatActivity {

    private RecyclerView rvStatusList;
    private StatusAdapter adapter;
    private List<Visitor> allVisitorList = new ArrayList<>();
    private List<Visitor> filteredList = new ArrayList<>();
    
    private LinearLayout layoutEmptyState;
    private TextView tvEmptySubtext;
    private String currentFilter = "All";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status_list);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupChips();
        listenForEntries();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvStatusList = findViewById(R.id.rvStatusList);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptySubtext = findViewById(R.id.tvEmptySubtext);
    }

    private void setupRecyclerView() {
        adapter = new StatusAdapter(this, filteredList);
        rvStatusList.setLayoutManager(new LinearLayoutManager(this));
        rvStatusList.setAdapter(adapter);
    }

    private void setupChips() {
        Chip chipAll = findViewById(R.id.chipAll);
        Chip chipPending = findViewById(R.id.chipPending);
        Chip chipApproved = findViewById(R.id.chipApproved);
        Chip chipRejected = findViewById(R.id.chipRejected);

        chipAll.setOnClickListener(v -> { currentFilter = "All"; applyFilter("All"); });
        chipPending.setOnClickListener(v -> { currentFilter = "Pending"; applyFilter("Pending"); });
        chipApproved.setOnClickListener(v -> { currentFilter = "Approved"; applyFilter("Approved"); });
        chipRejected.setOnClickListener(v -> { currentFilter = "Rejected"; applyFilter("Rejected"); });
    }

    private void listenForEntries() {
        String guardUid = mAuth.getCurrentUser().getUid();

        // Real-time listener for Firestore
        db.collection("visitors")
                .whereEqualTo("createdBy", guardUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("StatusListActivity", "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        allVisitorList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Visitor v = doc.toObject(Visitor.class);
                            if (v != null) {
                                allVisitorList.add(v);
                            }
                        }
                        applyFilter(currentFilter);
                    }
                });
    }

    private void applyFilter(String filter) {
        filteredList.clear();
        for (Visitor v : allVisitorList) {
            if (filter.equals("All") || filter.equalsIgnoreCase(v.getStatus())) {
                filteredList.add(v);
            }
        }
        adapter.notifyDataSetChanged();
        showEmptyState(filteredList.isEmpty(), filter);
    }

    private void showEmptyState(boolean isEmpty, String filter) {
        if (isEmpty) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptySubtext.setText("There are no " + filter.toLowerCase() + " entries found.");
        } else {
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}
