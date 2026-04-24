package com.example.securiodb.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.R;

import java.util.List;

public class BillHistoryAdapter extends RecyclerView.Adapter<BillHistoryAdapter.ViewHolder> {

    private List<BillItem> billList;

    public static class BillItem {
        public String month, amount, dueDate, status;
        public BillItem(String month, String amount, String dueDate, String status) {
            this.month = month;
            this.amount = amount;
            this.dueDate = dueDate;
            this.status = status;
        }
    }

    public BillHistoryAdapter(List<BillItem> billList) {
        this.billList = billList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BillItem item = billList.get(position);
        holder.tvMonth.setText(item.month);
        holder.tvAmount.setText(item.amount);
        holder.tvDueDate.setText("Due: " + item.dueDate);

        if ("Paid".equalsIgnoreCase(item.status)) {
            holder.tvBillStatus.setText("PAID");
            holder.tvBillStatus.setTextColor(Color.parseColor("#2E7D32"));
            holder.tvBillStatus.setBackgroundResource(R.drawable.bg_badge_paid);
        } else {
            holder.tvBillStatus.setText("UNPAID");
            holder.tvBillStatus.setTextColor(Color.parseColor("#C62828"));
            holder.tvBillStatus.setBackgroundResource(R.drawable.bg_badge_unpaid);
        }
    }

    @Override
    public int getItemCount() {
        return billList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth, tvAmount, tvDueDate, tvBillStatus;
        public ViewHolder(View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvBillStatus = itemView.findViewById(R.id.tvBillStatus);
        }
    }
}
