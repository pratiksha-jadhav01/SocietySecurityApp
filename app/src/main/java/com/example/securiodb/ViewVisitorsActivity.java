package com.example.securiodb;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.VisitorAdapter;
import com.example.securiodb.models.VisitorModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ViewVisitorsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private VisitorAdapter adapter;
    private List<VisitorModel> fullList = new ArrayList<>();
    private List<VisitorModel> filteredList = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;
    private TextView tvCount, chipAll, chipPending, chipApproved, chipRejected;
    private LinearLayout layoutEmpty;
    private String currentFilter = "All";
    private String flatNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_visitors);

        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        recycler      = findViewById(R.id.recyclerVisitors);
        tvCount       = findViewById(R.id.tvCount);
        layoutEmpty   = findViewById(R.id.layoutEmpty);
        chipAll       = findViewById(R.id.chipAll);
        chipPending   = findViewById(R.id.chipPending);
        chipApproved  = findViewById(R.id.chipApproved);
        chipRejected  = findViewById(R.id.chipRejected);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Initialize adapter with filter list and approve/reject callbacks
        adapter = new VisitorAdapter(this, filteredList, new VisitorAdapter.OnItemClickListener() {
            @Override
            public void onApprove(String docId, int position) {
                updateStatus(docId, "Approved");
            }

            @Override
            public void onReject(String docId, int position) {
                updateStatus(docId, "Rejected");
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        setupChips();
        fetchFlatAndStartListener();
    }

    private void fetchFlatAndStartListener() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(uid)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        flatNumber = snapshot.child("flatNumber").getValue(String.class);
                        if (flatNumber != null) {
                            startRealtimeListener();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void startRealtimeListener() {
        if (flatNumber == null) return;

        listenerReg = db.collection("visitors")
            .whereEqualTo("flatNumber", flatNumber)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (snapshots == null) return;
                
                fullList.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    VisitorModel v = doc.toObject(VisitorModel.class);
                    if (v != null) {
                        v.setDocId(doc.getId());
                        fullList.add(v);
                    }
                }
                
                applyFilter(currentFilter);
                
                long pending = 0;
                for (VisitorModel v : fullList) {
                    if ("Pending".equalsIgnoreCase(v.getStatus())) pending++;
                }
                tvCount.setText(pending + " pending");
            });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filteredList.clear();
        for (VisitorModel v : fullList) {
            if (filter.equalsIgnoreCase("All") || filter.equalsIgnoreCase(v.getStatus())) {
                filteredList.add(v);
            }
        }
        adapter.notifyDataSetChanged();
        layoutEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(filteredList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateStatus(String docId, String status) {
        db.collection("visitors").document(docId)
            .update("status", status)
            .addOnSuccessListener(v ->
                Toast.makeText(this, "Status updated: " + status, Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupChips() {
        TextView[] chips = {chipAll, chipPending, chipApproved, chipRejected};
        String[] filters = {"All", "Pending", "Approved", "Rejected"};
        
        for (int i = 0; i < chips.length; i++) {
            final String f = filters[i];
            chips[i].setOnClickListener(v -> {
                for (TextView c : chips) {
                    c.setBackgroundResource(R.drawable.chip_normal);
                    c.setTextColor(ContextCompat.getColor(this, R.color.soft_brown));
                }
                ((TextView) v).setBackgroundResource(R.drawable.chip_selected);
                ((TextView) v).setTextColor(Color.WHITE);
                applyFilter(f);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerReg != null) listenerReg.remove();
    }
}
