package com.example.securiodb;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.models.Notification;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private ChipGroup chipGroupFilter;

    private FirebaseFirestore db;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private ListenerRegistration notificationsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupFilters();

        loadNotifications("all");
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipVisitor) loadNotifications("visitor");
            else if (checkedId == R.id.chipDelivery) loadNotifications("delivery");
            else if (checkedId == R.id.chipSecurity) loadNotifications("security");
            else loadNotifications("all");
        });
    }

    private void loadNotifications(String type) {
        if (notificationsListener != null) notificationsListener.remove();

        progressBar.setVisibility(View.VISIBLE);
        Query query = db.collection("notifications").orderBy("timestamp", Query.Direction.DESCENDING);

        if (!"all".equals(type)) {
            query = query.whereEqualTo("type", type);
        }

        notificationsListener = query.addSnapshotListener((value, error) -> {
            progressBar.setVisibility(View.GONE);
            if (value != null) {
                notificationList.clear();
                notificationList.addAll(value.toObjects(Notification.class));
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationsListener != null) notificationsListener.remove();
    }

    // Inner Adapter
    class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
        private List<Notification> notifications;

        NotificationAdapter(List<Notification> notifications) { this.notifications = notifications; }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            Notification n = notifications.get(position);
            holder.tvTitle.setText(n.getTitle());
            holder.tvBody.setText(n.getBody());
            
            if (n.getTimestamp() != null) {
                holder.tvTime.setText(new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(n.getTimestamp().toDate()));
            }

            // Mark as read on click
            holder.itemView.setOnClickListener(v -> {
                if (!n.isRead()) {
                    db.collection("notifications").document(n.getNotificationId()).update("isRead", true);
                }
            });

            // Indicate unread
            holder.tvTitle.setTypeface(null, n.isRead() ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
            holder.unreadIndicator.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() { return notifications.size(); }

        class NotificationViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvBody, tvTime;
            View unreadIndicator;

            NotificationViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvNotificationTitle);
                tvBody = v.findViewById(R.id.tvNotificationBody);
                tvTime = v.findViewById(R.id.tvNotificationTime);
                unreadIndicator = v.findViewById(R.id.unreadIndicator);
            }
        }
    }
}
