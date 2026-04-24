package com.example.securiodb.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.R;
import com.example.securiodb.adapter.BillAdapter;
import com.example.securiodb.models.Bill;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceFragment extends Fragment {

    private String flatNo;
    private FirebaseFirestore db;
    private TextView tvTotalDue, tvUnpaidCount, tvPaidCount;
    private RecyclerView recycler;
    private List<Bill> billList = new ArrayList<>();
    private BillAdapter adapter;
    private ListenerRegistration billListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
            R.layout.fragment_maintenance, container, false);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null)
            flatNo = getArguments().getString("flatNo", "");

        tvTotalDue    = view.findViewById(R.id.tvTotalDue);
        tvUnpaidCount = view.findViewById(R.id.tvUnpaidCount);
        tvPaidCount   = view.findViewById(R.id.tvPaidCount);
        recycler      = view.findViewById(R.id.recyclerBills);

        adapter = new BillAdapter(requireContext(), billList, false, bill -> {
            // Handle action if needed
        });
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        loadBills();
        return view;
    }

    private void loadBills() {
        if (flatNo == null || flatNo.isEmpty()) return;

        // Note: The collection name was "maintenance" in the previous version, 
        // but OwnerBillsActivity uses "bills". I'll keep "bills" for consistency 
        // if they are meant to be the same, but the fragment had "maintenance".
        // Looking at the previous code, it was using "maintenance".
        // I will stick to "bills" if that's the new standard, or check which one is correct.
        // OwnerBillsActivity: db.collection("bills").whereEqualTo("flatNo", flatNo)
        
        billListener = db.collection("bills")
            .whereEqualTo("flatNo", flatNo)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                
                billList.clear();
                int unpaid = 0, paid = 0;
                double totalDue = 0;

                List<Bill> fetchedBills = snap.toObjects(Bill.class);
                for (Bill bill : fetchedBills) {
                    billList.add(bill);
                    if ("paid".equalsIgnoreCase(bill.getStatus())) {
                        paid++;
                    } else {
                        unpaid++;
                        totalDue += bill.getAmount();
                    }
                }

                adapter.notifyDataSetChanged();

                // Update header summary
                tvTotalDue.setText("₹" + (int) totalDue);
                tvUnpaidCount.setText(String.valueOf(unpaid));
                tvPaidCount.setText(String.valueOf(paid));
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (billListener != null) billListener.remove();
    }
}
