package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.VisitorAdapter;
import com.example.securiodb.models.Visitor;
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
    private List<Visitor> deliveryList = new ArrayList<>();
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
        adapter = new VisitorAdapter(deliveryList);
        rvDeliveries.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipToday) loadDeliveries("today");
            else if (checkedId == R.id.chipPending) loadDeliveries("Pending");
            else if (checkedId == R.id.chipDelivered) loadDeliveries("Approved"); // Using Approved as Delivered for now
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
                deliveryList.addAll(value.toObjects(Visitor.class));
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deliveryListener != null) deliveryListener.remove();
    }
}
