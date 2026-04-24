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

import com.example.securiodb.R;
import com.example.securiodb.models.AdminBillItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdminBillAdapter extends RecyclerView.Adapter<AdminBillAdapter.ViewHolder> {

    private Context context;
    private List<AdminBillItem> list;
    private FirebaseFirestore db;

    public AdminBillAdapter(Context context, List<AdminBillItem> list) {
        this.context = context;
        this.list = list;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_admin_bill, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminBillItem item = list.get(position);
        
        holder.tvUserId.setText(item.getUserId());
        holder.tvMonth.setText(item.getMonth());

        if ("Paid".equalsIgnoreCase(item.getStatus())) {
            holder.tvStatus.setText("PAID");
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_paid);
        } else {
            holder.tvStatus.setText("UNPAID");
            holder.tvStatus.setTextColor(Color.parseColor("#C62828"));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
        }

        holder.ivDelete.setOnClickListener(v -> showDeleteDialog(item.getDocumentId(), position));
    }

    private void showDeleteDialog(String docId, int position) {
        if (docId == null) return;
        
        new AlertDialog.Builder(context)
                .setTitle("Delete Bill")
                .setMessage("Are you sure you want to delete this bill record?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("bills").document(docId).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(context, "Bill deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserId, tvMonth, tvStatus;
        ImageView ivDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserId = itemView.findViewById(R.id.tvUserId);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivDelete = itemView.findViewById(R.id.ivDeleteBill);
        }
    }
}
