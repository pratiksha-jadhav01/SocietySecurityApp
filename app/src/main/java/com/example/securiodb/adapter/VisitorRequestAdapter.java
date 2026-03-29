package com.example.securiodb.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VisitorRequestAdapter extends
        RecyclerView.Adapter<VisitorRequestAdapter.ViewHolder> {

    public interface Callback { void onAction(String docId, int position); }

    private List<Map<String, Object>> list;
    private Context context;
    private Callback onApprove, onReject;

    public VisitorRequestAdapter(Context ctx,
            List<Map<String, Object>> list,
            Callback onApprove, Callback onReject) {
        this.context   = ctx;
        this.list      = list;
        this.onApprove = onApprove;
        this.onReject  = onReject;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
            .inflate(R.layout.item_visitor_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Map<String, Object> visitor = list.get(pos);
        String docId = (String) visitor.get("docId");

        String name    = getStr(visitor, "name",    "Unknown");
        String phone   = getStr(visitor, "phone",   "—");
        String flatNo  = getStr(visitor, "flatNo",  "—");
        String purpose = getStr(visitor, "purpose", "—");

        // Avatar: first letter of name
        h.tvAvatar.setText(name.isEmpty() ? "?" :
            String.valueOf(name.charAt(0)).toUpperCase());
        h.tvName.setText(name);
        h.tvPhone.setText("📞 " + phone);
        h.tvFlat.setText("🏠 Flat: " + flatNo);
        h.tvPurpose.setText("📋 Purpose: " + purpose);

        // Format timestamp
        Object ts = visitor.get("timestamp");
        if (ts instanceof Timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat(
                "dd MMM, hh:mm a", Locale.getDefault());
            h.tvTime.setText(sdf.format(((Timestamp) ts).toDate()));
        } else {
            h.tvTime.setText("Just now");
        }

        // Button listeners with null safety
        h.btnApprove.setOnClickListener(v -> {
            if (docId != null) onApprove.onAction(docId, h.getAdapterPosition());
        });
        h.btnReject.setOnClickListener(v -> {
            if (docId != null) onReject.onAction(docId, h.getAdapterPosition());
        });
    }

    // Safe string getter — prevents NPE on missing Firestore fields
    private String getStr(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return (val instanceof String && !((String)val).isEmpty())
            ? (String) val : def;
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvTime, tvPhone, tvFlat, tvPurpose;
        MaterialButton btnApprove, btnReject;
        ViewHolder(View v) {
            super(v);
            tvAvatar  = v.findViewById(R.id.tvAvatar);
            tvName    = v.findViewById(R.id.tvName);
            tvTime    = v.findViewById(R.id.tvTime);
            tvPhone   = v.findViewById(R.id.tvPhone);
            tvFlat    = v.findViewById(R.id.tvFlat);
            tvPurpose = v.findViewById(R.id.tvPurpose);
            btnApprove = v.findViewById(R.id.btnApprove);
            btnReject  = v.findViewById(R.id.btnReject);
        }
    }
}
