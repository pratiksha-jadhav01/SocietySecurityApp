package com.example.securiodb.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securiodb.R;
import com.example.securiodb.models.NoticeModel;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * NoticeAdapter binds notice data and handles admin delete actions.
 */
public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {

    private List<NoticeModel> list;
    private Context context;
    private boolean isAdminView;

    /**
     * Constructor for NoticeAdapter.
     * @param context The activity or fragment context.
     * @param list    The list of notices.
     */
    public NoticeAdapter(Context context, List<NoticeModel> list) {
        this.context = context;
        this.list    = list;
        // Detect if this is an admin view by checking the class name of context
        this.isAdminView = context.getClass().getSimpleName().contains("Admin");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NoticeModel notice = list.get(position);

        holder.tvTitle.setText(notice.getTitle() != null ? notice.getTitle() : "Notice");
        holder.tvMessage.setText(notice.getMessage() != null ? notice.getMessage() : "");

        String by = notice.getCreatedBy();
        holder.tvPostedBy.setText(by != null ? by.substring(0, 1).toUpperCase() + by.substring(1) : "Admin");

        long ts = notice.getTimestamp();
        if (ts > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(ts)));
        } else {
            holder.tvDate.setText("Just now");
        }

        // --- DELETE LOGIC ---
        if (isAdminView) {
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivDelete.setOnClickListener(v -> showDeleteConfirmation(notice.getNoticeId(), position));
        } else {
            holder.ivDelete.setVisibility(View.GONE);
        }
    }

    /**
     * Shows a confirmation dialog before deleting a notice from Firestore.
     */
    private void showDeleteConfirmation(String noticeId, int position) {
        if (noticeId == null || noticeId.isEmpty()) return;

        new AlertDialog.Builder(context)
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to delete this notice permanently?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNotice(noticeId, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the notice document from the 'notices' collection in Firestore.
     */
    private void deleteNotice(String noticeId, int position) {
        FirebaseFirestore.getInstance().collection("notices")
                .document(noticeId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Notice deleted successfully", Toast.LENGTH_SHORT).show();
                    // Note: Real-time listener will update the list automatically if active, 
                    // but we can also manually remove if needed.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDate, tvPostedBy;
        ImageView ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tvNoticeTitle);
            tvMessage  = itemView.findViewById(R.id.tvNoticeMessage);
            tvDate     = itemView.findViewById(R.id.tvNoticeDate);
            tvPostedBy = itemView.findViewById(R.id.tvPostedBy);
            ivDelete   = itemView.findViewById(R.id.ivDeleteNotice);
        }
    }
}
