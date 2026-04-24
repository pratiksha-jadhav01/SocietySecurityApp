package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.BillHistoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BillHistoryActivity extends AppCompatActivity {

    private RecyclerView rvBillHistory;
    private TextView tvNoBills;
    private BillHistoryAdapter adapter;
    private List<BillHistoryAdapter.BillItem> billList = new ArrayList<>();
    private DatabaseReference maintenanceRef, statusRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_history);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        userId = user.getUid();

        rvBillHistory = findViewById(R.id.rvBillHistory);
        tvNoBills = findViewById(R.id.tvNoBills);
        
        if (findViewById(R.id.toolbar) != null) {
            findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        }

        rvBillHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BillHistoryAdapter(billList);
        rvBillHistory.setAdapter(adapter);

        maintenanceRef = FirebaseDatabase.getInstance().getReference("maintenance");
        statusRef = FirebaseDatabase.getInstance().getReference("status").child(userId);

        fetchBills();
    }

    private void fetchBills() {
        maintenanceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot maintenanceSnapshot) {
                billList.clear();
                if (!maintenanceSnapshot.exists()) {
                    updateUI();
                    return;
                }

                long totalCount = maintenanceSnapshot.getChildrenCount();
                final int[] fetchedCount = {0};

                for (DataSnapshot snapshot : maintenanceSnapshot.getChildren()) {
                    String monthKey = snapshot.getKey();
                    Object amtObj = snapshot.child("amount").getValue();
                    Object dueObj = snapshot.child("dueDate").getValue();
                    
                    String amount = amtObj != null ? "₹ " + amtObj.toString() : "₹ 0";
                    String dueDate = dueObj != null ? dueObj.toString() : "N/A";

                    statusRef.child(monthKey).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot statusSnapshot) {
                            String status = "Unpaid";
                            if (statusSnapshot.exists()) {
                                String s = statusSnapshot.child("status").getValue(String.class);
                                if (s != null) status = s;
                            }
                            
                            billList.add(new BillHistoryAdapter.BillItem(monthKey.replace("_", " "), amount, dueDate, status));
                            fetchedCount[0]++;
                            
                            if (fetchedCount[0] == totalCount) {
                                adapter.notifyDataSetChanged();
                                updateUI();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            fetchedCount[0]++;
                            if (fetchedCount[0] == totalCount) {
                                adapter.notifyDataSetChanged();
                                updateUI();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BillHistoryActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (billList.isEmpty()) {
            tvNoBills.setVisibility(View.VISIBLE);
            rvBillHistory.setVisibility(View.GONE);
        } else {
            tvNoBills.setVisibility(View.GONE);
            rvBillHistory.setVisibility(View.VISIBLE);
        }
    }
}
