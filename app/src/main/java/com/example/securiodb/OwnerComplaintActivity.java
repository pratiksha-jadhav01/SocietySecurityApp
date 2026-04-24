package com.example.securiodb;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.ComplaintAdapter;
import com.example.securiodb.models.ComplaintModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for owners to manage their complaints with real-time updates from Firestore.
 */
public class OwnerComplaintActivity extends AppCompatActivity {

    private static final String TAG = "OwnerComplaintActivity";
    private static final String CHANNEL_ID = "complaint_channel";
    private static final String PREF_NAME  = "ComplaintNotifPrefs";

    private RecyclerView recyclerComplaints;
    private TextView tvEmpty;
    private List<ComplaintModel> complaintList = new ArrayList<>();
    private ComplaintAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private SharedPreferences prefs;
    private ListenerRegistration complaintsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_complaint);

        // Authentication check
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        // Initialize Firebase and Preferences
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Bind Views
        recyclerComplaints = findViewById(R.id.recyclerComplaints);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Setup RecyclerView
        adapter = new ComplaintAdapter(this, complaintList);
        recyclerComplaints.setLayoutManager(new LinearLayoutManager(this));
        recyclerComplaints.setAdapter(adapter);

        // Setup Toolbar
        if (findViewById(R.id.toolbar) != null) {
            findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        }

        // Raise Complaint Button
        findViewById(R.id.btnRaiseComplaint).setOnClickListener(v -> showRaiseDialog());

        // Notification Channel setup
        createNotificationChannel();

        // Start listening to complaints
        listenToComplaints();
    }

    /**
     * Listens to the 'complaints' collection in Firestore.
     * Filters complaints for the current user and triggers notifications when status changes.
     */
    private void listenToComplaints() {
        complaintsListener = db.collection("complaints")
                .whereEqualTo("ownerUid", currentUserId)
                .orderBy("raisedOn", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed", e);
                        // Fallback check: if raisedOn ordering failed because of missing index or data, try a simpler query
                        tryAlternativeListen();
                        return;
                    }

                    if (snapshots != null) {
                        processSnapshots(snapshots.getDocuments());
                    }
                });
    }

    private void tryAlternativeListen() {
        // Simple filter without ordering as a fallback
        db.collection("complaints")
                .whereEqualTo("ownerUid", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Alternative listen failed", e);
                        return;
                    }
                    if (snapshots != null) {
                        processSnapshots(snapshots.getDocuments());
                    }
                });
    }

    private void processSnapshots(List<DocumentSnapshot> documents) {
        complaintList.clear();
        for (DocumentSnapshot doc : documents) {
            ComplaintModel m = doc.toObject(ComplaintModel.class);
            if (m != null) {
                m.setComplaintId(doc.getId());
                complaintList.add(m);

                String status = m.getStatus();
                String response = m.getAdminResponse();
                
                // Trigger notification if status is 'Resolved' or 'In Progress' and user hasn't been notified of this specific update
                String statusKey = "status_" + doc.getId() + "_" + status;
                if (!"Pending".equalsIgnoreCase(status) && !prefs.getBoolean(statusKey, false)) {
                    showNotification("Complaint Update: " + status, 
                        (response != null && !response.isEmpty()) 
                        ? "Response: " + response 
                        : "Your complaint '" + m.getTitle() + "' is now " + status + ".");
                    prefs.edit().putBoolean(statusKey, true).apply();
                }
            }
        }
        updateUI(complaintList.isEmpty());
    }

    private void updateUI(boolean isEmpty) {
        if (isEmpty) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerComplaints.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerComplaints.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void showRaiseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Raise Complaint");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("Complaint Title");
        layout.addView(etTitle);

        final EditText etDesc = new EditText(this);
        etDesc.setHint("Describe your issue...");
        etDesc.setMinLines(3);
        layout.addView(etDesc);

        builder.setView(layout);
        builder.setPositiveButton("Submit", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String desc  = etDesc.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(OwnerComplaintActivity.this, "Title is required", Toast.LENGTH_SHORT).show();
                return;
            }
            saveComplaint(title, desc);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveComplaint(String title, String desc) {
        Map<String, Object> data = new HashMap<>();
        data.put("ownerUid",    currentUserId);
        data.put("userId",      currentUserId); // For double compatibility
        data.put("title",       title);
        data.put("description", desc);
        data.put("status",      "Pending");
        data.put("adminResponse", "");
        data.put("response",    "");
        data.put("raisedOn",    System.currentTimeMillis());
        data.put("timestamp",   System.currentTimeMillis());

        db.collection("complaints").add(data)
            .addOnSuccessListener(v -> Toast.makeText(this, "Complaint raised successfully!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.secu_ground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Complaint Updates", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for complaint status updates");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (complaintsListener != null) {
            complaintsListener.remove();
        }
    }
}
