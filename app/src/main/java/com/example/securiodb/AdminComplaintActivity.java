package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.AdminComplaintAdapter;
import com.example.securiodb.models.ComplaintModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for Admin to view and manage all complaints raised by owners.
 * Uses Firebase Realtime Database.
 */
public class AdminComplaintActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private TextView tvEmpty;
    private List<ComplaintModel> complaintList = new ArrayList<>();
    private AdminComplaintAdapter adapter;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_complaint);

        // Initialize Firebase Realtime Database
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Bind Views
        recycler = findViewById(R.id.recyclerAdminComplaints);
        tvEmpty  = findViewById(R.id.tvEmpty);

        // Setup RecyclerView with callback for resolving complaints
        adapter = new AdminComplaintAdapter(this, complaintList, this::resolveComplaint);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        // Setup Toolbar back button
        if (findViewById(R.id.toolbar) != null) {
            findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        }

        // Start listening to all complaints
        loadAllComplaints();
    }

    /**
     * Loads all complaints from the 'complaints' node.
     * Listens for real-time changes across the society.
     */
    private void loadAllComplaints() {
        dbRef.child("complaints")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    complaintList.clear();

                    if (!snapshot.exists()) {
                        updateUI(true);
                        return;
                    }

                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.getKey() == null) continue;

                        // Safely map snapshot to model with null checks
                        ComplaintModel m = new ComplaintModel();
                        m.setComplaintId(child.getKey());

                        Object uid = child.child("userId").getValue();
                        m.setUserId(uid != null ? uid.toString() : "Unknown");

                        Object title = child.child("title").getValue();
                        m.setTitle(title != null ? title.toString() : "No Title");

                        Object desc = child.child("description").getValue();
                        m.setDescription(desc != null ? desc.toString() : "");

                        Object status = child.child("status").getValue();
                        m.setStatus(status != null ? status.toString() : "Pending");

                        Object resp = child.child("response").getValue();
                        m.setResponse(resp != null ? resp.toString() : "");

                        Object time = child.child("timestamp").getValue();
                        if (time instanceof Long) {
                            m.setTimestamp((Long) time);
                        }

                        complaintList.add(m);
                    }

                    updateUI(complaintList.isEmpty());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(AdminComplaintActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateUI(boolean isEmpty) {
        if (isEmpty) {
            tvEmpty.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Updates the status of a complaint to 'Resolved' in Firebase.
     * Uses updateChildren to preserve existing fields like description and timestamp.
     */
    private void resolveComplaint(String complaintId, int position) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", "Resolved");
        update.put("response", "Your issue has been reviewed and resolved by admin.");

        dbRef.child("complaints").child(complaintId)
            .updateChildren(update)
            .addOnSuccessListener(v -> Toast.makeText(this, "Complaint marked as Resolved", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Refresh the specific item to re-enable the button if update failed
                adapter.notifyItemChanged(position);
            });
    }
}
