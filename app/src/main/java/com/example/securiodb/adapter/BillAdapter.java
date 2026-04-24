package com.example.securiodb.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.securiodb.R;
import com.example.securiodb.models.Bill;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {

    private Context context;
    private List<Bill> billList;
    private boolean isAdmin;
    private OnBillActionListener listener;

    public interface OnBillActionListener {
        void onAction(Bill bill);
    }

    public BillAdapter(Context context, List<Bill> billList, boolean isAdmin, OnBillActionListener listener) {
        this.context = context;
        this.billList = billList;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        Bill bill = billList.get(position);
        holder.tvMonth.setText(bill.getMonth());
        holder.tvAmount.setText("₹ " + bill.getAmount());
        holder.tvDueDate.setText(bill.getDueDate());
        holder.tvStatus.setText(bill.getStatus().toUpperCase());

        if ("due".equalsIgnoreCase(bill.getStatus())) {
            holder.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_rejected_bg));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_rejected_text));
            
            // Only show action button if it's Admin (Mark as Paid)
            // Owners will not see the "Pay Now" option as requested
            if (isAdmin) {
                holder.btnAction.setVisibility(View.VISIBLE);
                holder.btnAction.setText("MARK AS PAID");
            } else {
                holder.btnAction.setVisibility(View.GONE);
            }
        } else {
            holder.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_approved_bg));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_approved_text));
            holder.btnAction.setVisibility(View.GONE);
        }

        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) listener.onAction(bill);
        });
    }

    @Override
    public int getItemCount() {
        return billList.size();
    }

    public static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth, tvAmount, tvDueDate, tvStatus;
        MaterialCardView cardStatus;
        MaterialButton btnAction;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvBillMonth);
            tvAmount = itemView.findViewById(R.id.tvBillAmount);
            tvDueDate = itemView.findViewById(R.id.tvBillDueDate);
            tvStatus = itemView.findViewById(R.id.tvBillStatus);
            cardStatus = itemView.findViewById(R.id.cardStatus);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
