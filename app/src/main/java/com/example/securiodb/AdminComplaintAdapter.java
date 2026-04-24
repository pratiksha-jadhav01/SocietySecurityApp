package com.example.securiodb;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminComplaintAdapter extends RecyclerView.Adapter<AdminComplaintAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> list;
    private final OnComplaintClickListener listener;

    public interface OnComplaintClickListener {
        void onUpdateStatus(Map<String, Object> complaint, int position);
    }

    public AdminComplaintAdapter(Context context, List<Map<String, Object>> list, OnComplaintClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_complaint, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Map<String, Object> c = list.get(position);

        String flat = (String) c.get("flatNo");
        String title = (String) c.get("title");
        h.tvTitle.setText(String.format("%s (Flat: %s)", title != null ? title : "No Title", flat != null ? flat : "N/A"));
        h.tvDesc.setText((String) c.get("description"));
        h.tvCategory.setText((String) c.get("category"));

        String status = (String) c.get("status");
        h.tvStatus.setText(status);

        if (status != null) {
            switch (status) {
                case "Open":
                    h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_rejected_text));
                    break;
                case "In Progress":
                    h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.primary));
                    break;
                case "Resolved":
                    h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_approved_text));
                    break;
            }
        }

        Object timestamp = c.get("raisedOn");
        if (timestamp instanceof Long) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            h.tvDate.setText(String.format("Raised on %s", sdf.format(new Date((Long) timestamp))));
        }

        String response = (String) c.get("adminResponse");
        if (response != null && !response.isEmpty()) {
            h.tvAdminResponse.setVisibility(View.VISIBLE);
            h.tvAdminResponse.setText(String.format("Response: %s", response));
        } else {
            h.tvAdminResponse.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onUpdateStatus(c, position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvCategory, tvStatus, tvDate, tvAdminResponse;

        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvDesc = v.findViewById(R.id.tvDescription);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvDate = v.findViewById(R.id.tvTime);
            tvAdminResponse = v.findViewById(R.id.tvResponse);
        }
    }
}
