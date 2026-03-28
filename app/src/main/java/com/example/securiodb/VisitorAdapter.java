package com.example.securiodb;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securiodb.models.Visitor;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class VisitorAdapter extends RecyclerView.Adapter<VisitorAdapter.VisitorViewHolder> {

    private List<Visitor> visitorList;
    private OnVisitorActionListener listener;
    private String userRole;

    public interface OnVisitorActionListener {
        void onApprove(Visitor visitor);
        void onReject(Visitor visitor);
        void onMarkExit(Visitor visitor);
    }

    public VisitorAdapter(List<Visitor> visitorList, String userRole, OnVisitorActionListener listener) {
        this.visitorList = visitorList;
        this.userRole = userRole;
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
        holder.tvName.setText(visitor.getName());
        holder.chipPurpose.setText(visitor.getPurpose());
        holder.tvFlat.setText("Flat: " + visitor.getFlatNumber());
        holder.tvStatusBadge.setText(visitor.getStatus());
        holder.tvStatusBadge.setVisibility(View.VISIBLE);

        Glide.with(holder.itemView.getContext())
                .load(visitor.getPhotoUrl())
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(holder.ivPhoto);

        // UI Logic based on role and status
        if ("owner".equals(userRole) && "Pending".equals(visitor.getStatus())) {
            holder.layoutActions.setVisibility(View.VISIBLE);
        } else {
            holder.layoutActions.setVisibility(View.GONE);
        }

        if ("guard".equals(userRole) && "Approved".equals(visitor.getStatus()) && visitor.getExitTime() == null) {
            holder.btnMarkExit.setVisibility(View.VISIBLE);
        } else {
            holder.btnMarkExit.setVisibility(View.GONE);
        }

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(visitor);
        });
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(visitor);
        });
        holder.btnMarkExit.setOnClickListener(v -> {
            if (listener != null) listener.onMarkExit(visitor);
        });
    }

    @Override
    public int getItemCount() {
        return visitorList.size();
    }

    public static class VisitorViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvFlat, tvStatusBadge;
        Chip chipPurpose;
        LinearLayout layoutActions;
        Button btnApprove, btnReject, btnMarkExit;

        public VisitorViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivVisitor);
            tvName = itemView.findViewById(R.id.tvVisitorName);
            chipPurpose = itemView.findViewById(R.id.chipPurpose);
            tvFlat = itemView.findViewById(R.id.tvFlatNumber);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnMarkExit = itemView.findViewById(R.id.btnMarkExit);
        }
    }
}
