package com.example.securiodb;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.NoticeAdapter;
import com.example.securiodb.models.NoticeModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for Administrators to post new notices and view recent ones.
 * Uses Firebase Firestore to manage society notice data.
 */
public class AdminNoticeActivity extends AppCompatActivity {

    private TextInputLayout tilTitle, tilMessage;
    private TextInputEditText etTitle, etMessage;
    private MaterialButton btnPostNotice;
    private RecyclerView recyclerNotices;
    private View layoutEmpty;

    private FirebaseFirestore db;
    private ListenerRegistration noticeListener;
    private List<NoticeModel> noticeList = new ArrayList<>();
    private NoticeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notice);

        db = FirebaseFirestore.getInstance();

        // Bind UI components
        tilTitle = findViewById(R.id.tilTitle);
        tilMessage = findViewById(R.id.tilMessage);
        etTitle = findViewById(R.id.etTitle);
        etMessage = findViewById(R.id.etMessage);
        btnPostNotice = findViewById(R.id.btnPostNotice);
        recyclerNotices = findViewById(R.id.recyclerNotices);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        // Setup RecyclerView with delete listener
        adapter = new NoticeAdapter(this, noticeList, this::showDeleteConfirmation);
        recyclerNotices.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotices.setAdapter(adapter);

        // Back button navigation
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Set listener for the post button
        btnPostNotice.setOnClickListener(v -> validateAndPost());

        // Load existing notices
        startNoticeListener();
    }

    private void showDeleteConfirmation(NoticeModel notice) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to delete this notice?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNotice(notice))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNotice(NoticeModel notice) {
        if (notice.getNoticeId() == null) return;

        db.collection("notices").document(notice.getNoticeId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(AdminNoticeActivity.this, "Notice deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(AdminNoticeActivity.this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Listen for real-time updates in the 'notices' collection.
     */
    private void startNoticeListener() {
        noticeListener = db.collection("notices")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    return;
                }

                if (snapshots == null) return;

                noticeList.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String id = doc.getId();
                    String title = doc.getString("title");
                    String message = doc.getString("message");
                    String createdBy = doc.getString("createdBy");
                    
                    Timestamp ts = doc.getTimestamp("timestamp");
                    long tsMillis = (ts != null) ? ts.toDate().getTime() : 0;

                    noticeList.add(new NoticeModel(id, title, message, createdBy, tsMillis));
                }

                if (noticeList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerNotices.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerNotices.setVisibility(View.VISIBLE);
                }
                
                adapter.notifyDataSetChanged();
            });
    }

    /**
     * Validates user input and proceeds to post notice if valid.
     */
    private void validateAndPost() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title)) {
            tilTitle.setError("Notice title is required");
            return;
        } else {
            tilTitle.setError(null);
        }

        if (TextUtils.isEmpty(message)) {
            tilMessage.setError("Notice message is required");
            return;
        } else {
            tilMessage.setError(null);
        }

        btnPostNotice.setEnabled(false);
        btnPostNotice.setText("Posting...");

        postNoticeToFirestore(title, message);
    }

    private void postNoticeToFirestore(String title, String message) {
        Map<String, Object> notice = new HashMap<>();
        notice.put("title", title);
        notice.put("message", message);
        notice.put("createdBy", "admin");
        notice.put("timestamp", FieldValue.serverTimestamp());

        db.collection("notices")
            .add(notice)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(this, "Notice posted successfully!", Toast.LENGTH_SHORT).show();
                etTitle.setText("");
                etMessage.setText("");
                btnPostNotice.setEnabled(true);
                btnPostNotice.setText("POST NOTICE");
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnPostNotice.setEnabled(true);
                btnPostNotice.setText("POST NOTICE");
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (noticeListener != null) {
            noticeListener.remove();
        }
    }
}
