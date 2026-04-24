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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_management);

        db = FirebaseFirestore.getInstance();
        rvDeliveries = findViewById(R.id.rvDeliveries);
        chipGroupDelivery = findViewById(R.id.chipGroupDelivery);

        rvDeliveries.setLayoutManager(new LinearLayoutManager(this));
        // Corrected constructor usage for VisitorAdapter
        adapter = new VisitorAdapter(this, deliveryList, new VisitorAdapter.OnItemClickListener() {
            @Override
            public void onApprove(String docId, int position) {
                markAsReceived(docId);
            }

            @Override
            public void onReject(String docId, int position) {
                // Not implemented for delivery in this context
            }
        });
        rvDeliveries.setAdapter(adapter);

        fetchFlatAndLoadDeliveries();
        chipGroupDelivery.setOnCheckedStateChangeListener((group, checkedIds) -> loadDeliveries());
    }

    private void fetchFlatAndLoadDeliveries() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(uid)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        flatNumber = snapshot.child("flatNumber").getValue(String.class);
                        loadDeliveries();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void loadDeliveries() {
        if (flatNumber == null) return;

        Query query = db.collection("visitors")
                .whereEqualTo("flatNumber", flatNumber)
                .whereEqualTo("purpose", "Delivery");

        int checkedId = chipGroupDelivery.getCheckedChipId();
        if (checkedId == R.id.chipDelToday) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            query = query.whereGreaterThanOrEqualTo("timestamp", cal.getTime());
        } else if (checkedId == R.id.chipDelPending) {
            query = query.whereEqualTo("status", "Pending");
        } else if (checkedId == R.id.chipDelReceived) {
            query = query.whereEqualTo("status", "Approved");
        }

        query.orderBy("timestamp", Query.Direction.DESCENDING)
             .get().addOnSuccessListener(value -> {
            deliveryList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                VisitorModel v = doc.toObject(VisitorModel.class);
                if (v != null) {
                    v.setDocId(doc.getId());
                    deliveryList.add(v);
                }
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void markAsReceived(String docId) {
        db.collection("visitors").document(docId)
                .update("status", "Approved", "approvedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> loadDeliveries());
    }
}
