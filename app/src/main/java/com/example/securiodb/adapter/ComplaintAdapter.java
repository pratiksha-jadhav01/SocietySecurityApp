package com.example.securiodb.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.R;
import com.example.securiodb.models.ComplaintModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying complaints in a list using ComplaintModel.
 */
public class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.ViewHolder> {

    private List<ComplaintModel> list;
    private Context context;

    public ComplaintAdapter(Context context, List<ComplaintModel> list) {
        this.context = context;
        this.list    = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_complaint, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComplaintModel m = list.get(position);

        holder.tvTitle.setText(m.getTitle() != null ? m.getTitle() : "No title");
        holder.tvDescription.setText(m.getDescription() != null ? m.getDescription() : "");

        if (holder.tvCategory != null) {
            holder.tvCategory.setText(m.getCategory() != null ? m.getCategory() : "");
        }

        // Format timestamp
        long timestamp = m.getRaisedOn() != 0 ? m.getRaisedOn() : m.getTimestamp();
        if (timestamp != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(timestamp)));
        } else {
            holder.tvTime.setText("");
        }

        // Status badge
        String status = m.getStatus();
        if (status == null) status = "Open";
        holder.tvStatus.setText(status);

        if ("Resolved".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.badge_approved);
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));

            // Show admin response if available
            String resp = m.getAdminResponse();
            if (resp != null && !resp.isEmpty()) {
                holder.tvResponse.setVisibility(View.VISIBLE);
                holder.tvResponse.setText("Admin Response: " + resp);
            } else {
                holder.tvResponse.setVisibility(View.GONE);
            }
        } else {
            // Pending/Open status
            holder.tvStatus.setBackgroundResource(R.drawable.badge_pending);
            holder.tvStatus.setTextColor(Color.parseColor("#8B5E30"));
            holder.tvResponse.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvStatus, tvResponse, tvTime, tvCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle       = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus      = itemView.findViewById(R.id.tvStatus);
            tvResponse    = itemView.findViewById(R.id.tvResponse);
            tvTime        = itemView.findViewById(R.id.tvTime);
            tvCategory    = itemView.findViewById(R.id.tvCategory);
        }
    }
}
