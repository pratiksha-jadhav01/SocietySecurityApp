package com.example.securiodb.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        // Load Complaint Image (submitted by owner)
        String imageUrl = m.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            holder.ivComplaintImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(imageUrl).into(holder.ivComplaintImage);
        } else {
            holder.ivComplaintImage.setVisibility(View.GONE);
        }

        // Status badge
        String status = m.getStatus();
        if (status == null) status = "Open";
        holder.tvStatus.setText(status);

        if ("Resolved".equalsIgnoreCase(status) || "In Progress".equalsIgnoreCase(status)) {
            if ("Resolved".equalsIgnoreCase(status)) {
                holder.tvStatus.setBackgroundResource(R.drawable.badge_approved);
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            } else {
                holder.tvStatus.setBackgroundResource(R.drawable.badge_pending);
                holder.tvStatus.setTextColor(Color.parseColor("#8B5E30"));
            }

            // Show admin response area
            String resp = m.getAdminResponse();
            String resImg = m.getResolutionImageUrl();
            
            boolean hasResponse = (resp != null && !resp.isEmpty());
            boolean hasResImage = (resImg != null && !resImg.isEmpty());

            if (hasResponse || hasResImage) {
                holder.layoutAdminResponse.setVisibility(View.VISIBLE);
                
                if (hasResponse) {
                    holder.tvResponse.setVisibility(View.VISIBLE);
                    holder.tvResponse.setText("Admin Response: " + resp);
                } else {
                    holder.tvResponse.setVisibility(View.GONE);
                }

                if (hasResImage) {
                    holder.ivResolutionImage.setVisibility(View.VISIBLE);
                    Glide.with(context).load(resImg).into(holder.ivResolutionImage);
                } else {
                    holder.ivResolutionImage.setVisibility(View.GONE);
                }
            } else {
                holder.layoutAdminResponse.setVisibility(View.GONE);
            }
        } else {
            // Pending/Open status
            holder.tvStatus.setBackgroundResource(R.drawable.badge_pending);
            holder.tvStatus.setTextColor(Color.parseColor("#8B5E30"));
            holder.layoutAdminResponse.setVisibility(View.GONE);
        }

        // Handle Delete Action
        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                    .setTitle("Delete Complaint")
                    .setMessage("Are you sure you want to delete this complaint?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (m.getComplaintId() != null) {
                            deleteComplaint(m.getComplaintId(), position);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }

    private void deleteComplaint(String complaintId, int position) {
        FirebaseFirestore.getInstance().collection("complaints").document(complaintId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Complaint deleted", Toast.LENGTH_SHORT).show();
                // Note: Real-time listener in Activity will automatically update the list, 
                // but we can also manually remove it for immediate feedback.
            })
            .addOnFailureListener(e -> Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvStatus, tvResponse, tvTime, tvCategory;
        ImageView ivComplaintImage, ivResolutionImage, btnDelete;
        View layoutAdminResponse;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle       = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus      = itemView.findViewById(R.id.tvStatus);
            tvResponse    = itemView.findViewById(R.id.tvResponse);
            tvTime        = itemView.findViewById(R.id.tvTime);
            tvCategory    = itemView.findViewById(R.id.tvCategory);
            ivComplaintImage = itemView.findViewById(R.id.ivComplaintImage);
            ivResolutionImage = itemView.findViewById(R.id.ivResolutionImage);
            layoutAdminResponse = itemView.findViewById(R.id.layoutAdminResponse);
            btnDelete     = itemView.findViewById(R.id.btnDeleteComplaint);
        }
    }
}
