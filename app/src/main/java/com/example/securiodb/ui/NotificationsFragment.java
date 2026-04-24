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
import com.example.securiodb.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;

    private FirebaseFirestore db;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private ListenerRegistration notificationsListener;
    private String flatNo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            flatNo = getArguments().getString("flatNo", "");
        }

        rvNotifications = view.findViewById(R.id.rvNotifications);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        loadNotifications();
        return view;
    }

    private void loadNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        if (notificationsListener != null) notificationsListener.remove();

        progressBar.setVisibility(View.VISIBLE);
        // Show notifications specifically for this user
        Query query = db.collection("notifications")
                .whereEqualTo("targetUid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        notificationsListener = query.addSnapshotListener((value, error) -> {
            if (!isAdded()) return;
            progressBar.setVisibility(View.GONE);
            if (value != null) {
                notificationList.clear();
                notificationList.addAll(value.toObjects(Notification.class));
                adapter.notifyDataSetChanged();
                layoutEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationsListener != null) notificationsListener.remove();
    }

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

            holder.itemView.setOnClickListener(v -> {
                if (!n.isRead()) {
                    db.collection("notifications").document(n.getNotificationId()).update("isRead", true);
                }
            });

            holder.tvTitle.setTypeface(null, n.isRead() ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
            if (holder.unreadIndicator != null) {
                holder.unreadIndicator.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);
            }
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
