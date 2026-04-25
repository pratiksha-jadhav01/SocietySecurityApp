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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
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

    // Same list as in VisitorLogsActivity for consistency
    private static final List<String> DELIVERY_PURPOSES = Arrays.asList(
            "Delivery", "Flipkart", "Swiggy", "Amazon", "Zomato"
    );

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

        // Fetching all visitors and filtering in Java to avoid FAILED_PRECONDITION
        deliveryListener = db.collection("visitors")
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (value == null) return;

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
                    java.util.Date startOfDay = cal.getTime();

                    deliveryList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        VisitorModel v = doc.toObject(VisitorModel.class);
                        if (v != null) {
                            String purpose = doc.getString("purpose");
                            String status = doc.getString("status");
                            java.util.Date timestamp = doc.getDate("timestamp");

                            // Filter for Delivery types
                            if (!isDelivery(purpose)) continue;

                            boolean matchesFilter = true;
                            if ("today".equals(filter)) {
                                matchesFilter = (timestamp != null && timestamp.after(startOfDay));
                            } else if ("Pending".equals(filter) || "Approved".equals(filter)) {
                                matchesFilter = filter.equalsIgnoreCase(status);
                            }

                            if (matchesFilter) {
                                v.setDocId(doc.getId());
                                deliveryList.add(v);
                            }
                        }
                    }

                    // Sort descending by timestamp
                    deliveryList.sort((v1, v2) -> {
                        if (v1.getTimestamp() == null || v2.getTimestamp() == null) return 0;
                        return v2.getTimestamp().compareTo(v1.getTimestamp());
                    });

                    adapter.notifyDataSetChanged();
                });
    }

    private boolean isDelivery(String purpose) {
        if (purpose == null) return false;
        for (String p : DELIVERY_PURPOSES) {
            if (p.equalsIgnoreCase(purpose.trim())) return true;
        }
        return false;
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
