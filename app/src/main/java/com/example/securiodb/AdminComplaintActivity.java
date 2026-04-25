package com.example.securiodb;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securiodb.adapter.AdminComplaintAdapter;
import com.example.securiodb.models.ComplaintModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminComplaintActivity extends AppCompatActivity {

    private static final String TAG = "AdminComplaintActivity";
    private static final int PICK_IMAGE_REQUEST = 101;
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private List<ComplaintModel> complaintList = new ArrayList<>();
    private AdminComplaintAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;

    // For Image Upload in Dialog
    private Uri imageUri;
    private ImageView ivDialogImage;
    private ProgressBar pbDialogUpload;
    private String uploadedImageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_complaint);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerAdminComplaints);
        progressBar = findViewById(R.id.progressBar); // Re-binding to the correct ID from activity_admin_complaint.xml if missing it might crash
        
        // Let's double check the layout if it has progressBar
        tvEmpty = findViewById(R.id.tvEmpty);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        adapter = new AdminComplaintAdapter(this, complaintList, (id, pos) -> showUpdateStatusDialog(complaintList.get(pos), pos));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadAllComplaints();
    }

    private void loadAllComplaints() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        listenerReg = db.collection("complaints")
                .orderBy("raisedOn", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Log.e(TAG, "Listen failed", error);
                        return;
                    }

                    complaintList.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ComplaintModel model = doc.toObject(ComplaintModel.class);
                            if (model != null) {
                                model.setComplaintId(doc.getId());
                                complaintList.add(model);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (tvEmpty != null) {
                        tvEmpty.setVisibility(complaintList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void showUpdateStatusDialog(ComplaintModel complaint, int position) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_raise_complaint, null);
        TextInputEditText etResponse = view.findViewById(R.id.etComplaintDesc);
        Spinner spinnerStatus = view.findViewById(R.id.spinnerCategory);
        ivDialogImage = view.findViewById(R.id.ivComplaintImage);
        pbDialogUpload = view.findViewById(R.id.pbUpload);
        View frameImage = view.findViewById(R.id.frameComplaintImage);
        
        view.findViewById(R.id.etComplaintTitle).setVisibility(View.GONE);
        
        etResponse.setHint("Admin Response / Resolution Details");
        etResponse.setText(complaint.getAdminResponse());

        String[] statuses = {"Open", "In Progress", "Resolved"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        // Set current status
        String currentStatus = complaint.getStatus();
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(currentStatus)) {
                spinnerStatus.setSelection(i);
                break;
            }
        }

        // Setup Image Picker
        imageUri = null;
        uploadedImageUrl = complaint.getResolutionImageUrl() != null ? complaint.getResolutionImageUrl() : "";
        if (!uploadedImageUrl.isEmpty()) {
            Glide.with(this).load(uploadedImageUrl).into(ivDialogImage);
            ivDialogImage.setImageTintList(null);
        }

        frameImage.setOnClickListener(v -> openImagePicker());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Complaint Status")
                .setView(view)
                .setPositiveButton("Update", (dialog, which) -> {
                    String response = etResponse.getText().toString().trim();
                    String status = spinnerStatus.getSelectedItem().toString();
                    updateComplaint(complaint.getComplaintId(), status, response, uploadedImageUrl);
                })
                .setNegativeButton("Cancel", (dialog, which) -> adapter.notifyItemChanged(position))
                .show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            if (ivDialogImage != null) {
                ivDialogImage.setImageURI(imageUri);
                ivDialogImage.setImageTintList(null);
            }
            uploadImageToCloudinary();
        }
    }

    private void uploadImageToCloudinary() {
        if (imageUri == null) return;

        if (pbDialogUpload != null) pbDialogUpload.setVisibility(View.VISIBLE);
        
        CloudinaryUploader.uploadImage(this, imageUri, new CloudinaryUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                uploadedImageUrl = imageUrl;
                if (pbDialogUpload != null) pbDialogUpload.setVisibility(View.GONE);
                Toast.makeText(AdminComplaintActivity.this, "Image uploaded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (pbDialogUpload != null) pbDialogUpload.setVisibility(View.GONE);
                Toast.makeText(AdminComplaintActivity.this, "Upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(int percent) {
                // Could update progress bar
            }
        });
    }

    private void updateComplaint(String docId, String status, String response, String imageUrl) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            return;
        }

        if (docId == null || docId.isEmpty()) {
            Toast.makeText(this, "Complaint ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("adminResponse", response);
        updates.put("response", response);
        updates.put("resolutionImageUrl", imageUrl);

        db.collection("complaints").document(docId).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Complaint updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    String msg = e.getMessage();
                    if (e instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        msg = "Permission denied - check Admin role in Firestore users collection";
                    }
                    Toast.makeText(this, "Failed: " + msg, Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerReg != null) listenerReg.remove();
    }
}
