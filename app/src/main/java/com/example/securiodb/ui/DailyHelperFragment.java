package com.example.securiodb.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.R;
import com.example.securiodb.adapter.HelperAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyHelperFragment extends Fragment {

    private String flatNo;
    private FirebaseFirestore db;
    private RecyclerView recycler;
    private LinearLayout layoutEmpty;
    private List<Map<String, Object>> helperList = new ArrayList<>();
    private HelperAdapter adapter;
    private ListenerRegistration listenerReg;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_helper, container, false);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null)
            flatNo = getArguments().getString("flatNo", "");

        recycler    = view.findViewById(R.id.recyclerHelpers);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        adapter = new HelperAdapter(requireContext(), helperList, new HelperAdapter.OnHelperClickListener() {
            @Override
            public void onDelete(String docId) {
                deleteHelper(docId);
            }

            @Override
            public void onToggle(String docId, boolean isActive) {
                toggleActive(docId, isActive);
            }
        });
        
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        view.findViewById(R.id.fabAddHelper).setOnClickListener(v -> showAddHelperDialog());

        loadHelpers();
        return view;
    }

    private void loadHelpers() {
        if (flatNo == null || flatNo.isEmpty()) return;

        listenerReg = db.collection("dailyHelpers")
            .whereEqualTo("flatNumber", flatNo) // Standardized to flatNumber
            .orderBy("addedOn", Query.Direction.DESCENDING)
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                helperList.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("docId", doc.getId());
                        helperList.add(data);
                    }
                }
                adapter.notifyDataSetChanged();
                layoutEmpty.setVisibility(helperList.isEmpty() ? View.VISIBLE : View.GONE);
                recycler.setVisibility(helperList.isEmpty() ? View.GONE : View.VISIBLE);
            });
    }

    private void showAddHelperDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_helper, null);

        TextInputEditText etName  = dialogView.findViewById(R.id.etHelperName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etHelperPhone);
        TextInputEditText etTime  = dialogView.findViewById(R.id.etHelperTime);
        Spinner spinnerType       = dialogView.findViewById(R.id.spinnerType);
        
        CheckBox cbMon = dialogView.findViewById(R.id.cbMon);
        CheckBox cbTue = dialogView.findViewById(R.id.cbTue);
        CheckBox cbWed = dialogView.findViewById(R.id.cbWed);
        CheckBox cbThu = dialogView.findViewById(R.id.cbThu);
        CheckBox cbFri = dialogView.findViewById(R.id.cbFri);
        CheckBox cbSat = dialogView.findViewById(R.id.cbSat);
        CheckBox cbSun = dialogView.findViewById(R.id.cbSun);
        CheckBox cbDaily = dialogView.findViewById(R.id.cbDaily);

        cbDaily.setOnCheckedChangeListener((btn, isChecked) -> {
            cbMon.setEnabled(!isChecked); cbTue.setEnabled(!isChecked);
            cbWed.setEnabled(!isChecked); cbThu.setEnabled(!isChecked);
            cbFri.setEnabled(!isChecked); cbSat.setEnabled(!isChecked);
            cbSun.setEnabled(!isChecked);
        });

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item,
            new String[]{"Maid", "Cook", "Driver", "Gardener", "Other"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Register Daily Helper")
            .setView(dialogView)
            .setPositiveButton("Register", (d, w) -> {
                String name  = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String time  = etTime.getText().toString().trim();
                String type  = spinnerType.getSelectedItem().toString();

                if (name.isEmpty() || phone.length() != 10) {
                    Toast.makeText(requireContext(), "Enter valid name and phone", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> schedule = new ArrayList<>();
                if (cbDaily.isChecked()) {
                    schedule.add("Daily");
                } else {
                    if (cbMon.isChecked()) schedule.add("Monday");
                    if (cbTue.isChecked()) schedule.add("Tuesday");
                    if (cbWed.isChecked()) schedule.add("Wednesday");
                    if (cbThu.isChecked()) schedule.add("Thursday");
                    if (cbFri.isChecked()) schedule.add("Friday");
                    if (cbSat.isChecked()) schedule.add("Saturday");
                    if (cbSun.isChecked()) schedule.add("Sunday");
                }

                if (schedule.isEmpty()) {
                    Toast.makeText(requireContext(), "Select at least one day", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveHelper(name, phone, type, schedule, time.isEmpty() ? "Any time" : time);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveHelper(String name, String phone, String type, List<String> schedule, String entryTime) {
        String ownerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> helper = new HashMap<>();
        helper.put("flatNumber",  flatNo); // Standardized to flatNumber
        helper.put("ownerUid",    ownerUid);
        helper.put("helperName",  name);
        helper.put("helperPhone", phone);
        helper.put("helperType",  type);
        helper.put("schedule",    schedule);
        helper.put("entryTime",   entryTime);
        helper.put("isActive",    true);
        helper.put("addedOn",     System.currentTimeMillis());

        db.collection("dailyHelpers").add(helper)
            .addOnSuccessListener(ref -> Toast.makeText(requireContext(), name + " registered!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteHelper(String docId) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Helper")
            .setMessage("Remove this daily helper?")
            .setPositiveButton("Remove", (d, w) -> db.collection("dailyHelpers").document(docId).delete())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void toggleActive(String docId, boolean isActive) {
        db.collection("dailyHelpers").document(docId).update("isActive", isActive);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (listenerReg != null) listenerReg.remove();
    }
}
