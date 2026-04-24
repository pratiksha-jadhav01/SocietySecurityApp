package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.VisitorAdapter;
import com.example.securiodb.models.VisitorModel;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DeliveryLogsActivity extends AppCompatActivity {

    private RecyclerView rvDeliveries;
    private ProgressBar progressBar;
    private ChipGroup chipGroupFilter;
    
    private FirebaseFirestore db;
    private VisitorAdapter adapter;
    private List<VisitorModel> deliveryList = new ArrayList<>();
    private ListenerRegistration deliveryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_logs);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupFilters();
        
        loadDeliveries("all");
    }

    private void initViews() {
        rvDeliveries = findViewById(R.id.rvDeliveries);
        progressBar = findViewById(R.id.progressBar);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvDeliveries.setLayoutManager(new LinearLayoutManager(this));
        // Using the correct constructor for com.example.securiodb.adapter.VisitorAdapter
        adapter = new VisitorAdapter(this, deliveryList, new VisitorAdapter.OnItemClickListener() {
            @Override
            public void onApprove(String docId, int position) {
                updateStatus(docId, "Approved");
            }

            @Override
            public void onReject(String docId, int position) {
                updateStatus(docId, "Rejected");
            }
        });
        rvDeliveries.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipToday) loadDeliveries("today");
            else if (checkedId == R.id.chipPending) loadDeliveries("Pending");
            else if (checkedId == R.id.chipDelivered) loadDeliveries("Approved");
            else loadDeliveries("all");
        });
    }

    private void loadDeliveries(String filter) {
        if (deliveryListener != null) deliveryListener.remove();
        
        progressBar.setVisibility(View.VISIBLE);
        Query query = db.collection("visitors")
                .whereEqualTo("purpose", "Delivery")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if ("today".equals(filter)) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            query = query.whereGreaterThanOrEqualTo("timestamp", cal.getTime());
        } else if ("Pending".equals(filter) || "Approved".equals(filter)) {
            query = query.whereEqualTo("status", filter);
        }

        deliveryListener = query.addSnapshotListener((value, error) -> {
            progressBar.setVisibility(View.GONE);
            if (value != null) {
                deliveryList.clear();
                deliveryList.addAll(value.toObjects(VisitorModel.class));
                // Manually set docId from document snapshot
                for (int i = 0; i < value.getDocuments().size(); i++) {
                    deliveryList.get(i).setDocId(value.getDocuments().get(i).getId());
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void updateStatus(String docId, String status) {
        db.collection("visitors").document(docId).update("status", status)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Status updated to " + status, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deliveryListener != null) deliveryListener.remove();
    }
}
