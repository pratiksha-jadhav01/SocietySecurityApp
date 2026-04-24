package com.example.securiodb.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.R;
import com.example.securiodb.adapter.VisitorRequestAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApprovalsFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private List<Map<String, Object>> pendingList = new ArrayList<>();
    private VisitorRequestAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;
    private String flatNo;
    private String filterType = "Visitor"; // Default

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_approvals, container, false);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            flatNo = getArguments().getString("flatNo", "");
            // Support both "purpose" and "type" keys for backward compatibility in arguments
            filterType = getArguments().getString("type", getArguments().getString("purpose", "Visitor"));
        }

        recyclerView = view.findViewById(R.id.recyclerPending);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);
        tvTitle = view.findViewById(R.id.tvTitle);

        if (tvTitle != null) {
            tvTitle.setText(filterType.equalsIgnoreCase("Delivery") ? "Pending Deliveries" : "Visitor Requests");
        }

        adapter = new VisitorRequestAdapter(requireContext(), pendingList,
            (docId, position) -> updateVisitorStatus(docId, "Approved"),
            (docId, position) -> updateVisitorStatus(docId, "Rejected")
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        loadPendingRequests();
        return view;
    }

    private void loadPendingRequests() {
        if (flatNo == null || flatNo.isEmpty()) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);

        // We filter by flatNumber OR flatNo using multiple queries if needed, 
        // but here we'll try to use the most common one and handle the other via code or unified schema.
        // For now, let's use the field we've standardized on: flatNumber
        
        Query query = db.collection("visitors")
            .whereEqualTo("flatNumber", flatNo)
            .whereEqualTo("status", "Pending")
            .orderBy("timestamp", Query.Direction.DESCENDING);

        listenerReg = query.addSnapshotListener((snapshots, error) -> {
            if (!isAdded()) return;
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            
            if (error != null) {
                return;
            }
            
            pendingList.clear();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        // Client-side filtering for entryType to allow for flexibility with "Visitor" vs "Delivery"
                        String entryType = (String) data.get("entryType");
                        if (entryType == null) entryType = (String) data.get("purpose"); // fallback
                        
                        if (filterType.equalsIgnoreCase("Delivery")) {
                            if ("Delivery".equalsIgnoreCase(entryType)) {
                                data.put("docId", doc.getId());
                                pendingList.add(data);
                            }
                        } else {
                            // If filtering for Visitors, show everything that isn't Delivery
                            if (!"Delivery".equalsIgnoreCase(entryType)) {
                                data.put("docId", doc.getId());
                                pendingList.add(data);
                            }
                        }
                    }
                }
            }
            adapter.notifyDataSetChanged();
            if (layoutEmpty != null) layoutEmpty.setVisibility(pendingList.isEmpty() ? View.VISIBLE : View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(pendingList.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void updateVisitorStatus(String docId, String newStatus) {
        String ownerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> update = new HashMap<>();
        update.put("status", newStatus);
        update.put("approvedBy", ownerUid);
        update.put("approvedAt", FieldValue.serverTimestamp());

        db.collection("visitors").document(docId)
            .update(update)
            .addOnSuccessListener(v -> {
                if (!isAdded()) return;
                String msg = newStatus.equals("Approved") ? "✅ Approved" : "❌ Rejected";
                Snackbar.make(recyclerView, msg, Snackbar.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerReg != null) listenerReg.remove();
    }
}
