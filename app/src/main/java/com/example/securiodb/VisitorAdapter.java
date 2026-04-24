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

import java.util.List;

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
        
        if (holder.tvName != null) {
            holder.tvName.setText(visitor.getName());
        }
        
        if (holder.tvPurpose != null) {
            holder.tvPurpose.setText("Purpose: " + visitor.getPurpose());
        }
        
        if (holder.tvFlat != null) {
            holder.tvFlat.setText("Flat: " + visitor.getFlatNumber());
        }
        
        if (holder.tvStatus != null) {
            holder.tvStatus.setText(visitor.getStatus());
        }

        if (holder.ivVisitorPhoto != null) {
            // Using both photoUrl and imageUrl for compatibility with different entry points
            String imgUrl = visitor.getPhotoUrl();
            if (imgUrl == null || imgUrl.isEmpty()) {
                imgUrl = visitor.getImageUrl(); // Fallback to imageUrl
            }

            Glide.with(holder.itemView.getContext())
                    .load(imgUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .circleCrop()
                    .into(holder.ivVisitorPhoto);
        }

        // UI Logic based on role and status
        if (holder.layoutActions != null) {
            if ("owner".equals(userRole) && "Pending".equals(visitor.getStatus())) {
                holder.layoutActions.setVisibility(View.VISIBLE);
            } else {
                holder.layoutActions.setVisibility(View.GONE);
            }
        }

        if (holder.btnMarkExit != null) {
            if ("guard".equals(userRole) && "Approved".equals(visitor.getStatus()) && visitor.getExitTime() == null) {
                holder.btnMarkExit.setVisibility(View.VISIBLE);
            } else {
                holder.btnMarkExit.setVisibility(View.GONE);
            }
        }

        if (holder.btnApprove != null) {
            holder.btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(visitor);
            });
        }
        if (holder.btnReject != null) {
            holder.btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(visitor);
            });
        }
        if (holder.btnMarkExit != null) {
            holder.btnMarkExit.setOnClickListener(v -> {
                if (listener != null) listener.onMarkExit(visitor);
            });
        }
    }

    @Override
    public int getItemCount() {
        return visitorList != null ? visitorList.size() : 0;
    }

    public static class VisitorViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVisitorPhoto;
        TextView tvName, tvFlat, tvPurpose, tvStatus, tvTime;
        LinearLayout layoutActions;
        Button btnApprove, btnReject, btnMarkExit;

        public VisitorViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVisitorPhoto = itemView.findViewById(R.id.ivVisitorPhoto);
            tvName = itemView.findViewById(R.id.tvVisitorName);
            tvFlat = itemView.findViewById(R.id.tvFlat);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTime = itemView.findViewById(R.id.tvTime);

            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnMarkExit = itemView.findViewById(R.id.btnMarkExit);
        }
    }
}
