package com.example.securiodb.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.R;
import com.example.securiodb.models.ComplaintModel;

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
        
        // Replaced User ID with Flat No.
        String flat = m.getFlatNo();
        holder.tvFlatNo.setText("Flat No: " + (flat != null ? flat : "---"));

        String status = m.getStatus() != null ? m.getStatus() : "Pending";
        holder.tvStatus.setText(status);

        if ("Resolved".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.badge_approved);
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            holder.btnResolve.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.badge_pending);
            holder.tvStatus.setTextColor(Color.parseColor("#8B5E30"));
            holder.btnResolve.setVisibility(View.VISIBLE);
            holder.btnResolve.setEnabled(true);
            holder.btnResolve.setText("Mark as Resolved");
        }

        holder.btnResolve.setOnClickListener(v -> {
            if (callback != null && m.getComplaintId() != null) {
                holder.btnResolve.setEnabled(false);
                holder.btnResolve.setText("Resolving...");
                callback.onResolve(m.getComplaintId(), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvStatus, tvFlatNo;
        Button btnResolve;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle       = itemView.findViewById(R.id.tvAdminTitle);
            tvDescription = itemView.findViewById(R.id.tvAdminDescription);
            tvStatus      = itemView.findViewById(R.id.tvAdminStatus);
            tvFlatNo      = itemView.findViewById(R.id.tvAdminFlatNo);
            btnResolve    = itemView.findViewById(R.id.btnResolve);
        }
    }
}
