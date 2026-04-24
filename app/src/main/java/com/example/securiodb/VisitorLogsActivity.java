package com.example.securiodb;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.models.Visitor;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class VisitorLogsActivity extends AppCompatActivity {

    private static final String TAG = "VisitorLogsActivity";
    private RecyclerView rvVisitors;
    private ProgressBar progressBar;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupTime;
    
    private FirebaseFirestore db;
    private VisitorAdapter adapter;
    private List<Visitor> visitorList = new ArrayList<>();
    private List<Visitor> filteredList = new ArrayList<>();
    private ListenerRegistration logsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_logs);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupFilters();
        
        // Initial load (Today)
        loadLogs("today");
    }

    private void initViews() {
        rvVisitors = findViewById(R.id.rvVisitors);
        progressBar = findViewById(R.id.progressBar);
        etSearch = findViewById(R.id.etSearch);
        chipGroupTime = findViewById(R.id.chipGroupTime);

        if (findViewById(R.id.toolbar) != null) {
            findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        rvVisitors.setLayoutManager(new LinearLayoutManager(this));
        // role "admin" to see all logs
        // Using VisitorAdapter from the same package which matches the constructor arguments
        adapter = new VisitorAdapter(filteredList, "admin", null); 
        rvVisitors.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupTime.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipToday) loadLogs("today");
            else if (checkedId == R.id.chipWeek) loadLogs("week");
            else if (checkedId == R.id.chipMonth) loadLogs("month");
            else if (checkedId == R.id.chipAll) loadLogs("all");
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLogs(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadLogs(String timeFrame) {
        if (logsListener != null) {
            logsListener.remove();
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        Query query = db.collection("visitors")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (!"all".equals(timeFrame)) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if ("week".equals(timeFrame)) {
                cal.add(Calendar.DAY_OF_YEAR, -7);
            } else if ("month".equals(timeFrame)) {
                cal.add(Calendar.MONTH, -1);
            }
            query = query.whereGreaterThanOrEqualTo("timestamp", new Timestamp(cal.getTime()));
        }

        logsListener = query.addSnapshotListener((snapshots, e) -> {
            progressBar.setVisibility(View.GONE);
            if (e != null) {
                Log.e(TAG, "Listen failed", e);
                return;
            }

            visitorList.clear();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Visitor visitor = doc.toObject(Visitor.class);
                    if (visitor != null) {
                        visitor.setVisitorId(doc.getId());
                        visitorList.add(visitor);
                    }
                }
            }
            filterLogs(etSearch.getText().toString());
        });
    }

    private void filterLogs(String query) {
        filteredList.clear();
        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(visitorList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Visitor v : visitorList) {
                if ((v.getName() != null && v.getName().toLowerCase().contains(lowerQuery)) || 
                    (v.getFlatNumber() != null && v.getFlatNumber().toLowerCase().contains(lowerQuery))) {
                    filteredList.add(v);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (logsListener != null) {
            logsListener.remove();
        }
    }
}
