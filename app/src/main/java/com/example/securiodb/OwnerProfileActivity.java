package com.example.securiodb;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OwnerProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvFlat;
    private TextView tvStatApproved, tvStatPending, tvStatTotal;
    private ImageView ivOwnerAvatar;
    private FloatingActionButton fabEditPhoto;
    private View btnChangePassword, btnLogout;
    private BottomNavigationView bottomNav;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String CLOUD_NAME = "dgvlx2nad";
    private static final String CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";
    private static final String UPLOAD_PRESET  = "visitor_upload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        uid = mAuth.getCurrentUser().getUid();

        initViews();
        fetchProfile();
        setupBottomNav();

        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnChangePassword.setOnClickListener(v -> {
            mAuth.sendPasswordResetEmail(mAuth.getCurrentUser().getEmail())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        fabEditPhoto.setOnClickListener(v -> openGallery());
    }

    private void initViews() {
        tvName = findViewById(R.id.tvOwnerNameProfile);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvFlat = findViewById(R.id.chipFlatNumber);
        
        tvStatApproved = findViewById(R.id.tvStatApproved);
        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatTotal = findViewById(R.id.tvStatTotal);

        ivOwnerAvatar = findViewById(R.id.ivOwnerAvatar);
        fabEditPhoto = findViewById(R.id.fabEditPhoto);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNav = findViewById(R.id.bottomNav);
        progressBar = findViewById(R.id.progressBar);
    }

    private void fetchProfile() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String phone = doc.getString("phone");
                String flatNumber = doc.getString("flatNumber");
                
                tvName.setText(name);
                tvEmail.setText(email);
                tvPhone.setText(phone);
                tvFlat.setText("Flat " + flatNumber);
                
                String profileUrl = doc.getString("profileImageUrl");
                if (profileUrl != null && !profileUrl.isEmpty()) {
                    Glide.with(this).load(profileUrl).placeholder(android.R.drawable.ic_menu_report_image).into(ivOwnerAvatar);
                }

                if (flatNumber != null) {
                    fetchStats(flatNumber);
                }
            }
        });
    }

    private void fetchStats(String flatNumber) {
        // Total history
        db.collection("visitors").whereEqualTo("flatNumber", flatNumber).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvStatTotal.setText(String.valueOf(queryDocumentSnapshots.size()));
                });

        // Approved
        db.collection("visitors").whereEqualTo("flatNumber", flatNumber).whereEqualTo("status", "Approved").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvStatApproved.setText(String.valueOf(queryDocumentSnapshots.size()));
                });

        // Pending
        db.collection("visitors").whereEqualTo("flatNumber", flatNumber).whereEqualTo("status", "Pending").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvStatPending.setText(String.valueOf(queryDocumentSnapshots.size()));
                });
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                uploadToCloudinary(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadToCloudinary(Bitmap bitmap) {
        progressBar.setVisibility(View.VISIBLE);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        final byte[] imageBytes = baos.toByteArray();

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, CLOUDINARY_URL,
            response -> {
                try {
                    String result = new String(response.data);
                    JSONObject jsonResponse = new JSONObject(result);
                    String imageUrl = jsonResponse.getString("secure_url");
                    updateProfileImageInFirestore(imageUrl);
                } catch (Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
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
                params.put("file", new DataPart("profile_" + uid + ".jpg", imageBytes, "image/jpeg"));
                return params;
            }
        };

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(60000, 0, 1f));
        Volley.newRequestQueue(this).add(multipartRequest);
    }

    private void updateProfileImageInFirestore(String imageUrl) {
        db.collection("users").document(uid)
            .update("profileImageUrl", imageUrl)
            .addOnSuccessListener(aVoid -> {
                progressBar.setVisibility(View.GONE);
                Glide.with(this).load(imageUrl).into(ivOwnerAvatar);
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to update Firestore", Toast.LENGTH_SHORT).show();
            });
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.navProfile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navHome) {
                startActivity(new Intent(this, OwnerDashboardActivity.class));
                finish();
            } else if (id == R.id.navRequests) {
                // Assuming ViewVisitorsActivity is for requests
                startActivity(new Intent(this, ViewVisitorsActivity.class));
                finish();
            } else if (id == R.id.navHistory) {
                startActivity(new Intent(this, VisitorHistoryActivity.class));
                finish();
            } else if (id == R.id.navHelpers) {
                // Return to dashboard and select Helpers
                Intent intent = new Intent(this, OwnerDashboardActivity.class);
                intent.putExtra("TARGET_NAV", R.id.navHelpers);
                startActivity(intent);
                finish();
            }
            return true;
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
