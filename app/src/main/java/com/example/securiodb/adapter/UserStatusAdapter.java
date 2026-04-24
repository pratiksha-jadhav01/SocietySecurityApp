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
import com.example.securiodb.models.UserStatusModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter to display payment status for each user.
 */
public class UserStatusAdapter extends RecyclerView.Adapter<UserStatusAdapter.ViewHolder> {

    private List<UserStatusModel> list;
    private Context context;

    public UserStatusAdapter(Context context, List<UserStatusModel> list) {
        this.context = context;
        this.list    = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_user_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserStatusModel model = list.get(position);

        // Show user ID (Standardized as Flat Number or UID)
        holder.tvUserId.setText(model.getUserId() != null
            ? "User ID: " + model.getUserId() : "User ID: Unknown");

        // Format timestamp to readable date
        if (model.getUpdatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.tvUpdatedAt.setText(
                "Updated: " + sdf.format(new Date(model.getUpdatedAt())));
        } else {
            holder.tvUpdatedAt.setText("No update recorded");
        }

        // Status badge color and text
        String status = model.getStatus() != null
            ? model.getStatus() : "Unpaid";
        holder.tvStatus.setText(status);

        if (status.equalsIgnoreCase("Paid")) {
            holder.tvStatus.setBackgroundResource(R.drawable.badge_approved);
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.badge_rejected);
            holder.tvStatus.setTextColor(Color.parseColor("#C62828"));
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserId, tvUpdatedAt, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserId    = itemView.findViewById(R.id.tvUserId);
            tvUpdatedAt = itemView.findViewById(R.id.tvUpdatedAt);
            tvStatus    = itemView.findViewById(R.id.tvStatus);
        }
    }
}
