package com.example.securiodb;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for guards to record new visitor entries.
 * Integrated with Firestore for storage and Realtime Database for instant owner notifications.
 */
public class AddVisitorActivity extends AppCompatActivity {

    private ImageView imgPreview;
    private TextInputLayout tilName, tilPhone, tilFlat, tilPurpose;
    private TextInputEditText etName, etPhone, etFlat, etPurpose;
    private MaterialButton btnCamera, btnGallery, btnSubmit;
    private TextView tvTitle;

    private MaterialCardView cardHelperFound, cardHelperNotScheduled;
    private TextView tvHelperName, tvHelperType, tvHelperSchedule, tvNotScheduledMsg, tvHelperFlat;

    private Uri selectedImageUri = null;
    private static final int CAMERA_REQUEST  = 101;
    private static final int GALLERY_REQUEST = 102;
    private static final int CAM_PERM_CODE   = 201;
    private static final int GAL_PERM_CODE   = 202;

    private FirebaseFirestore db;
    private DatabaseReference mRealtimeDb;
    private ProgressDialog progressDialog;
    private boolean isPreApprove = false;

    private boolean isHelperDetected = false;
    private boolean isHelperScheduledToday = false;
    private String detectedHelperDocId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_visitor);

        db = FirebaseFirestore.getInstance();
        mRealtimeDb = FirebaseDatabase.getInstance().getReference("visitors");
        isPreApprove = getIntent().getBooleanExtra("PRE_APPROVE", false);

        initViews();
        setupHelperDetection();

        if (isPreApprove) {
            if (tvTitle != null) tvTitle.setText("Pre-Approve Visitor");
            btnSubmit.setText("PRE-APPROVE VISITOR");
            
            String ownerFlat = getIntent().getStringExtra("OWNER_FLAT");
            if (ownerFlat != null && !ownerFlat.isEmpty()) {
                etFlat.setText(ownerFlat);
                etFlat.setEnabled(false);
            }
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnCamera.setOnClickListener(v -> checkCameraPermission());
        btnGallery.setOnClickListener(v -> checkGalleryPermission());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);
    }

    private void initViews() {
        imgPreview  = findViewById(R.id.imgPreview);
        tilName     = findViewById(R.id.tilName);
        tilPhone    = findViewById(R.id.tilPhone);
        tilFlat     = findViewById(R.id.tilFlat);
        tilPurpose  = findViewById(R.id.tilPurpose);
        etName      = findViewById(R.id.etName);
        etPhone     = findViewById(R.id.etPhone);
        etFlat      = findViewById(R.id.etFlat);
        etPurpose   = findViewById(R.id.etPurpose);
        btnCamera   = findViewById(R.id.btnCamera);
        btnGallery  = findViewById(R.id.btnGallery);
        btnSubmit   = findViewById(R.id.btnSubmit);
        tvTitle     = findViewById(R.id.tvTitle);

        cardHelperFound = findViewById(R.id.cardHelperFound);
        cardHelperNotScheduled = findViewById(R.id.cardHelperNotScheduled);
        tvHelperName = findViewById(R.id.tvHelperName);
        tvHelperType = findViewById(R.id.tvHelperType);
        tvHelperSchedule = findViewById(R.id.tvHelperSchedule);
        tvNotScheduledMsg = findViewById(R.id.tvNotScheduledMsg);
        tvHelperFlat = findViewById(R.id.tvHelperFlat);
    }

    private void setupHelperDetection() {
        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                String flat  = etFlat.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                // Trigger detection whenever we have a flat number and a full 10-digit phone
                if (flat.length() >= 1 && phone.length() == 10) {
                    checkIfDailyHelper(flat, phone);
                } else {
                    hideBanners();
                }
            }
        };
        etFlat.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);
    }

    private void hideBanners() {
        if (cardHelperFound != null) cardHelperFound.setVisibility(View.GONE);
        if (cardHelperNotScheduled != null) cardHelperNotScheduled.setVisibility(View.GONE);
        isHelperDetected = false;
        isHelperScheduledToday = false;
        if (btnSubmit != null) {
            btnSubmit.setText(isPreApprove ? "PRE-APPROVE VISITOR" : "SUBMIT ENTRY");
            btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A2347")));
        }
    }

    private void checkIfDailyHelper(String flat, String phone) {
        String today = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());

        db.collection("dailyHelpers")
            .whereEqualTo("helperPhone", phone)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener(snap -> {
                if (snap.isEmpty()) {
                    hideBanners();
                    return;
                }

                DocumentSnapshot docMatch = null;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    String hFlat = doc.getString("flatNumber");
                    if (hFlat == null) hFlat = doc.getString("flatNo");
                    
                    if (flat.equalsIgnoreCase(hFlat)) {
                        docMatch = doc;
                        break;
                    }
                }

                if (docMatch == null) {
                    hideBanners();
                    return;
                }

                isHelperDetected  = true;
                detectedHelperDocId = docMatch.getId();
                String helperName = docMatch.getString("helperName");
                String helperType = docMatch.getString("helperType");
                String helperFlatNum = docMatch.getString("flatNumber");
                if (helperFlatNum == null) helperFlatNum = docMatch.getString("flatNo");
                List<String> schedule = (List<String>) docMatch.get("schedule");

                boolean scheduledToday = schedule != null && (schedule.contains("Daily") || schedule.contains(today));

                if (scheduledToday) {
                    isHelperScheduledToday = true;
                    if (cardHelperFound != null) cardHelperFound.setVisibility(View.VISIBLE);
                    if (cardHelperNotScheduled != null) cardHelperNotScheduled.setVisibility(View.GONE);

                    if (tvHelperName != null) tvHelperName.setText("Name: " + helperName);
                    if (tvHelperType != null) tvHelperType.setText("Type: " + helperType);
                    if (tvHelperFlat != null) tvHelperFlat.setText("Flat: " + helperFlatNum);
                    etName.setText(helperName);
                    etPurpose.setText("Daily Helper — " + helperType);

                    if (btnSubmit != null) {
                        btnSubmit.setText("ALLOW ENTRY (Auto-Approved)");
                        btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                    }
                } else {
                    isHelperScheduledToday = false;
                    if (cardHelperFound != null) cardHelperFound.setVisibility(View.GONE);
                    if (cardHelperNotScheduled != null) cardHelperNotScheduled.setVisibility(View.VISIBLE);
                    if (btnSubmit != null) {
                        btnSubmit.setText(isPreApprove ? "PRE-APPROVE VISITOR" : "SUBMIT ENTRY");
                        btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A2347")));
                    }
                }
            })
            .addOnFailureListener(e -> hideBanners());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAM_PERM_CODE);
        }
    }

    private void openCamera() {
        try {
            File photoFile = File.createTempFile("visitor_" + System.currentTimeMillis(), ".jpg", getCacheDir());
            selectedImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (intent.resolveActivity(getPackageManager()) != null) startActivityForResult(intent, CAMERA_REQUEST);
        } catch (IOException e) {
            Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkGalleryPermission() {
        String perm = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) openGallery();
        else ActivityCompat.requestPermissions(this, new String[]{perm}, GAL_PERM_CODE);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (code == CAM_PERM_CODE) openCamera(); else openGallery();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;
        if (req == CAMERA_REQUEST) {
            if (selectedImageUri != null) {
                imgPreview.setImageURI(null);
                imgPreview.setImageURI(selectedImageUri);
                imgPreview.setImageTintList(null); // Clear placeholder tint to show photo
            }
        } else if (req == GALLERY_REQUEST && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Glide.with(this).load(selectedImageUri).into(imgPreview);
                imgPreview.setImageTintList(null);
            }
        }
    }

    private void validateAndSubmit() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String flat = etFlat.getText().toString().trim().toUpperCase();
        String purpose = etPurpose.getText().toString().trim();

        if (name.isEmpty() || phone.length() != 10 || flat.isEmpty() || purpose.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isHelperDetected && isHelperScheduledToday) {
            saveAutoApprovedHelper(name, phone, flat, purpose);
        } else {
            if (selectedImageUri != null) uploadToCloudinary(name, phone, flat, purpose);
            else saveNormalVisitor(name, phone, flat, purpose, "");
        }
    }

    private void uploadToCloudinary(String name, String phone, String flat, String purpose) {
        progressDialog.show();
        CloudinaryUploader.uploadImage(this, selectedImageUri, new CloudinaryUploader.UploadCallback() {
            @Override public void onSuccess(String imageUrl) {
                runOnUiThread(() -> saveNormalVisitor(name, phone, flat, purpose, imageUrl));
            }
            @Override public void onFailure(String err) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(AddVisitorActivity.this, "Upload failed: " + err, Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onProgress(int p) {
                runOnUiThread(() -> progressDialog.setMessage("Uploading... " + p + "%"));
            }
        });
    }

    private void saveAutoApprovedHelper(String name, String phone, String flat, String purpose) {
        btnSubmit.setEnabled(false);
        String guardUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        long ts = System.currentTimeMillis();

        Map<String, Object> entry = new HashMap<>();
        entry.put("name", name);
        entry.put("phone", phone);
        entry.put("flatNumber", flat);
        entry.put("purpose", purpose);
        entry.put("status", "Approved");
        entry.put("entryType", "DailyHelper");
        entry.put("helperDocId", detectedHelperDocId);
        entry.put("createdBy", guardUid);
        entry.put("timestamp", ts);
        entry.put("entryTime", ts);

        // Save to Firestore
        db.collection("visitors").add(entry).addOnSuccessListener(ref -> {
            db.collection("dailyHelpers").document(detectedHelperDocId).update("lastSeen", ts, "lastSeenBy", guardUid);
            sendNotificationToOwner(flat, "Daily Helper Arrival", name + " (" + purpose + ") is entering for your flat.");
            finish();
        });

        // Trigger Realtime DB for notification
        mRealtimeDb.push().setValue(entry);
    }

    private void saveNormalVisitor(String name, String phone, String flat, String purpose, String imageUrl) {
        btnSubmit.setEnabled(false);
        progressDialog.show();
        String guardUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        long ts = System.currentTimeMillis();

        Map<String, Object> visitor = new HashMap<>();
        visitor.put("name", name);
        visitor.put("phone", phone);
        visitor.put("flatNumber", flat);
        visitor.put("purpose", purpose);
        visitor.put("imageUrl", imageUrl); // Integrated Image URL
        visitor.put("status", isPreApprove ? "Approved" : "Pending");
        visitor.put("timestamp", ts);
        visitor.put("entryTime", ts);
        visitor.put("createdBy", guardUid); // Added guard UID
        visitor.put("entryType", isHelperDetected ? "DailyHelper" : "Visitor");

        // Save to Firestore (Permanent record)
        db.collection("visitors").add(visitor).addOnSuccessListener(ref -> {
            progressDialog.dismiss();
            sendNotificationToOwner(flat, "New Visitor Request", name + " is at the gate for " + purpose);
            finish();
        });

        // Save to Realtime Database (Instant notification trigger for Owner)
        mRealtimeDb.push().setValue(visitor);
    }

    private void sendNotificationToOwner(String flatNo, String title, String body) {
        db.collection("users").whereEqualTo("flatNumber", flatNo).get()
            .addOnSuccessListener(snap -> {
                if (!snap.isEmpty()) {
                    String ownerUid = snap.getDocuments().get(0).getId();
                    DocumentReference notifRef = db.collection("notifications").document();
                    
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("notificationId", notifRef.getId());
                    notif.put("title", title);
                    notif.put("body", body);
                    notif.put("type", "visitor");
                    notif.put("timestamp", Timestamp.now());
                    notif.put("isRead", false);
                    notif.put("targetUid", ownerUid);
                    
                    notifRef.set(notif);
                }
            });
    }
}
