package com.example.securiodb.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.R;
import com.example.securiodb.models.NoticeModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying society notices in a RecyclerView.
 * Handles binding NoticeModel data to item_notice layout.
 */
public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {

    private List<NoticeModel> list;
    private Context context;
    private OnNoticeClickListener listener;

    public interface OnNoticeClickListener {
        void onDelete(NoticeModel notice);
    }

    /**
     * Constructor for NoticeAdapter.
     * @param context The activity or fragment context.
     * @param list The list of NoticeModel objects to display.
     */
    public NoticeAdapter(Context context, List<NoticeModel> list) {
        this.context = context;
        this.list = list;
    }

    public NoticeAdapter(Context context, List<NoticeModel> list, OnNoticeClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    /**
     * Simplified constructor for when context is not explicitly needed.
     * @param list The list of NoticeModel objects.
     */
    public NoticeAdapter(List<NoticeModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use parent context if local context is null
        Context ctx = context != null ? context : parent.getContext();
        View view = LayoutInflater.from(ctx).inflate(R.layout.item_notice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NoticeModel notice = list.get(position);

        // Set Title with null safety
        String title = notice.getTitle();
        holder.tvTitle.setText(title != null ? title : "Notice");

        // Set Message with null safety
        String message = notice.getMessage();
        holder.tvMessage.setText(message != null ? message : "");

        // Set Posted By with null safety and capitalization
        String by = notice.getCreatedBy();
        if (by != null && !by.isEmpty()) {
            String formattedBy = by.substring(0, 1).toUpperCase() + by.substring(1);
            holder.tvPostedBy.setText(formattedBy);
        } else {
            holder.tvPostedBy.setText("Admin");
        }

        // Format timestamp to a readable date string
        long ts = notice.getTimestamp();
        if (ts > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(ts)));
        } else {
            // Handles potential delay in Firestore server timestamp sync
            holder.tvDate.setText("Just now");
        }

        if (listener != null) {
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivDelete.setOnClickListener(v -> listener.onDelete(notice));
        } else {
            holder.ivDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        // Safe check to prevent NullPointerException
        return list != null ? list.size() : 0;
    }

    /**
     * ViewHolder class to hold references to UI components for each notice item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDate, tvPostedBy;
        ImageView ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNoticeTitle);
            tvMessage = itemView.findViewById(R.id.tvNoticeMessage);
            tvDate = itemView.findViewById(R.id.tvNoticeDate);
            tvPostedBy = itemView.findViewById(R.id.tvPostedBy);
            ivDelete = itemView.findViewById(R.id.ivDeleteNotice);
        }
    }
}
