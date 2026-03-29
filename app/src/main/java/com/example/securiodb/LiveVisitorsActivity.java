package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.VisitorAdapter;
import com.example.securiodb.models.Visitor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class LiveVisitorsActivity extends AppCompatActivity {

    private RecyclerView rvLiveVisitors;
    private TextView tvLiveCount;
    
    private FirebaseFirestore db;
    private String flatNumber;
    private List<Visitor> liveList = new ArrayList<>();
    private VisitorAdapter adapter;
    private ListenerRegistration liveListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_visitors);

        db = FirebaseFirestore.getInstance();
        rvLiveVisitors = findViewById(R.id.rvLiveVisitors);
        tvLiveCount = findViewById(R.id.tvLiveCount);

        rvLiveVisitors.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VisitorAdapter(liveList);
        rvLiveVisitors.setAdapter(adapter);

        fetchFlatAndStartLiveListener();
    }

    private void fetchFlatAndStartLiveListener() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            flatNumber = doc.getString("flatNumber");
            startLiveListener();
        });
    }

    private void startLiveListener() {
        liveListener = db.collection("visitors")
                .whereEqualTo("flatNumber", flatNumber)
                .whereEqualTo("status", "Approved")
                .whereEqualTo("exitTime", null)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    
                    liveList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        liveList.add(doc.toObject(Visitor.class));
                    }
                    adapter.notifyDataSetChanged();
                    tvLiveCount.setText(liveList.size() + " People");
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (liveListener != null) liveListener.remove();
    }
}
