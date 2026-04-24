package com.example.securiodb;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GuardHelperAdapter extends RecyclerView.Adapter<GuardHelperAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> helpers;

    public GuardHelperAdapter(Context context, List<Map<String, Object>> helpers) {
        this.context = context;
        this.helpers = helpers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_guard_helper, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> helper = helpers.get(position);

        String name = (String) helper.getOrDefault("helperName", "Unknown");
        String type = (String) helper.getOrDefault("helperType", "Staff");
        
        // Handle both possible field names for flat number
        String flat = (String) helper.get("flatNumber");
        if (flat == null) flat = (String) helper.get("flatNo");
        if (flat == null) flat = "---";

        List<String> schedule = (List<String>) helper.get("schedule");

        holder.tvHelperName.setText(name);
        holder.tvHelperType.setText(type);
        holder.tvFlatNo.setText("Flat: " + flat);

        // Avatar Letter
        if (name != null && !name.isEmpty()) {
            holder.tvAvatarLetter.setText(name.substring(0, 1).toUpperCase());
        }

        // Duty Status Logic
        String today = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
        boolean onDutyToday = schedule != null &&
            (schedule.contains("Daily") || schedule.contains(today));

        holder.tvDutyStatus.setText(onDutyToday ? "On duty today" : "Off today");
        holder.tvDutyStatus.setBackgroundResource(onDutyToday ? R.drawable.badge_approved_dark : R.drawable.badge_neutral);
        holder.tvDutyStatus.setTextColor(onDutyToday ? Color.WHITE : Color.parseColor("#8892B0"));

        // Schedule pills row
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        String[] fullDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        
        holder.scheduleRow.removeAllViews();

        if (schedule != null && schedule.contains("Daily")) {
            // Show single "Daily" pill
            holder.scheduleRow.addView(makePill("Daily", true));
        } else {
            for (int i = 0; i < days.length; i++) {
                boolean inSchedule = schedule != null && schedule.contains(fullDays[i]);
                boolean isToday = fullDays[i].equals(today);
                
                // Highlight only if it's in schedule AND it's today
                TextView pill = makePill(days[i], inSchedule && isToday);
                
                // Fade days not in schedule
                pill.setAlpha(inSchedule ? 1.0f : 0.25f);
                holder.scheduleRow.addView(pill);
            }
        }
    }

    private TextView makePill(String text, boolean highlight) {
        TextView tv = new TextView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 8, 0);
        tv.setLayoutParams(lp);
        tv.setText(text);
        tv.setTextSize(9);
        tv.setPadding(12, 4, 12, 4);
        tv.setBackgroundResource(highlight ? R.drawable.chip_selected : R.drawable.chip_normal);
        tv.setTextColor(highlight ? Color.WHITE : Color.parseColor("#8892B0"));
        return tv;
    }

    @Override
    public int getItemCount() {
        return helpers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHelperName, tvHelperType, tvFlatNo, tvAvatarLetter, tvDutyStatus;
        LinearLayout scheduleRow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHelperName   = itemView.findViewById(R.id.tvHelperName);
            tvHelperType   = itemView.findViewById(R.id.tvHelperType);
            tvFlatNo       = itemView.findViewById(R.id.tvFlatNo);
            tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
            tvDutyStatus   = itemView.findViewById(R.id.tvDutyStatus);
            scheduleRow    = itemView.findViewById(R.id.scheduleRow);
        }
    }
}
