package com.example.securiodb;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.example.securiodb.models.Visitor;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsActivity extends AppCompatActivity {

    private TextView tvTotalVisitors, tvTotalDeliveries;
    private BarChart barChart;
    private MaterialButton btnDateRange, btnExport;
    
    private FirebaseFirestore db;
    private List<Visitor> reportData = new ArrayList<>();
    private long startDateMs, endDateMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupDatePicker();
        
        // Default: Last 7 days
        Calendar cal = Calendar.getInstance();
        endDateMs = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        startDateMs = cal.getTimeInMillis();
        
        fetchReportData();

        btnExport.setOnClickListener(v -> exportToCSV());
    }

    private void initViews() {
        tvTotalVisitors = findViewById(R.id.tvTotalVisitors);
        tvTotalDeliveries = findViewById(R.id.tvTotalDeliveries);
        barChart = findViewById(R.id.barChart);
        btnDateRange = findViewById(R.id.btnDateRange);
        btnExport = findViewById(R.id.btnExportCSV);
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .build();

        btnDateRange.setOnClickListener(v -> picker.show(getSupportFragmentManager(), "RANGE_PICKER"));

        picker.addOnPositiveButtonClickListener(selection -> {
            startDateMs = selection.first;
            endDateMs = selection.second;
            fetchReportData();
        });
    }

    private void fetchReportData() {
        db.collection("visitors")
                .whereGreaterThanOrEqualTo("timestamp", new Date(startDateMs))
                .whereLessThanOrEqualTo("timestamp", new Date(endDateMs))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reportData.clear();
                    int visitors = 0;
                    int deliveries = 0;
                    Map<String, Integer> dayCounts = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Visitor v = doc.toObject(Visitor.class);
                        reportData.add(v);

                        if ("Visitor".equalsIgnoreCase(v.getPurpose())) visitors++;
                        else if ("Delivery".equalsIgnoreCase(v.getPurpose())) deliveries++;

                        // Group by day for chart
                        String day = new SimpleDateFormat("EEE", Locale.getDefault()).format(v.getTimestamp().toDate());
                        dayCounts.put(day, dayCounts.getOrDefault(day, 0) + 1);
                    }

                    tvTotalVisitors.setText(String.valueOf(visitors));
                    tvTotalDeliveries.setText(String.valueOf(deliveries));
                    updateChart(dayCounts);
                });
    }

    private void updateChart(Map<String, Integer> counts) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        // Simplified: Last 7 days labels
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            String day = new SimpleDateFormat("EEE", Locale.getDefault()).format(cal.getTime());
            labels.add(day);
            entries.add(new BarEntry(i, counts.getOrDefault(day, 0)));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Visitors");
        dataSet.setColor(getResources().getColor(R.color.admin_primary));
        
        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setGranularity(1f);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void exportToCSV() {
        StringBuilder csv = new StringBuilder("Name,Phone,Flat,Purpose,Status,Date\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        
        for (Visitor v : reportData) {
            csv.append(v.getName()).append(",")
               .append(v.getPhone()).append(",")
               .append(v.getFlatNumber()).append(",")
               .append(v.getPurpose()).append(",")
               .append(v.getStatus()).append(",")
               .append(sdf.format(v.getTimestamp().toDate())).append("\n");
        }

        String fileName = "Security_Report_" + System.currentTimeMillis() + ".csv";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
        try {
            if (uri != null) {
                OutputStream out = getContentResolver().openOutputStream(uri);
                out.write(csv.toString().getBytes());
                out.close();
                Toast.makeText(this, "Report saved to Downloads", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }
}
