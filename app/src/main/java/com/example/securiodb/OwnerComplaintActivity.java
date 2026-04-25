package com.example.securiodb;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securiodb.adapter.ComplaintAdapter;
import com.example.securiodb.models.ComplaintModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for owners to manage their complaints with real-time updates from Firestore.
 */
public class OwnerComplaintActivity extends AppCompatActivity {

    private static final String TAG = "OwnerComplaintActivity";
    private static final String CHANNEL_ID = "complaint_channel";
    private static final String PREF_NAME  = "ComplaintNotifPrefs";
    private static final int PICK_IMAGE_REQUEST = 101;

    private RecyclerView recyclerComplaints;
    private TextView tvEmpty;
    private List<ComplaintModel> complaintList = new ArrayList<>();
    private ComplaintAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private String flatNo = "";
    private SharedPreferences prefs;
    private ListenerRegistration complaintsListener;

    // For Image Upload in Dialog
    private Uri imageUri;
    private ImageView ivDialogImage;
    private ProgressBar pbDialogUpload;
    private String uploadedImageUrl = "";
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_complaint);

        // Authentication check
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        // Initialize Firebase and Preferences
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Fetch User Info (Flat Number)
        fetchUserInfo();

        // Bind Views
        recyclerComplaints = findViewById(R.id.recyclerComplaints);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Setup RecyclerView
        adapter = new ComplaintAdapter(this, complaintList);
        recyclerComplaints.setLayoutManager(new LinearLayoutManager(this));
        recyclerComplaints.setAdapter(adapter);

        // Setup Toolbar
        if (findViewById(R.id.toolbar) != null) {
            findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        }

        // Raise Complaint Button
        findViewById(R.id.btnRaiseComplaint).setOnClickListener(v -> showRaiseComplaintDialog());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting...");
        progressDialog.setCancelable(false);

        // Notification Channel setup
        createNotificationChannel();

        // Start listening to complaints
        listenToComplaints();
    }

    private void fetchUserInfo() {
        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                flatNo = doc.getString("flatNumber");
                if (flatNo == null) flatNo = doc.getString("flatNo");
                if (flatNo == null) flatNo = "Unknown";
            }
        });
    }

    /**
     * Listens to the 'complaints' collection in Firestore.
     * Filters complaints for the current user and triggers notifications when status changes.
     */
    private void listenToComplaints() {
        complaintsListener = db.collection("complaints")
                .whereEqualTo("ownerUid", currentUserId)
                .orderBy("raisedOn", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed", e);
                        // Fallback check
                        tryAlternativeListen();
                        return;
                    }

                    if (snapshots != null) {
                        processSnapshots(snapshots.getDocuments());
                    }
                });
    }

    private void tryAlternativeListen() {
        db.collection("complaints")
                .whereEqualTo("ownerUid", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Alternative listen failed", e);
                        return;
                    }
                    if (snapshots != null) {
                        processSnapshots(snapshots.getDocuments());
                    }
                });
    }

    private void processSnapshots(List<DocumentSnapshot> documents) {
        complaintList.clear();
        for (DocumentSnapshot doc : documents) {
            ComplaintModel m = doc.toObject(ComplaintModel.class);
            if (m != null) {
                m.setComplaintId(doc.getId());
                complaintList.add(m);

                String status = m.getStatus();
                String response = m.getAdminResponse();
                
                String statusKey = "status_" + doc.getId() + "_" + status;
                if (!"Pending".equalsIgnoreCase(status) && !prefs.getBoolean(statusKey, false)) {
                    showNotification("Complaint Update: " + status, 
                        (response != null && !response.isEmpty()) 
                        ? "Response: " + response 
                        : "Your complaint '" + m.getTitle() + "' is now " + status + ".");
                    prefs.edit().putBoolean(statusKey, true).apply();
                }
            }
        }
        updateUI(complaintList.isEmpty());
    }

    private void updateUI(boolean isEmpty) {
        if (isEmpty) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerComplaints.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerComplaints.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void showRaiseComplaintDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_raise_complaint, null);
        TextInputEditText etTitle = view.findViewById(R.id.etComplaintTitle);
        TextInputEditText etDesc = view.findViewById(R.id.etComplaintDesc);
        Spinner spinnerCategory = view.findViewById(R.id.spinnerCategory);
        ivDialogImage = view.findViewById(R.id.ivComplaintImage);
        pbDialogUpload = view.findViewById(R.id.pbUpload);
        View frameImage = view.findViewById(R.id.frameComplaintImage);

        uploadedImageUrl = "";
        imageUri = null;

        String[] categories = {"Plumbing", "Electrical", "Security", "Cleaning", "Parking", "Other"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        frameImage.setOnClickListener(v -> openImagePicker());

        new MaterialAlertDialogBuilder(this)
            .setTitle("Raise Complaint")
            .setView(view)
            .setPositiveButton("Submit", (dialog, which) -> {
                String title = etTitle.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();
                String category = spinnerCategory.getSelectedItem().toString();

                if (title.isEmpty() || desc.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveComplaint(title, desc, category, uploadedImageUrl);
            })
            .setNegativeButton("Cancel", null)
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
                Toast.makeText(OwnerComplaintActivity.this, "Image uploaded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (pbDialogUpload != null) pbDialogUpload.setVisibility(View.GONE);
                Toast.makeText(OwnerComplaintActivity.this, "Upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(int percent) {}
        });
    }

    private void saveComplaint(String title, String desc, String category, String imageUrl) {
        progressDialog.show();
        Map<String, Object> data = new HashMap<>();
        data.put("ownerUid",    currentUserId);
        data.put("userId",      currentUserId);
        data.put("flatNo",      flatNo);
        data.put("title",       title);
        data.put("description", desc);
        data.put("category",    category);
        data.put("status",      "Pending");
        data.put("imageUrl",    imageUrl);
        data.put("adminResponse", "");
        data.put("response",    "");
        data.put("raisedOn",    System.currentTimeMillis());
        data.put("timestamp",   System.currentTimeMillis());

        db.collection("complaints").add(data)
            .addOnSuccessListener(v -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Complaint raised successfully!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.secu_ground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Complaint Updates", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for complaint status updates");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (complaintsListener != null) {
            complaintsListener.remove();
        }
    }
}
