package com.example.securiodb;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.VisitorAdapter;
import com.example.securiodb.models.Visitor;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class VisitorLogsActivity extends AppCompatActivity {

    private RecyclerView rvVisitors;
    private ProgressBar progressBar;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupTime;
    
    private DatabaseReference mDatabase;
    private VisitorAdapter adapter;
    private List<Visitor> visitorList = new ArrayList<>();
    private List<Visitor> filteredList = new ArrayList<>();
    private ValueEventListener logsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_logs);

        mDatabase = FirebaseDatabase.getInstance().getReference();

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

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvVisitors.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VisitorAdapter(filteredList); 
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
            mDatabase.child("visitors").removeEventListener(logsListener);
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        logsListener = mDatabase.child("visitors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                visitorList.clear();
                
                long cutoffTimestamp = 0;
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);

                if ("today".equals(timeFrame)) {
                    cutoffTimestamp = cal.getTimeInMillis();
                } else if ("week".equals(timeFrame)) {
                    cal.add(Calendar.DAY_OF_YEAR, -7);
                    cutoffTimestamp = cal.getTimeInMillis();
                } else if ("month".equals(timeFrame)) {
                    cal.add(Calendar.MONTH, -1);
                    cutoffTimestamp = cal.getTimeInMillis();
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Visitor visitor = ds.getValue(Visitor.class);
                    if (visitor != null && "Visitor".equalsIgnoreCase(visitor.getPurpose())) {
                        Object ts = ds.child("timestamp").getValue();
                        long visitorTs = 0;
                        if (ts instanceof Long) {
                            visitorTs = (Long) ts;
                        }

                        if ("all".equals(timeFrame) || visitorTs >= cutoffTimestamp) {
                            visitorList.add(visitor);
                        }
                    }
                }
                
                // Sort by timestamp descending
                Collections.sort(visitorList, (v1, v2) -> {
                    Object t1 = v1.getTimestamp();
                    Object t2 = v2.getTimestamp();
                    long time1 = (t1 instanceof Long) ? (Long) t1 : 0;
                    long time2 = (t2 instanceof Long) ? (Long) t2 : 0;
                    return Long.compare(time2, time1);
                });

                filterLogs(etSearch.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
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
        if (mDatabase != null && logsListener != null) {
            mDatabase.child("visitors").removeEventListener(logsListener);
        }
    }
}
