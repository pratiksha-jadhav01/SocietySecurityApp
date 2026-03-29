package com.example.securiodb;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class VisitorEntryActivity extends AppCompatActivity {

    // UI elements
    private ImageView imgVisitorPhoto;
    private MaterialButton btnCapturePhoto, btnSubmit;
    private TextInputLayout tilName, tilPhone, tilFlatNo, tilPurpose;
    private TextInputEditText etName, etPhone, etFlatNo, etPurpose;

    // Camera variables
    private Bitmap capturedPhotoBitmap = null; // stores photo in memory only
    private boolean isPhotoCaptured = false;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_CAPTURE_CODE    = 101;

    // Firebase
    private FirebaseFirestore db;
    private String guardUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_entry);

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            guardUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Bind views
        imgVisitorPhoto = findViewById(R.id.imgVisitorPhoto);
        btnCapturePhoto = findViewById(R.id.btnCapturePhoto);
        btnSubmit       = findViewById(R.id.btnSubmit);
        tilName    = findViewById(R.id.tilName);
        tilPhone   = findViewById(R.id.tilPhone);
        tilFlatNo  = findViewById(R.id.tilFlatNo);
        tilPurpose = findViewById(R.id.tilPurpose);
        etName    = findViewById(R.id.etName);
        etPhone   = findViewById(R.id.etPhone);
        etFlatNo  = findViewById(R.id.etFlatNo);
        etPurpose = findViewById(R.id.etPurpose);

        // Toolbar back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Button listeners
        btnCapturePhoto.setOnClickListener(v -> checkCameraPermission());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    // ─── STEP 1: Check camera permission ───────────────────────
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(); // permission granted
            } else {
                Toast.makeText(this,
                    "Camera permission denied",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ─── STEP 2: Open camera — thumbnail mode (NO FileProvider needed) ──
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_CAPTURE_CODE);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── STEP 3: Handle camera result ──────────────────────────
    @Override
    protected void onActivityResult(int requestCode,
            int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_CAPTURE_CODE
                && resultCode == RESULT_OK) {

            if (data != null && data.getExtras() != null) {
                capturedPhotoBitmap = (Bitmap) data.getExtras().get("data");

                if (capturedPhotoBitmap != null) {
                    imgVisitorPhoto.setImageBitmap(capturedPhotoBitmap);
                    btnCapturePhoto.setText("🔄 Retake Photo");
                    isPhotoCaptured = true;
                } else {
                    Toast.makeText(this,
                        "Could not capture photo, try again",
                        Toast.LENGTH_SHORT).show();
                }
            }

        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── STEP 4: Validate form fields ──────────────────────────
    private void validateAndSubmit() {
        String name    = etName.getText().toString().trim();
        String phone   = etPhone.getText().toString().trim();
        String flatNo  = etFlatNo.getText().toString().trim().toUpperCase();
        String purpose = etPurpose.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            tilName.setError("Visitor name is required");
            isValid = false;
        } else { tilName.setError(null); }

        if (phone.isEmpty() || phone.length() != 10) {
            tilPhone.setError("Enter valid 10-digit number");
            isValid = false;
        } else { tilPhone.setError(null); }

        if (flatNo.isEmpty()) {
            tilFlatNo.setError("Flat number is required");
            isValid = false;
        } else { tilFlatNo.setError(null); }

        if (purpose.isEmpty()) {
            tilPurpose.setError("Purpose is required");
            isValid = false;
        } else { tilPurpose.setError(null); }

        if (!isValid) return; 

        saveVisitorToFirestore(name, phone, flatNo, purpose);
    }

    // ─── STEP 5: Save to Firestore ────
    private void saveVisitorToFirestore(String name, String phone,
            String flatNo, String purpose) {

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        Map<String, Object> visitorData = new HashMap<>();
        visitorData.put("name",        name);
        visitorData.put("phone",       phone);
        visitorData.put("flatNumber",  flatNo); // MATCHES OwnerDashboard filter
        visitorData.put("purpose",     purpose);
        visitorData.put("imageUrl",    "not_used"); 
        visitorData.put("status",      "Pending");
        visitorData.put("createdBy",   guardUid);
        visitorData.put("approvedBy",  null);
        visitorData.put("entryTime",   FieldValue.serverTimestamp());
        visitorData.put("exitTime",    null);
        visitorData.put("timestamp",   FieldValue.serverTimestamp());

        db.collection("visitors")
            .add(visitorData)
            .addOnSuccessListener(docRef -> {
                sendNotificationToOwner(name, flatNo, "Visitor");
                Toast.makeText(this,
                    "✅ Visitor entry submitted!",
                    Toast.LENGTH_SHORT).show();
                finish(); 
            })
            .addOnFailureListener(e -> {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("SUBMIT ENTRY");
                Toast.makeText(this,
                    "❌ Failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            });
    }

    private void sendNotificationToOwner(String visitorName, String flatNo, String type) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "New " + type + " Arrival");
        notification.put("body", visitorName + " has arrived for Flat " + flatNo);
        notification.put("flatNumber", flatNo);
        notification.put("type", type.toLowerCase());
        notification.put("timestamp", FieldValue.serverTimestamp());
        notification.put("isRead", false);

        db.collection("notifications").add(notification);
    }
}
