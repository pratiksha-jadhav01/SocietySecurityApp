package com.example.securiodb.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securiodb.R;
import com.example.securiodb.models.Visitor;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.StatusViewHolder> {

    private Context context;
    private List<Visitor> visitorList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public StatusAdapter(Context context, List<Visitor> visitorList) {
        this.context = context;
        this.visitorList = visitorList;
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_status_visitor, parent, false);
        return new StatusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        Visitor visitor = visitorList.get(position);

        // Set basic info
        holder.tvName.setText(visitor.getName());
        holder.tvFlat.setText("Flat: " + visitor.getFlatNumber());
        holder.chipPurpose.setText(visitor.getPurpose());

        // Load photo with Glide
        Glide.with(context)
                .load(visitor.getPhotoUrl())
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(holder.ivPhoto);

        // Status badge logic
        String status = visitor.getStatus() != null ? visitor.getStatus() : "Pending";
        holder.tvStatus.setText(status.toUpperCase());
        
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(8f);

        if ("Pending".equalsIgnoreCase(status)) {
            shape.setColor(Color.parseColor("#FAEEDA"));
            holder.tvStatus.setTextColor(Color.parseColor("#854F0B"));
        } else if ("Approved".equalsIgnoreCase(status)) {
            shape.setColor(Color.parseColor("#EAF3DE"));
            holder.tvStatus.setTextColor(Color.parseColor("#27500A"));
        } else if ("Rejected".equalsIgnoreCase(status)) {
            shape.setColor(Color.parseColor("#FCEBEB"));
            holder.tvStatus.setTextColor(Color.parseColor("#A32D2D"));
        }
        holder.tvStatus.setBackground(shape);

        // Format timestamps
        if (visitor.getEntryTime() != null) {
            holder.tvEntryTime.setText(dateFormat.format(visitor.getEntryTime().toDate()));
        } else {
            holder.tvEntryTime.setText("N/A");
        }

        if (visitor.getExitTime() != null) {
            holder.tvExitTime.setText(dateFormat.format(visitor.getExitTime().toDate()));
        } else {
            holder.tvExitTime.setText("Not exited yet");
        }
    }

    @Override
    public int getItemCount() {
        return visitorList.size();
    }

    static class StatusViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvFlat, tvStatus, tvEntryTime, tvExitTime;
        Chip chipPurpose;

        public StatusViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivVisitorPhoto);
            tvName = itemView.findViewById(R.id.tvVisitorName);
            tvFlat = itemView.findViewById(R.id.tvFlatNumber);
            tvStatus = itemView.findViewById(R.id.tvStatusBadge);
            tvEntryTime = itemView.findViewById(R.id.tvEntryTime);
            tvExitTime = itemView.findViewById(R.id.tvExitTime);
            chipPurpose = itemView.findViewById(R.id.chipPurpose);
        }
    }
}
