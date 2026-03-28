package com.example.securiodb;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.securiodb.model.Visitor;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class VisitorEntryActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etFlat;
    private Spinner spinnerPurpose;
    private ImageView ivPhoto;
    private Button btnCapture, btnSubmit;
    private ProgressBar progressBar;

    private Bitmap imageBitmap;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_entry);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind Views
        etName = findViewById(R.id.etVisitorName);
        etPhone = findViewById(R.id.etPhone);
        etFlat = findViewById(R.id.etFlatNumber);
        spinnerPurpose = findViewById(R.id.spinnerPurpose);
        ivPhoto = findViewById(R.id.ivVisitorPhoto);
        btnCapture = findViewById(R.id.btnCapturePhoto);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        // Handle Intent Extras for Purpose
        String typeExtra = getIntent().getStringExtra("type");
        if (typeExtra != null) {
            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerPurpose.getAdapter();
            if (adapter != null) {
                int spinnerPosition = adapter.getPosition(typeExtra);
                spinnerPurpose.setSelection(spinnerPosition);
            }
        }

        btnCapture.setOnClickListener(v -> capturePhoto());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    // Camera Result Launcher
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    imageBitmap = (Bitmap) extras.get("data");
                    ivPhoto.setImageBitmap(imageBitmap);
                }
            }
    );

    private void capturePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateAndSubmit() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String flat = etFlat.getText().toString().trim();
        String purpose = spinnerPurpose.getSelectedItem().toString();

        if (name.isEmpty() || phone.isEmpty() || flat.isEmpty()) {
            Toast.makeText(this, "Please fill all text fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageBitmap == null) {
            Toast.makeText(this, "Please capture a visitor photo", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadImageAndSaveData(name, phone, flat, purpose);
    }

    private void uploadImageAndSaveData(String name, String phone, String flat, String purpose) {
        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        // 1. Upload Photo to Firebase Storage
        String fileName = "visitors/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] data = baos.toByteArray();

        ref.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    // 2. Once uploaded, save data to Firestore
                    saveToFirestore(name, phone, flat, purpose, uri.toString());
                }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Photo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToFirestore(String name, String phone, String flat, String purpose, String url) {
        String visitorId = db.collection("visitors").document().getId();
        
        // Using Map for flexibility or the POJO
        Visitor visitor = new Visitor(
                visitorId, name, phone, flat, purpose, url,
                "Pending", mAuth.getUid(), Timestamp.now()
        );

        db.collection("visitors").document(visitorId).set(visitor)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Visitor entry created successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Return to dashboard
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
