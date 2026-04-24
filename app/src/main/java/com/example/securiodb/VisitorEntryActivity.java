package com.example.securiodb;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class VisitorEntryActivity extends AppCompatActivity {

    private ImageView imgVisitorPhoto;
    private MaterialButton btnCapturePhoto, btnSubmit;
    private TextInputLayout tilName, tilPhone, tilFlatNo, tilPurpose;
    private TextInputEditText etName, etPhone, etFlatNo, etPurpose;

    private Bitmap capturedPhotoBitmap = null;
    private boolean isPhotoCaptured = false;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_CAPTURE_CODE    = 101;

    // Correct Cloudinary Config
    private static final String CLOUD_NAME = "dgvlx2nad";
    private static final String CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";
    private static final String UPLOAD_PRESET  = "visitor_upload"; 

    private FirebaseFirestore db;
    private String guardUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_entry);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            guardUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        btnCapturePhoto.setOnClickListener(v -> checkCameraPermission());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
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
                imgVisitorPhoto.setImageBitmap(capturedPhotoBitmap);
                imgVisitorPhoto.setImageTintList(null); // Clear XML tint to make photo visible
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
        String phone = etPhone.getText().toString().trim();
        String flat = etFlatNo.getText().toString().trim().toUpperCase();
        String purpose = etPurpose.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || flat.isEmpty() || purpose.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadToCloudinary(name, phone, flat, purpose);
    }

    private void uploadToCloudinary(String name, String phone, String flat, String purpose) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Uploading...");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        capturedPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        final byte[] imageBytes = baos.toByteArray();

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, CLOUDINARY_URL,
            response -> {
                try {
                    String result = new String(response.data);
                    JSONObject jsonResponse = new JSONObject(result);
                    String imageUrl = jsonResponse.getString("secure_url");
                    saveToFirestore(name, phone, flat, purpose, imageUrl);
                } catch (Exception e) {
                    resetSubmitButton("JSON Error");
                }
            },
            error -> {
                String msg = "Upload Failed";
                if (error.networkResponse != null) {
                    msg += " (Status: " + error.networkResponse.statusCode + ")";
                    Log.e("Cloudinary", "Body: " + new String(error.networkResponse.data));
                }
                resetSubmitButton(msg);
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
                params.put("file", new DataPart("visitor.jpg", imageBytes, "image/jpeg"));
                return params;
            }
        };

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(60000, 0, 1f));
        Volley.newRequestQueue(this).add(multipartRequest);
    }

    private void resetSubmitButton(String msg) {
        btnSubmit.setEnabled(true);
        btnSubmit.setText("SUBMIT ENTRY");
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void saveToFirestore(String name, String phone, String flat, String purpose, String imageUrl) {
        btnSubmit.setText("Saving...");
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("phone", phone);
        data.put("flatNumber", flat);
        data.put("purpose", purpose);
        data.put("entryType", "Visitor"); // Added for consistent filtering
        data.put("imageUrl", imageUrl);
        data.put("status", "Pending");
        data.put("createdBy", guardUid);
        data.put("timestamp", FieldValue.serverTimestamp());
        data.put("entryTime", FieldValue.serverTimestamp());

        db.collection("visitors").add(data)
            .addOnSuccessListener(v -> {
                Toast.makeText(this, "Entry Created!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> resetSubmitButton("Firestore Error"));
    }
}
