package com.example.securiodb.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securiodb.R;
import com.example.securiodb.models.ComplaintModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Adapter for Admin to view and resolve complaints.
 */
public class AdminComplaintAdapter extends RecyclerView.Adapter<AdminComplaintAdapter.ViewHolder> {

    public interface ResolveCallback {
        void onResolve(String complaintId, int position);
    }

    private List<ComplaintModel> list;
    private Context context;
    private ResolveCallback callback;

    public AdminComplaintAdapter(Context context, List<ComplaintModel> list, ResolveCallback callback) {
        this.context  = context;
        this.list     = list;
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_admin_complaint, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComplaintModel m = list.get(position);

        holder.tvTitle.setText(m.getTitle() != null ? m.getTitle() : "No title");
        holder.tvDescription.setText(m.getDescription() != null ? m.getDescription() : "");
        
        String flat = m.getFlatNo();
        holder.tvFlatNo.setText("Flat No: " + (flat != null ? flat : "---"));

        String status = m.getStatus() != null ? m.getStatus() : "Pending";
        holder.tvStatus.setText(status);

        if ("Resolved".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.badge_approved);
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            holder.btnResolve.setVisibility(View.VISIBLE);
            holder.btnResolve.setText("UPDATE STATUS");
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.badge_pending);
            holder.tvStatus.setTextColor(Color.parseColor("#8B5E30"));
            holder.btnResolve.setVisibility(View.VISIBLE);
            holder.btnResolve.setText("UPDATE STATUS");
        }

        // Load Complaint Image if available
        String imageUrl = m.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            holder.ivComplaintImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_search_cream)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivComplaintImage);
        } else {
            holder.ivComplaintImage.setVisibility(View.GONE);
        }

        // Resolution Details & Image
        String resDetails = m.getAdminResponse();
        String resImage = m.getResolutionImageUrl();
        if ((resDetails != null && !resDetails.isEmpty()) || (resImage != null && !resImage.isEmpty())) {
            holder.layoutResolution.setVisibility(View.VISIBLE);
            holder.tvResolutionDetails.setText(resDetails != null ? resDetails : "");
            if (resImage != null && !resImage.isEmpty()) {
                holder.ivResolutionImage.setVisibility(View.VISIBLE);
                Glide.with(context).load(resImage).into(holder.ivResolutionImage);
            } else {
                holder.ivResolutionImage.setVisibility(View.GONE);
            }
        } else {
            holder.layoutResolution.setVisibility(View.GONE);
        }

        holder.btnResolve.setOnClickListener(v -> {
            if (callback != null && m.getComplaintId() != null) {
                callback.onResolve(m.getComplaintId(), position);
            }
        });

        // Delete Functionality
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                .setTitle("Delete Complaint")
                .setMessage("Are you sure you want to delete this complaint permanently?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (m.getComplaintId() != null) {
                        FirebaseFirestore.getInstance().collection("complaints")
                            .document(m.getComplaintId())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(context, "Complaint deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvStatus, tvFlatNo, tvResolutionDetails;
        ImageView ivComplaintImage, ivResolutionImage, btnDelete;
        Button btnResolve;
        View layoutResolution;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle       = itemView.findViewById(R.id.tvAdminTitle);
            tvDescription = itemView.findViewById(R.id.tvAdminDescription);
            tvStatus      = itemView.findViewById(R.id.tvAdminStatus);
            tvFlatNo      = itemView.findViewById(R.id.tvAdminFlatNo);
            ivComplaintImage = itemView.findViewById(R.id.ivComplaintImage);
            btnResolve    = itemView.findViewById(R.id.btnResolve);
            btnDelete     = itemView.findViewById(R.id.btnDeleteComplaint); 
            layoutResolution = itemView.findViewById(R.id.layoutResolution);
            tvResolutionDetails = itemView.findViewById(R.id.tvResolutionDetails);
            ivResolutionImage = itemView.findViewById(R.id.ivResolutionImage);
        }
    }
}
