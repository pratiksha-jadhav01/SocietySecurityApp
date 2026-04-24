package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securiodb.adapter.BillAdapter;
import com.example.securiodb.models.Bill;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class OwnerBillsActivity extends AppCompatActivity {

    private RecyclerView rvBills;
    private BillAdapter adapter;
    private List<Bill> billList = new ArrayList<>();
    private List<Bill> filteredList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private String flatNo;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_bills);

        db = FirebaseFirestore.getInstance();
        flatNo = getIntent().getStringExtra("flatNo");

        rvBills = findViewById(R.id.rvBills);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tabLayout = findViewById(R.id.tabLayout);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvBills.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BillAdapter(this, filteredList, false, bill -> {
            // Future "Pay Now" implementation
            Toast.makeText(this, "Payment integration coming soon", Toast.LENGTH_SHORT).show();
        });
        rvBills.setAdapter(adapter);

        if (flatNo == null || flatNo.isEmpty()) {
            fetchUserFlatAndBills();
        } else {
            fetchBills();
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterBills(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void fetchUserFlatAndBills() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                flatNo = doc.getString("flatNumber");
                if (flatNo == null) flatNo = doc.getString("flatNo");
                fetchBills();
            }
        });
    }

    private void fetchBills() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("bills")
                .whereEqualTo("flatNo", flatNo)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) return;

                    billList.clear();
                    if (value != null) {
                        billList.addAll(value.toObjects(Bill.class));
                    }
                    filterBills(tabLayout.getSelectedTabPosition());
                });
    }

    private void filterBills(int position) {
        filteredList.clear();
        for (Bill bill : billList) {
            if (position == 0) {
                filteredList.add(bill);
            } else if (position == 1 && "due".equalsIgnoreCase(bill.getStatus())) {
                filteredList.add(bill);
            } else if (position == 2 && "paid".equalsIgnoreCase(bill.getStatus())) {
                filteredList.add(bill);
            }
        }
        
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
