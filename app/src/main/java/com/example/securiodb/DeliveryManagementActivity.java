package com.example.securiodb;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.VisitorAdapter;
import com.example.securiodb.models.VisitorModel;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DeliveryManagementActivity extends AppCompatActivity {

    private RecyclerView rvDeliveries;
    private ChipGroup chipGroupDelivery;
    
    private FirebaseFirestore db;
    private String flatNumber;
    private List<VisitorModel> deliveryList = new ArrayList<>();
    private VisitorAdapter adapter;
    private ListenerRegistration deliveryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_management);

        db = FirebaseFirestore.getInstance();
        rvDeliveries = findViewById(R.id.rvDeliveries);
        chipGroupDelivery = findViewById(R.id.chipGroupDelivery);

        rvDeliveries.setLayoutManager(new LinearLayoutManager(this));
        
        // Updated adapter with both Approve and Reject logic
        adapter = new VisitorAdapter(this, deliveryList, new VisitorAdapter.OnItemClickListener() {
            @Override
            public void onApprove(String docId, int position) {
                updateDeliveryStatus(docId, "Approved");
            }

            @Override
            public void onReject(String docId, int position) {
                updateDeliveryStatus(docId, "Rejected");
            }
        });
        rvDeliveries.setAdapter(adapter);

        fetchFlatAndLoadDeliveries();
        chipGroupDelivery.setOnCheckedStateChangeListener((group, checkedIds) -> loadDeliveries());
    }

    private void fetchFlatAndLoadDeliveries() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                flatNumber = doc.getString("flatNumber");
                if (flatNumber == null) flatNumber = doc.getString("flatNo");
                loadDeliveries();
            }
        });
    }

    private void loadDeliveries() {
        if (flatNumber == null) return;
        if (deliveryListener != null) deliveryListener.remove();

        // Simplified query to avoid FAILED_PRECONDITION (composite index requirement)
        deliveryListener = db.collection("visitors")
                .whereEqualTo("flatNumber", flatNumber)
                .addSnapshotListener((value, e) -> {
                    if (e != null || value == null) return;
                    
                    int checkedId = chipGroupDelivery.getCheckedChipId();
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

                            // Filter for Delivery only
                            if (!"Delivery".equalsIgnoreCase(purpose)) continue;

                            boolean matchesFilter = true;
                            if (checkedId == R.id.chipDelToday) {
                                matchesFilter = (timestamp != null && timestamp.after(startOfDay));
                            } else if (checkedId == R.id.chipDelPending) {
                                matchesFilter = "Pending".equalsIgnoreCase(status);
                            } else if (checkedId == R.id.chipDelReceived) {
                                matchesFilter = "Approved".equalsIgnoreCase(status);
                            }

                            if (matchesFilter) {
                                v.setDocId(doc.getId());
                                deliveryList.add(v);
                            }
                        }
                    }
                    // Sort descending by timestamp in Java to avoid index requirement
                    deliveryList.sort((v1, v2) -> {
                        if (v1.getTimestamp() == null || v2.getTimestamp() == null) return 0;
                        return v2.getTimestamp().compareTo(v1.getTimestamp());
                    });
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateDeliveryStatus(String docId, String status) {
        db.collection("visitors").document(docId)
                .update("status", status, "approvedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> {
                    String msg = "Approved".equals(status) ? "✅ Delivery Approved" : "❌ Delivery Rejected";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    // SnapshotListener will automatically update the list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deliveryListener != null) deliveryListener.remove();
    }
}
