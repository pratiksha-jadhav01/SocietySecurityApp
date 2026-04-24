package com.example.securiodb.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securiodb.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.ViewHolder> {

    private List<Map<String, Object>> entries;
    private OnEntryActionListener listener;

    public interface OnEntryActionListener {
        void onExitClick(String docId);
        void onDeleteClick(String docId);
    }

    // Retaining legacy interface for backward compatibility if needed, 
    // but moving to OnEntryActionListener
    public interface OnExitClickListener extends OnEntryActionListener {}

    public StatusAdapter(List<Map<String, Object>> entries, OnEntryActionListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    public void updateList(List<Map<String, Object>> newList) {
        this.entries = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_status_visitor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> entry = entries.get(position);
        String docId = (String) entry.get("id");
        String name = (String) entry.get("name");
        String flat = (String) entry.get("flatNumber");
        if (flat == null) flat = (String) entry.get("flatNo");
        
        String purpose = (String) entry.get("purpose");
        String status = (String) entry.get("status");
        
        String photoUrl = (String) entry.get("imageUrl");
        if (photoUrl == null || photoUrl.isEmpty()) {
            photoUrl = (String) entry.get("photoUrl");
        }
        
        Object timestamp = entry.get("timestamp");
        Object entryTime = entry.get("entryTime");
        Object exitTime = entry.get("exitTime");

        holder.tvName.setText(name);
        holder.tvFlat.setText("Flat: " + (flat != null ? flat : "---"));
        holder.tvPurpose.setText("Purpose: " + purpose);
        
        Object timeToDisplay = entryTime != null ? entryTime : timestamp;
        if (timeToDisplay instanceof com.google.firebase.Timestamp) {
            Date date = ((com.google.firebase.Timestamp) timeToDisplay).toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tvTime.setText("Entry: " + sdf.format(date));
        } else if (timeToDisplay instanceof Long) {
            Date date = new Date((Long) timeToDisplay);
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tvTime.setText("Entry: " + sdf.format(date));
        }

        holder.tvStatus.setText(status != null ? status.toUpperCase() : "PENDING");
        
        // Color coding
        if ("Approved".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
            if (exitTime == null) {
                holder.btnExit.setVisibility(View.VISIBLE);
            } else {
                holder.btnExit.setVisibility(View.GONE);
                if (exitTime instanceof com.google.firebase.Timestamp) {
                    Date date = ((com.google.firebase.Timestamp) exitTime).toDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    holder.tvTime.append(" | Exit: " + sdf.format(date));
                } else if (exitTime instanceof Long) {
                    Date date = new Date((Long) exitTime);
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    holder.tvTime.append(" | Exit: " + sdf.format(date));
                }
            }
        } else if ("Rejected".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);
            holder.btnExit.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.btnExit.setVisibility(View.GONE);
        }

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(photoUrl)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.ic_menu_camera)
                    .centerCrop()
                    .into(holder.ivPhoto);
        } else {
            holder.ivPhoto.setImageResource(android.R.drawable.ic_menu_camera);
        }

        holder.btnExit.setOnClickListener(v -> {
            if (listener != null) listener.onExitClick(docId);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(docId);
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivPhoto;
        TextView tvName, tvFlat, tvPurpose, tvTime, tvStatus;
        Button btnExit;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivVisitorPhoto);
            tvName = itemView.findViewById(R.id.tvVisitorName);
            tvFlat = itemView.findViewById(R.id.tvFlatNumber);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvTime = itemView.findViewById(R.id.tvTimeInfo);
            tvStatus = itemView.findViewById(R.id.tvStatusBadge);
            btnExit = itemView.findViewById(R.id.btnMarkExit);
            btnDelete = itemView.findViewById(R.id.btnDeleteEntry);
        }
    }
}
