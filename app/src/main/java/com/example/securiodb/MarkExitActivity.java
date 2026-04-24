package com.example.securiodb;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.StatusAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Activity for guards to mark the exit time of visitors and helpers.
 * Shows all individuals currently inside the premises.
 */
public class MarkExitActivity extends AppCompatActivity implements StatusAdapter.OnExitClickListener {

    private RecyclerView rvMarkExit;
    private SearchView searchView;
    private TextView tvEmpty;
    private StatusAdapter adapter;
    private List<Map<String, Object>> allInsideEntries = new ArrayList<>();
    
    private FirebaseFirestore db;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_exit);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupRealtimeListener();
        setupSearch();
    }

    private void initViews() {
        rvMarkExit = findViewById(R.id.rvMarkExit);
        searchView = findViewById(R.id.searchView);
        tvEmpty = findViewById(R.id.tvEmpty);
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new StatusAdapter(new ArrayList<>(), this);
        rvMarkExit.setLayoutManager(new LinearLayoutManager(this));
        rvMarkExit.setAdapter(adapter);
    }

    /**
     * Listens for all individuals (Visitors/Helpers) who are currently inside.
     * Note: Removed 'createdBy' filter so any guard on duty can mark any person's exit.
     */
    private void setupRealtimeListener() {
        // Query for everyone currently inside (Approved but no exit time recorded)
        registration = db.collection("visitors")
                .whereEqualTo("status", "Approved")
                .whereEqualTo("exitTime", null)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("MarkExit", "Listen failed", error);
                        return;
                    }
                    if (value != null) {
                        allInsideEntries.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("id", doc.getId());
                                allInsideEntries.add(data);
                            }
                        }
                        filterList(searchView.getQuery().toString());
                    }
                });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });
    }

    /**
     * Filters the currently inside list by name or flat number.
     */
    private void filterList(String query) {
        List<Map<String, Object>> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        if (lowerQuery.isEmpty()) {
            filteredList.addAll(allInsideEntries);
        } else {
            for (Map<String, Object> entry : allInsideEntries) {
                String name = getStr(entry, "name").toLowerCase();
                String flat = getStr(entry, "flatNumber").toLowerCase();
                if (flat.isEmpty()) flat = getStr(entry, "flatNo").toLowerCase();

                if (name.contains(lowerQuery) || flat.contains(lowerQuery)) {
                    filteredList.add(entry);
                }
            }
        }

        adapter.updateList(filteredList);
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val != null) ? String.valueOf(val) : "";
    }

    @Override
    public void onExitClick(String docId) {
        db.collection("visitors").document(docId)
                .update("exitTime", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Exit marked successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to mark exit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDeleteClick(String docId) {
        db.collection("visitors").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
