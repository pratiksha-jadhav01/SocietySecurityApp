package com.example.securiodb;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.Volley;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class AddDeliveryActivity extends AppCompatActivity {

    private TextInputEditText etName, etOtherCompany, etFlat, etPackage;
    private TextInputLayout tilOtherCompany;
    private ChipGroup chipGroupCompany;
    private ProgressBar progressBar;
    private ImageView imgDeliveryPhoto;
    private View btnCapturePhoto;
    
    private Bitmap capturedPhotoBitmap = null;
    private boolean isPhotoCaptured = false;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_CAPTURE_CODE    = 101;

    // Cloudinary Config (Same as VisitorEntryActivity)
    private static final String CLOUD_NAME = "dgvlx2nad";
    private static final String CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";
    private static final String UPLOAD_PRESET  = "visitor_upload";

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
        imgDeliveryPhoto = findViewById(R.id.imgDeliveryPhoto);
        btnCapturePhoto = findViewById(R.id.btnCapturePhoto);

        chipGroupCompany.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipOtherCompany)) {
                tilOtherCompany.setVisibility(View.VISIBLE);
            } else {
                tilOtherCompany.setVisibility(View.GONE);
            }
        });

        btnCapturePhoto.setOnClickListener(v -> checkCameraPermission());
        findViewById(R.id.btnSubmit).setOnClickListener(v -> validateAndSubmit());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_CAPTURE_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_CAPTURE_CODE && resultCode == RESULT_OK && data != null) {
            capturedPhotoBitmap = (Bitmap) data.getExtras().get("data");
            if (capturedPhotoBitmap != null) {
                imgDeliveryPhoto.setImageBitmap(capturedPhotoBitmap);
                isPhotoCaptured = true;
            }
        }
    }

    private void validateAndSubmit() {
        if (!isPhotoCaptured || capturedPhotoBitmap == null) {
            Toast.makeText(this, "Capture photo first", Toast.LENGTH_SHORT).show();
            return;
        }
        
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

        uploadToCloudinary(name, company, flat, packageType);
    }

    private void uploadToCloudinary(String name, String company, String flat, String packageType) {
        progressBar.setVisibility(View.VISIBLE);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        capturedPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        final byte[] imageBytes = baos.toByteArray();

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, CLOUDINARY_URL,
            response -> {
                try {
                    String result = new String(response.data);
                    JSONObject jsonResponse = new JSONObject(result);
                    String imageUrl = jsonResponse.getString("secure_url");
                    saveDelivery(name, company, flat, packageType, imageUrl);
                } catch (Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "JSON Error", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show();
            }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("upload_preset", UPLOAD_PRESET);
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                params.put("file", new DataPart("delivery.jpg", imageBytes, "image/jpeg"));
                return params;
            }
        };

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(60000, 0, 1f));
        Volley.newRequestQueue(this).add(multipartRequest);
    }

    private void saveDelivery(String name, String company, String flat, String packageType, String imageUrl) {
        Map<String, Object> delivery = new HashMap<>();
        delivery.put("name", name);
        delivery.put("company", company);
        delivery.put("flatNumber", flat);
        delivery.put("packageType", packageType);
        delivery.put("imageUrl", imageUrl);
        delivery.put("purpose", "Delivery");
        delivery.put("status", "Pending");
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
