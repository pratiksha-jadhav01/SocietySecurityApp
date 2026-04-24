package com.example.securiodb.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.R;
import com.example.securiodb.models.Notice;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder> {

    private List<Notice> noticeList;
    private boolean isAdmin;

    public NoticeAdapter(List<Notice> noticeList) {
        this.noticeList = noticeList;
        this.isAdmin = false;
    }

    public NoticeAdapter(List<Notice> noticeList, boolean isAdmin) {
        this.noticeList = noticeList;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
        return new NoticeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        Notice notice = noticeList.get(position);
        holder.tvTitle.setText(notice.getTitle() != null ? notice.getTitle() : "No Title");
        holder.tvMessage.setText(notice.getMessage() != null ? notice.getMessage() : "No Message");

        String createdBy = notice.getCreatedBy() != null ? notice.getCreatedBy() : "Admin";
        String dateStr = "Posted by " + createdBy;
        
        if (notice.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            dateStr += " · " + sdf.format(notice.getTimestamp().toDate());
        } else {
            dateStr += " · Just now";
        }
        holder.tvMeta.setText(dateStr);

        if (isAdmin && notice.getNoticeId() != null) {
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivDelete.setOnClickListener(v -> {
                FirebaseFirestore.getInstance().collection("notices")
                        .document(notice.getNoticeId())
                        .delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(v.getContext(), "Notice Deleted", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(v.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        } else {
            holder.ivDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return noticeList != null ? noticeList.size() : 0;
    }

    public static class NoticeViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvMeta;
        ImageButton ivDelete;

        public NoticeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNoticeTitle);
            tvMessage = itemView.findViewById(R.id.tvNoticeBody);
            tvMeta = itemView.findViewById(R.id.tvNoticeMeta);
            ivDelete = itemView.findViewById(R.id.ivDeleteNotice);
        }
    }
}
