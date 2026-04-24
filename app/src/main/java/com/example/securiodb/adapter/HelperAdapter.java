package com.example.securiodb.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.HelperHistoryActivity;
import com.example.securiodb.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HelperAdapter extends RecyclerView.Adapter<HelperAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> list;
    private OnHelperClickListener listener;

    public interface OnHelperClickListener {
        void onDelete(String docId);
        void onToggle(String docId, boolean isActive);
    }

    public HelperAdapter(Context context, List<Map<String, Object>> list, OnHelperClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_helper, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Map<String, Object> helper = list.get(position);
        String docId = (String) helper.get("docId");
        String name = (String) helper.get("helperName");
        String phone = (String) helper.get("helperPhone");
        String type = (String) helper.get("helperType");
        String time = (String) helper.get("entryTime");
        boolean isActive = helper.get("isActive") != null && (boolean) helper.get("isActive");
        List<String> schedule = (List<String>) helper.get("schedule");

        h.tvName.setText(name);
        h.tvType.setText(type);
        h.tvTime.setText("Expected: " + time);
        
        // Reset listener to avoid triggering it during binding
        h.switchActive.setOnCheckedChangeListener(null);
        h.switchActive.setChecked(isActive);

        if (schedule != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < schedule.size(); i++) {
                sb.append(schedule.get(i));
                if (i < schedule.size() - 1) sb.append(", ");
            }
            h.tvSchedule.setText(sb.toString());
        }

        // Format last seen
        Object lastSeen = helper.get("lastSeen");
        if (lastSeen instanceof Long && (Long) lastSeen > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            h.tvLastSeen.setText("Last seen: " + sdf.format(new Date((Long) lastSeen)));
        } else {
            h.tvLastSeen.setText("Last seen: Never recorded");
        }

        h.switchActive.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) listener.onToggle(docId, isChecked);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(docId);
        });

        h.btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            context.startActivity(intent);
        });

        h.btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(context, HelperHistoryActivity.class);
            intent.putExtra("HELPER_ID", docId);
            intent.putExtra("HELPER_NAME", name);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvSchedule, tvTime, tvLastSeen;
        SwitchMaterial switchActive;
        MaterialButton btnCall, btnDelete, btnHistory;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvHelperName);
            tvType = v.findViewById(R.id.tvHelperType);
            tvSchedule = v.findViewById(R.id.tvSchedule);
            tvTime = v.findViewById(R.id.tvEntryTime);
            tvLastSeen = v.findViewById(R.id.tvLastSeen);
            switchActive = v.findViewById(R.id.switchActive);
            btnCall = v.findViewById(R.id.btnCallHelper);
            btnDelete = v.findViewById(R.id.btnDeleteHelper);
            btnHistory = v.findViewById(R.id.btnHistory);
        }
    }
}
