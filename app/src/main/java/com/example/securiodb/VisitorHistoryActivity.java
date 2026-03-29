package com.example.securiodb;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.VisitorAdapter;
import com.example.securiodb.models.Visitor;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class VisitorHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private ChipGroup chipGroupDate, chipGroupStatus;
    
    private FirebaseFirestore db;
    private String flatNumber;
    private List<Visitor> visitorList = new ArrayList<>();
    private VisitorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_history);

        db = FirebaseFirestore.getInstance();
        rvHistory = findViewById(R.id.rvHistory);
        chipGroupDate = findViewById(R.id.chipGroupDate);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VisitorAdapter(visitorList);
        rvHistory.setAdapter(adapter);

        fetchFlatAndLoadHistory();
        setupFilters();
    }

    private void fetchFlatAndLoadHistory() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(uid)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        flatNumber = snapshot.child("flatNumber").getValue(String.class);
                        loadHistory();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(VisitorHistoryActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void setupFilters() {
        chipGroupDate.setOnCheckedStateChangeListener((group, checkedIds) -> loadHistory());
        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> loadHistory());
    }

    private void loadHistory() {
        if (flatNumber == null) return;

        Query query = db.collection("visitors")
                .whereEqualTo("flatNumber", flatNumber)
                .whereEqualTo("purpose", "Visitor");

        // Date Filter
        int dateId = chipGroupDate.getCheckedChipId();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        
        if (dateId == R.id.chipToday) {
            query = query.whereGreaterThanOrEqualTo("timestamp", cal.getTime());
        } else if (dateId == R.id.chipWeek) {
            cal.add(Calendar.DAY_OF_YEAR, -7);
            query = query.whereGreaterThanOrEqualTo("timestamp", cal.getTime());
        } else if (dateId == R.id.chipMonth) {
            cal.add(Calendar.MONTH, -1);
            query = query.whereGreaterThanOrEqualTo("timestamp", cal.getTime());
        }

        // Status Filter
        int statusId = chipGroupStatus.getCheckedChipId();
        if (statusId == R.id.chipApproved) {
            query = query.whereEqualTo("status", "Approved");
        } else if (statusId == R.id.chipRejected) {
            query = query.whereEqualTo("status", "Rejected");
        }

        // Add sorting after all filters
        query = query.orderBy("timestamp", Query.Direction.DESCENDING);

        query.get().addOnSuccessListener(value -> {
            visitorList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                Visitor v = doc.toObject(Visitor.class);
                if (v != null) {
                    v.setVisitorId(doc.getId());
                    visitorList.add(v);
                }
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            // If it fails due to missing index, sorting might be an issue with combined filters
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
