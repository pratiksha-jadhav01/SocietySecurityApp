package com.example.securiodb.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securiodb.R;
import com.example.securiodb.model.Visitor;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VisitorAdapter extends RecyclerView.Adapter<VisitorAdapter.VisitorViewHolder> {

    private List<Visitor> visitorList;
    private boolean isOwnerView;
    private OnVisitorActionListener listener;

    public interface OnVisitorActionListener {
        void onApprove(Visitor visitor);
        void onReject(Visitor visitor);
    }

    public VisitorAdapter(List<Visitor> visitorList, boolean isOwnerView, OnVisitorActionListener listener) {
        this.visitorList = visitorList;
        this.isOwnerView = isOwnerView;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VisitorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_visitor, parent, false);
        return new VisitorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VisitorViewHolder holder, int position) {
        Visitor visitor = visitorList.get(position);

        // Basic Info
        holder.tvName.setText(visitor.name);
        holder.tvFlat.setText("Flat: " + visitor.flatNumber);
        
        // Purpose Chip
        holder.chipPurpose.setText(visitor.purpose);
        if ("Delivery".equalsIgnoreCase(visitor.purpose)) {
            holder.chipPurpose.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
        } else {
            holder.chipPurpose.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
        }

        // Time Formatting
        if (visitor.timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(visitor.timestamp.toDate()));
        }

        // Photo loading with Glide
        Glide.with(holder.itemView.getContext())
                .load(visitor.photoUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.ivPhoto);

        // Status and Actions Logic
        if ("Pending".equalsIgnoreCase(visitor.status)) {
            if (isOwnerView) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setVisibility(View.GONE);
            } else {
                holder.layoutActions.setVisibility(View.GONE);
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("PENDING");
                holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#FBC02D"));
            }
        } else {
            holder.layoutActions.setVisibility(View.GONE);
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(visitor.status.toUpperCase());
            
            if ("Approved".equalsIgnoreCase(visitor.status)) {
                holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#2E7D32"));
            } else {
                holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#C62828"));
            }
        }

        // Action Listeners
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(visitor);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(visitor);
        });
    }

    @Override
    public int getItemCount() {
        return visitorList.size();
    }

    static class VisitorViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivPhoto;
        TextView tvName, tvFlat, tvTime, tvStatusBadge;
        Chip chipPurpose;
        LinearLayout layoutActions;
        Button btnApprove, btnReject;

        public VisitorViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivVisitor);
            tvName = itemView.findViewById(R.id.tvVisitorName);
            tvFlat = itemView.findViewById(R.id.tvFlatNumber);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            chipPurpose = itemView.findViewById(R.id.chipPurpose);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
