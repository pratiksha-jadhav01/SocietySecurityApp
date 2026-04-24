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
 * OwnerNoticeActivity displays a real-time list of society notices for the owners.
 * It also triggers local notifications when a new notice is detected using NotificationHelper.
 */
public class OwnerNoticeActivity extends AppCompatActivity {

    private static final String PREF_NAME    = "NoticeBoardPrefs";
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

        db    = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        recyclerNotices = findViewById(R.id.recyclerNotices);
        tvNoticeCount   = findViewById(R.id.tvNoticeCount);
        layoutEmpty     = findViewById(R.id.layoutEmpty);

        // Create notification channel once at start
        NotificationHelper.createChannel(this);

        // Setup back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup RecyclerView
        adapter = new NoticeAdapter(this, noticeList);
        recyclerNotices.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotices.setAdapter(adapter);

        // Start real-time Firestore listener
        startNoticeListener();
    }

    /**
     * Listens for real-time updates in the 'notices' collection.
     * Triggers a notification if a new notice is detected.
     */
    private void startNoticeListener() {
        listenerReg = db.collection("notices")
            // Show latest notice first
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {

                // Safely handle error
                if (error != null) {
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshots == null) return;

                noticeList.clear();

                if (snapshots.isEmpty()) {
                    // Show empty state UI
                    if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerNotices.setVisibility(View.GONE);
                    if (tvNoticeCount != null) tvNoticeCount.setText("0 notices");
                    adapter.notifyDataSetChanged();
                    return;
                }

                // Get the latest notice ID for notification comparison
                String latestId = snapshots.getDocuments().get(0).getId();
                String lastNotifiedId = prefs.getString(PREF_LAST_ID, "");

                // Loop through documents and build the notice list
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String id = doc.getId();
                    
                    // Null-safe field reading
                    Object titleObj = doc.get("title");
                    String title = titleObj != null ? titleObj.toString() : "No Title";

                    Object msgObj = doc.get("message");
                    String message = msgObj != null ? msgObj.toString() : "";

                    Object byObj = doc.get("createdBy");
                    String createdBy = byObj != null ? byObj.toString() : "admin";

                    // Firestore Timestamp extraction
                    Timestamp ts = doc.getTimestamp("timestamp");
                    long timestampMillis = (ts != null) ? ts.toDate().getTime() : 0;

                    noticeList.add(new NoticeModel(id, title, message, createdBy, timestampMillis));
                }

                // Show or hide empty state depending on data
                if (noticeList.isEmpty()) {
                    if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerNotices.setVisibility(View.GONE);
                } else {
                    if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
                    recyclerNotices.setVisibility(View.VISIBLE);
                }

                if (tvNoticeCount != null) {
                    String countText = noticeList.size() + " notice" + (noticeList.size() == 1 ? "" : "s");
                    tvNoticeCount.setText(countText);
                }

                adapter.notifyDataSetChanged();

                // ── NOTIFICATION LOGIC ─────────────────────
                // Notify user only if the latest notice is brand new (not seen before)
                if (!latestId.equals(lastNotifiedId) && !noticeList.isEmpty()) {
                    NoticeModel latest = noticeList.get(0);

                    NotificationHelper.showNoticeNotification(
                        this,
                        latest.getTitle(),
                        latest.getMessage(),
                        OwnerNoticeActivity.class
                    );

                    // Save latest ID to prevent duplicate notifications for the same notice
                    prefs.edit().putString(PREF_LAST_ID, latestId).apply();
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener to prevent memory leaks
        if (listenerReg != null) {
            listenerReg.remove();
        }
    }
}
