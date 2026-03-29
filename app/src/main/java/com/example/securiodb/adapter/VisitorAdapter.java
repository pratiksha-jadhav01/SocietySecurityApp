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
import com.example.securiodb.models.Visitor;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class VisitorAdapter extends RecyclerView.Adapter<VisitorAdapter.VisitorViewHolder> {

    private List<Visitor> visitorList;
    private boolean isOwnerView;
    private boolean isAdminView;
    private OnVisitorActionListener listener;

    public interface OnVisitorActionListener {
        void onApprove(Visitor visitor);
        void onReject(Visitor visitor);
        void onOverride(Visitor visitor);
    }

    public VisitorAdapter(List<Visitor> visitorList) {
        this.visitorList = visitorList;
    }

    public VisitorAdapter(List<Visitor> visitorList, boolean isOwnerView, OnVisitorActionListener listener) {
        this.visitorList = visitorList;
        this.isOwnerView = isOwnerView;
        this.listener = listener;
    }

    public void setAdminView(boolean isAdminView) {
        this.isAdminView = isAdminView;
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
        holder.tvName.setText(visitor.getName());
        holder.tvFlat.setText("Flat: " + visitor.getFlatNumber());
        
        // Purpose Chip
        if (holder.chipPurpose != null) {
            holder.chipPurpose.setText(visitor.getPurpose());
            if ("Delivery".equalsIgnoreCase(visitor.getPurpose())) {
                holder.chipPurpose.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
            } else {
                holder.chipPurpose.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
            }
        }

        // Time Formatting
        if (visitor.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            if (holder.tvTime != null) {
                holder.tvTime.setText(sdf.format(visitor.getTimestamp().toDate()));
            } else if (holder.chipTime != null) {
                holder.chipTime.setText(sdf.format(visitor.getTimestamp().toDate()));
            }
        }

        // Photo loading with Glide
        Glide.with(holder.itemView.getContext())
                .load(visitor.getPhotoUrl())
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.ivPhoto);

        // Status and Actions Logic
        if ("Pending".equalsIgnoreCase(visitor.getStatus())) {
            if (isOwnerView || isAdminView) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setVisibility(View.GONE);
                if (isAdminView) {
                    holder.btnApprove.setText("OVERRIDE");
                    holder.btnApprove.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7B1FA2")));
                }
            } else {
                holder.layoutActions.setVisibility(View.GONE);
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setText("PENDING");
                holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#FBC02D"));
            }
        } else {
            holder.layoutActions.setVisibility(View.GONE);
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(visitor.getStatus().toUpperCase());
            
            if ("Approved".equalsIgnoreCase(visitor.getStatus())) {
                holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#2E7D32"));
            } else {
                holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#C62828"));
            }
        }

        // Action Listeners
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                if (isAdminView) listener.onOverride(visitor);
                else listener.onApprove(visitor);
            }
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
        Chip chipPurpose, chipTime;
        LinearLayout layoutActions;
        Button btnApprove, btnReject;

        public VisitorViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivVisitor);
            tvName = itemView.findViewById(R.id.tvVisitorName);
            tvFlat = itemView.findViewById(R.id.tvFlatNumber);
            tvTime = itemView.findViewById(R.id.tvTime);
            chipTime = itemView.findViewById(R.id.chipTime);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            chipPurpose = itemView.findViewById(R.id.chipPurpose);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
