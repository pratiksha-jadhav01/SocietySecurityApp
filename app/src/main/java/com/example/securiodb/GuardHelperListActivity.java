package com.example.securiodb;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GuardHelperListActivity extends AppCompatActivity {

    private EditText etSearchFlat;
    private TextView tvHelperCount;
    private RecyclerView recycler;
    private LinearLayout layoutEmpty;
    private List<Map<String, Object>> allHelpers  = new ArrayList<>();
    private List<Map<String, Object>> shownHelpers = new ArrayList<>();
    private GuardHelperAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guard_helper_list);

        db            = FirebaseFirestore.getInstance();
        etSearchFlat  = findViewById(R.id.etSearchFlat);
        tvHelperCount = findViewById(R.id.tvHelperCount);
        recycler      = findViewById(R.id.recyclerHelpers);
        layoutEmpty   = findViewById(R.id.layoutEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new GuardHelperAdapter(this, shownHelpers);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        loadAllActiveHelpers();

        // Search by flat number as guard types
        etSearchFlat.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                filterByFlat(s.toString().trim().toUpperCase());
            }
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Loads all active daily helpers from Firestore.
     * Uses a real-time listener to keep the list updated.
     */
    private void loadAllActiveHelpers() {
        // Load ALL active helpers across all flats. 
        // Removed orderBy("flatNo") because it excludes documents that use "flatNumber" field name.
        db.collection("dailyHelpers")
            .whereEqualTo("isActive", true)
            .addSnapshotListener((snap, e) -> {
                if (e != null) {
                    Log.e("GuardHelperList", "Listen failed.", e);
                    return;
                }
                if (snap == null) return;
                
                allHelpers.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("docId", doc.getId());
                        allHelpers.add(data);
                    }
                }
                
                // Sort by flat in memory to handle both field names
                Collections.sort(allHelpers, (a, b) -> {
                    String f1 = getFlat(a);
                    String f2 = getFlat(b);
                    return f1.compareTo(f2);
                });

                // Apply current search filter
                String query = etSearchFlat.getText().toString().trim().toUpperCase();
                filterByFlat(query);
                
                if (tvHelperCount != null) {
                    tvHelperCount.setText(allHelpers.size() + " active helpers registered");
                }
            });
    }

    private String getFlat(Map<String, Object> h) {
        String flat = (String) h.get("flatNumber");
        if (flat == null) flat = (String) h.get("flatNo");
        return flat != null ? flat : "";
    }

    /**
     * Filters the helper list based on the flat number entered in the search box.
     * @param query The flat number search string.
     */
    private void filterByFlat(String query) {
        shownHelpers.clear();
        for (Map<String, Object> h : allHelpers) {
            String flat = getFlat(h);
            if (query.isEmpty() || (flat != null && flat.toUpperCase().contains(query))) {
                shownHelpers.add(h);
            }
        }
        adapter.notifyDataSetChanged();
        
        boolean empty = shownHelpers.isEmpty();
        if (layoutEmpty != null) layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recycler != null) recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
