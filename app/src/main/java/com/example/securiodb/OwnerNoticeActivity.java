package com.example.securiodb;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.NoticeAdapter;
import com.example.securiodb.models.NoticeModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for Society Owners to view notices.
 * Implements real-time updates and notifications for new notices.
 */
public class OwnerNoticeActivity extends AppCompatActivity {

    private static final String PREF_NAME = "NoticeBoardPrefs";
    private static final String PREF_LAST_ID = "lastNoticeId";

    private RecyclerView recyclerNotices;
    private TextView tvNoticeCount;
    private View layoutEmpty;

    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;
    private List<NoticeModel> noticeList = new ArrayList<>();
    private NoticeAdapter adapter;

    // SharedPreferences to track last seen notice and avoid duplicate notifications
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_notice);

        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Initialize UI components
        recyclerNotices = findViewById(R.id.recyclerNotices);
        tvNoticeCount = findViewById(R.id.tvNoticeCount);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        // Create notification channel for new notices
        NotificationHelper.createChannel(this);

        // Back button navigation
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup RecyclerView with NoticeAdapter
        // Using NoticeAdapter which takes List<NoticeModel>
        adapter = new NoticeAdapter(this, noticeList);
        recyclerNotices.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotices.setAdapter(adapter);

        // Initialize real-time listener for notices
        startNoticeListener();
    }

    /**
     * Sets up a real-time listener on the Firestore 'notices' collection.
     * Triggers notifications for new notices and updates the UI.
     */
    private void startNoticeListener() {
        listenerReg = db.collection("notices")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {

                if (error != null) {
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshots == null) return;

                noticeList.clear();

                if (snapshots.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerNotices.setVisibility(View.GONE);
                    if (tvNoticeCount != null) tvNoticeCount.setText("0 notices");
                    adapter.notifyDataSetChanged();
                    return;
                }

                // Identify the most recent notice ID (first document in DESC order)
                String latestId = snapshots.getDocuments().get(0).getId();
                String lastNotifiedId = prefs.getString(PREF_LAST_ID, "");

                // Map Firestore documents to NoticeModel objects
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String id = doc.getId();
                    String title = doc.getString("title");
                    String message = doc.getString("message");
                    String createdBy = doc.getString("createdBy");
                    
                    Timestamp firestoreTs = doc.getTimestamp("timestamp");
                    long tsMillis = (firestoreTs != null) ? firestoreTs.toDate().getTime() : 0;

                    noticeList.add(new NoticeModel(id, title, message, createdBy, tsMillis));
                }

                // Toggle visibility based on content
                if (noticeList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerNotices.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerNotices.setVisibility(View.VISIBLE);
                }

                // Update notice count display
                if (tvNoticeCount != null) {
                    String countText = noticeList.size() + " notice" + (noticeList.size() == 1 ? "" : "s");
                    tvNoticeCount.setText(countText);
                }

                adapter.notifyDataSetChanged();

                // Notification Logic: Only show if the latest notice is actually new
                if (!latestId.equals(lastNotifiedId) && !noticeList.isEmpty()) {
                    NoticeModel latest = noticeList.get(0);

                    NotificationHelper.showNoticeNotification(
                        this,
                        latest.getTitle(),
                        latest.getMessage(),
                        OwnerNoticeActivity.class
                    );

                    // Store the ID to prevent repeated notifications for the same notice
                    prefs.edit().putString(PREF_LAST_ID, latestId).apply();
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Crucial: Remove listener to prevent memory leaks when activity is destroyed
        if (listenerReg != null) {
            listenerReg.remove();
        }
    }
}
