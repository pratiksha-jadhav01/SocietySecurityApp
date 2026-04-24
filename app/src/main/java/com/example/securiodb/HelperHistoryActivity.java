package com.example.securiodb;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HelperHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<Map<String, Object>> historyList = new ArrayList<>();
    private FirebaseFirestore db;
    private String helperId, helperName;
    private View layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_history);

        db = FirebaseFirestore.getInstance();
        helperId = getIntent().getStringExtra("HELPER_ID");
        helperName = getIntent().getStringExtra("HELPER_NAME");

        TextView tvTitleName = findViewById(R.id.tvHelperName);
        if (helperName != null) tvTitleName.setText(helperName);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvHistory = findViewById(R.id.rvHistory);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList);
        rvHistory.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        if (helperId == null) return;

        // Query visitors collection where helperDocId matches and order by entryTime
        db.collection("visitors")
            .whereEqualTo("helperDocId", helperId)
            .orderBy("entryTime", Query.Direction.DESCENDING)
            .addSnapshotListener((snap, e) -> {
                if (snap == null) return;
                
                historyList.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("docId", doc.getId());
                        historyList.add(data);
                    }
                }
                
                adapter.notifyDataSetChanged();
                layoutEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
            });
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<Map<String, Object>> list;
        private SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        private SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        HistoryAdapter(List<Map<String, Object>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_helper_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            Map<String, Object> item = list.get(position);
            
            Long entryTime = (Long) item.get("entryTime");
            Long exitTime = (Long) item.get("exitTime");
            String guardId = (String) item.get("createdBy");

            if (entryTime != null) {
                Date d = new Date(entryTime);
                h.tvDate.setText(dateFmt.format(d));
                h.tvEntry.setText("Entry: " + timeFmt.format(d));
            }

            if (exitTime != null && exitTime > 0) {
                h.tvExit.setText("Exit: " + timeFmt.format(new Date(exitTime)));
                h.tvExit.setTextColor(0xFFA07850); // Soft brown
            } else {
                h.tvExit.setText("Exit: Still Inside");
                h.tvExit.setTextColor(0xFFC62828); // Red
            }

            // Fetch guard name - for simplicity here we just show the ID or a placeholder
            // In a real app, you'd fetch the name from a 'users' cache or another query
            h.tvGuard.setText("Recorded by Guard");
            if (guardId != null) {
                FirebaseFirestore.getInstance().collection("users").document(guardId)
                    .get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            h.tvGuard.setText("by Guard: " + doc.getString("name"));
                        }
                    });
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvEntry, tvExit, tvGuard;
            ViewHolder(View v) {
                super(v);
                tvDate = v.findViewById(R.id.tvDate);
                tvEntry = v.findViewById(R.id.tvEntryTime);
                tvExit = v.findViewById(R.id.tvExitTime);
                tvGuard = v.findViewById(R.id.tvGuardName);
            }
        }
    }
}
