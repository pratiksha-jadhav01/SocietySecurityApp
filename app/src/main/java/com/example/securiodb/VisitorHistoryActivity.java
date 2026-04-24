package com.example.securiodb;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class VisitorHistoryActivity extends AppCompatActivity {

    private static final String TAG = "VisitorHistoryActivity";
    private RecyclerView rvHistory;
    private ChipGroup chipGroupDate, chipGroupStatus;
    
    private FirebaseFirestore db;
    private String flatNumber;
    private List<VisitorModel> visitorList = new ArrayList<>();
    private VisitorAdapter adapter;
    private ListenerRegistration historyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_history);

        db = FirebaseFirestore.getInstance();
        rvHistory = findViewById(R.id.rvHistory);
        chipGroupDate = findViewById(R.id.chipGroupDate);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);

        if (findViewById(R.id.toolbar) != null) {
            findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        }

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VisitorAdapter(this, visitorList, null);
        rvHistory.setAdapter(adapter);

        fetchFlatAndLoadHistory();
        setupFilters();
    }

    private void fetchFlatAndLoadHistory() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Using Firestore as the primary source of truth for user profile
        db.collection("users").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    flatNumber = doc.getString("flatNumber");
                    if (flatNumber == null) flatNumber = doc.getString("flatNo");
                    
                    if (flatNumber != null) {
                        loadHistory();
                    } else {
                        Toast.makeText(this, "Flat number not found in profile", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user profile", e);
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            });
    }

    private void setupFilters() {
        chipGroupDate.setOnCheckedStateChangeListener((group, checkedIds) -> loadHistory());
        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> loadHistory());
    }

    private void loadHistory() {
        if (flatNumber == null) return;

        if (historyListener != null) {
            historyListener.remove();
        }

        Query query = db.collection("visitors")
                .whereEqualTo("flatNumber", flatNumber);

        // Date Filter
        int dateId = chipGroupDate.getCheckedChipId();
        if (dateId != R.id.chipAllDates) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); 
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
            
            if (dateId == R.id.chipToday) {
                query = query.whereGreaterThanOrEqualTo("timestamp", cal.getTime());
            } else if (dateId == R.id.chipWeek) {
                cal.add(Calendar.DAY_OF_YEAR, -7);
                query = query.whereGreaterThanOrEqualTo("timestamp", cal.getTime());
            } else if (dateId == R.id.chipMonth) {
                cal.add(Calendar.MONTH, -1);
                query = query.whereGreaterThanOrEqualTo("timestamp", cal.getTime());
            }
        }

        // Status Filter
        int statusId = chipGroupStatus.getCheckedChipId();
        if (statusId == R.id.chipApproved) {
            query = query.whereEqualTo("status", "Approved");
        } else if (statusId == R.id.chipRejected) {
            query = query.whereEqualTo("status", "Rejected");
        }

        // Sort by timestamp
        query = query.orderBy("timestamp", Query.Direction.DESCENDING);

        historyListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "History listener failed: " + error.getMessage(), error);
                // If it fails with "index required", we might need to simplify or notify
                if (error.getMessage().contains("index")) {
                    Toast.makeText(this, "Filtering requires index. Check logs.", Toast.LENGTH_LONG).show();
                }
                return;
            }

            if (value != null) {
                visitorList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    VisitorModel v = doc.toObject(VisitorModel.class);
                    if (v != null) {
                        v.setDocId(doc.getId());
                        visitorList.add(v);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyListener != null) {
            historyListener.remove();
        }
    }
}
