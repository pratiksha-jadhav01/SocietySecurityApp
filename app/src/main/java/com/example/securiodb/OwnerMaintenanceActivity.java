package com.example.securiodb;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.BillAdapter;
import com.example.securiodb.models.Bill;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class OwnerMaintenanceActivity extends AppCompatActivity {

    private TextView tvMonth, tvAmount, tvDueDate, tvStatus;
    private Button btnMarkPaid;
    private RecyclerView rvHistory;
    private BillAdapter historyAdapter;
    private List<Bill> billList = new ArrayList<>();
    
    private FirebaseFirestore db;
    private String flatNo;
    private String currentBillId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_maintenance);

        db = FirebaseFirestore.getInstance();
        
        tvMonth = findViewById(R.id.tvMonth);
        tvAmount = findViewById(R.id.tvAmount);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvStatus = findViewById(R.id.tvStatus);
        btnMarkPaid = findViewById(R.id.btnMarkPaid);
        rvHistory = findViewById(R.id.rvMaintenanceHistory);

        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            historyAdapter = new BillAdapter(this, billList, false, null);
            rvHistory.setAdapter(historyAdapter);
        }

        fetchUserAndBills();

        btnMarkPaid.setOnClickListener(v -> markAsPaid());
    }

    private void fetchUserAndBills() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                flatNo = doc.getString("flatNumber");
                if (flatNo == null) flatNo = doc.getString("flatNo");
                loadBills();
            }
        });
    }

    private void loadBills() {
        if (flatNo == null) return;

        db.collection("bills")
            .whereEqualTo("flatNo", flatNo)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (value == null) return;

                billList.clear();
                boolean currentSet = false;

                for (DocumentSnapshot doc : value.getDocuments()) {
                    Bill bill = doc.toObject(Bill.class);
                    if (bill == null) continue;
                    billList.add(bill);

                    if (!currentSet && "due".equalsIgnoreCase(bill.getStatus())) {
                        currentBillId = doc.getId();
                        tvMonth.setText(bill.getMonth());
                        tvAmount.setText(String.format("₹ %.0f", bill.getAmount()));
                        tvDueDate.setText(bill.getDueDate());
                        tvStatus.setText("UNPAID");
                        tvStatus.setTextColor(Color.RED);
                        btnMarkPaid.setVisibility(View.VISIBLE);
                        currentSet = true;
                    }
                }

                if (!currentSet && !billList.isEmpty()) {
                    Bill latest = billList.get(0);
                    tvMonth.setText(latest.getMonth());
                    tvAmount.setText(String.format("₹ %.0f", latest.getAmount()));
                    tvDueDate.setText(latest.getDueDate());
                    tvStatus.setText("PAID");
                    tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                    btnMarkPaid.setVisibility(View.GONE);
                }

                if (historyAdapter != null) historyAdapter.notifyDataSetChanged();
            });
    }

    private void markAsPaid() {
        if (currentBillId == null) return;

        // Ensure we update the correct document ID
        db.collection("bills").document(currentBillId)
            .update("status", "Paid")
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Bill Paid Successfully!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> {
                Log.e("OwnerMaintenance", "Error updating status", e);
                Toast.makeText(this, "Permission Denied: Ensure Firestore rules allow writing to bills", Toast.LENGTH_LONG).show();
            });
    }
}
