package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.AdminBillAdapter;
import com.example.securiodb.models.AdminBillItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminMaintenanceActivity extends AppCompatActivity {
    private RecyclerView rvAdminMaintenance;
    private TextView tvEmpty;
    private AdminBillAdapter adminAdapter;
    private List<AdminBillItem> adminList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_maintenance);

        db = FirebaseFirestore.getInstance();
        rvAdminMaintenance = findViewById(R.id.rvAdminMaintenance);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        if (rvAdminMaintenance != null) {
            rvAdminMaintenance.setLayoutManager(new LinearLayoutManager(this));
            adminAdapter = new AdminBillAdapter(this, adminList);
            rvAdminMaintenance.setAdapter(adminAdapter);
        }

        fetchFirestoreBills();
    }

    private void fetchFirestoreBills() {
        db.collection("bills")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (value == null) return;
                adminList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    String docId = doc.getId();
                    String flat = doc.getString("flatNo");
                    String month = doc.getString("month");
                    String status = doc.getString("status");
                    
                    // Pass docId to the constructor
                    adminList.add(new AdminBillItem(docId, flat, month, status));
                }
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(adminList.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (adminAdapter != null) {
                    adminAdapter.notifyDataSetChanged();
                }
            });
    }
}
