package com.example.securiodb;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MarkExitActivity extends AppCompatActivity implements StatusAdapter.OnExitClickListener {

    private RecyclerView rvMarkExit;
    private SearchView searchView;
    private TextView tvEmpty;
    private StatusAdapter adapter;
    private List<Map<String, Object>> allInsideEntries = new ArrayList<>();
    
    private FirebaseFirestore db;
    private String guardUid;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_exit);

        db = FirebaseFirestore.getInstance();
        guardUid = FirebaseAuth.getInstance().getUid();

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

    private void setupRealtimeListener() {
        registration = db.collection("visitors")
                .whereEqualTo("createdBy", guardUid)
                .whereEqualTo("status", "Approved")
                .whereEqualTo("exitTime", null)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
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

    private void filterList(String query) {
        List<Map<String, Object>> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        if (lowerQuery.isEmpty()) {
            filteredList.addAll(allInsideEntries);
        } else {
            for (Map<String, Object> entry : allInsideEntries) {
                String name = ((String) entry.get("name")).toLowerCase();
                String flat = ((String) entry.get("flatNumber")).toLowerCase();
                if (name.contains(lowerQuery) || flat.contains(lowerQuery)) {
                    filteredList.add(entry);
                }
            }
        }

        adapter.updateList(filteredList);
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onExitClick(String docId) {
        db.collection("visitors").document(docId)
                .update("exitTime", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Exit marked", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
