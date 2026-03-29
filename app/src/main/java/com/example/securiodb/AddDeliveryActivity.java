package com.example.securiodb;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddDeliveryActivity extends AppCompatActivity {

    private TextInputEditText etName, etOtherCompany, etFlat, etPackage;
    private TextInputLayout tilOtherCompany;
    private ChipGroup chipGroupCompany;
    private ProgressBar progressBar;
    
    private FirebaseFirestore db;
    private String guardUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_delivery);

        db = FirebaseFirestore.getInstance();
        guardUid = FirebaseAuth.getInstance().getUid();

        initViews();
    }

    private void initViews() {
        etName = findViewById(R.id.etDeliveryName);
        etOtherCompany = findViewById(R.id.etOtherCompany);
        etFlat = findViewById(R.id.etFlatNumber);
        etPackage = findViewById(R.id.etPackageType);
        tilOtherCompany = findViewById(R.id.tilOtherCompany);
        chipGroupCompany = findViewById(R.id.chipGroupCompany);
        progressBar = findViewById(R.id.progressBar);

        chipGroupCompany.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipOtherCompany)) {
                tilOtherCompany.setVisibility(View.VISIBLE);
            } else {
                tilOtherCompany.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btnSubmit).setOnClickListener(v -> validateAndSubmit());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void validateAndSubmit() {
        String name = etName.getText().toString().trim();
        String flat = etFlat.getText().toString().trim();
        String packageType = etPackage.getText().toString().trim();
        int chipId = chipGroupCompany.getCheckedChipId();

        if (name.isEmpty() || flat.isEmpty() || packageType.isEmpty() || chipId == -1) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String company;
        if (chipId == R.id.chipOtherCompany) {
            company = etOtherCompany.getText().toString().trim();
            if (company.isEmpty()) {
                Toast.makeText(this, "Please specify company name", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Chip selectedChip = findViewById(chipId);
            company = selectedChip.getText().toString();
        }

        saveDelivery(name, company, flat, packageType);
    }

    private void saveDelivery(String name, String company, String flat, String packageType) {
        progressBar.setVisibility(View.VISIBLE);
        
        Map<String, Object> delivery = new HashMap<>();
        delivery.put("name", name);
        delivery.put("company", company);
        delivery.put("flatNumber", flat);
        delivery.put("packageType", packageType);
        delivery.put("purpose", "Delivery");
        delivery.put("status", "Pending"); // Can be auto-approved based on settings
        delivery.put("createdBy", guardUid);
        delivery.put("entryTime", FieldValue.serverTimestamp());
        delivery.put("timestamp", FieldValue.serverTimestamp());
        delivery.put("exitTime", null);

        db.collection("visitors").add(delivery)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Delivery logged successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
