package com.example.securiodb.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securiodb.R;
import com.example.securiodb.models.VisitorModel;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class VisitorAdapter extends RecyclerView.Adapter<VisitorAdapter.ViewHolder> {

    private List<VisitorModel> list;
    private Context context;
    private OnItemClickListener listener;
    private boolean isMini = false;

    public interface OnItemClickListener {
        void onApprove(String docId, int position);
        void onReject(String docId, int position);
    }

    public VisitorAdapter(Context ctx, List<VisitorModel> list, OnItemClickListener listener) {
        this.context  = ctx;
        this.list     = list;
        this.listener = listener;
    }

    public VisitorAdapter(Context ctx, List<VisitorModel> list, boolean isMini, OnItemClickListener listener) {
        this.context  = ctx;
        this.list     = list;
        this.isMini   = isMini;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = isMini ? R.layout.item_visitor_request_mini : R.layout.item_visitor;
        View v = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        VisitorModel v = list.get(pos);

        if (h.tvName != null) h.tvName.setText(v.getName());
        if (h.tvPurpose != null) h.tvPurpose.setText("Purpose: " + v.getPurpose());
        
        if (h.tvFlat != null) {
            h.tvFlat.setText("Flat: " + v.getFlat());
        }

        if (h.tvTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Timestamp entryTime = v.getEntryTime();
            if (entryTime != null) {
                h.tvTime.setText(sdf.format(entryTime.toDate()));
            } else {
                h.tvTime.setText("--:--");
            }
        }

        String status = v.getStatus();
        if (h.tvStatus != null) {
            h.tvStatus.setText(status != null ? status : "Pending");
            try {
                switch (status != null ? status : "Pending") {
                    case "Approved":
                        h.tvStatus.setBackgroundResource(R.drawable.badge_approved);
                        h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_approved_text));
                        break;
                    case "Rejected":
                        h.tvStatus.setBackgroundResource(R.drawable.badge_rejected);
                        h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_rejected_text));
                        break;
                    default: // Pending
                        h.tvStatus.setBackgroundResource(R.drawable.badge_pending);
                        h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.primary));
                }
            } catch (Exception ignored) {}
        }

        // Action visibility logic
        if ("Pending".equalsIgnoreCase(status) && listener != null && h.layoutActions != null) {
            h.layoutActions.setVisibility(View.VISIBLE);
        } else if (h.layoutActions != null) {
            h.layoutActions.setVisibility(View.GONE);
        }

        // Photo loading using the unified ID ivVisitorPhoto
        if (h.ivVisitorPhoto != null) {
            String imgUrl = v.getImageUrl();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                Glide.with(context)
                        .load(imgUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(h.ivVisitorPhoto);
            } else {
                h.ivVisitorPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        // Button clicks
        if (h.btnApprove != null) {
            h.btnApprove.setOnClickListener(view -> {
                if (listener != null) listener.onApprove(v.getDocId(), pos);
            });
        }
        if (h.btnReject != null) {
            h.btnReject.setOnClickListener(view -> {
                if (listener != null) listener.onReject(v.getDocId(), pos);
            });
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVisitorPhoto;
        TextView tvName, tvFlat, tvPurpose, tvStatus, tvTime;
        View layoutActions;
        Button btnApprove, btnReject;

        ViewHolder(View v) {
            super(v);
            tvName         = v.findViewById(R.id.tvVisitorName);
            tvPurpose      = v.findViewById(R.id.tvPurpose);
            tvTime         = v.findViewById(R.id.tvTime);
            btnApprove     = v.findViewById(R.id.btnApprove);
            btnReject      = v.findViewById(R.id.btnReject);
            ivVisitorPhoto = v.findViewById(R.id.ivVisitorPhoto);
            tvFlat         = v.findViewById(R.id.tvFlat);
            tvStatus       = v.findViewById(R.id.tvStatus);
            layoutActions  = v.findViewById(R.id.layoutActions);
            
            // Fallback for layoutActions if not explicitly found
            if (layoutActions == null && btnApprove != null) {
                layoutActions = (View) btnApprove.getParent();
            }
        }
    }
}
