package com.example.securiodb;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AdminProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private ImageView ivAdminAvatar;
    private FloatingActionButton fabEditPhoto;
    private View btnChangePassword, btnAppSettings, btnPrivacy, btnLogout;
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
        setContentView(R.layout.activity_admin_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();
        }

        initViews();
        loadAdminData();
        setupBottomNav();
        setupClickListeners();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvAdminName);
        tvEmail = findViewById(R.id.tvAdminEmail);
        ivAdminAvatar = findViewById(R.id.ivAdminAvatar);
        fabEditPhoto = findViewById(R.id.fabEditPhoto);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnAppSettings = findViewById(R.id.btnAppSettings);
        btnPrivacy = findViewById(R.id.btnPrivacy);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNav = findViewById(R.id.adminBottomNav);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadAdminData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvEmail.setText(user.getEmail());
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            tvName.setText(doc.getString("name"));
                            String profileUrl = doc.getString("profileImageUrl");
                            if (profileUrl != null && !profileUrl.isEmpty()) {
                                Glide.with(this).load(profileUrl).placeholder(android.R.drawable.ic_menu_gallery).into(ivAdminAvatar);
                            }
                        }
                    });
        }
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_visitors) {
                startActivity(new Intent(this, VisitorLogsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(this, ManageUsersActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    private void setupClickListeners() {
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnAppSettings.setOnClickListener(v -> Toast.makeText(this, "App Settings coming soon", Toast.LENGTH_SHORT).show());
        btnPrivacy.setOnClickListener(v -> Toast.makeText(this, "Privacy Policy loaded", Toast.LENGTH_SHORT).show());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        fabEditPhoto.setOnClickListener(v -> openGallery());
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
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
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
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
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
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Glide.with(this).load(imageUrl).into(ivAdminAvatar);
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to update Firestore", Toast.LENGTH_SHORT).show();
            });
    }

    private void showChangePasswordDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        TextInputEditText etNewPassword = view.findViewById(R.id.etNewPassword);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(view)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass = etNewPassword.getText().toString().trim();
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mAuth.getCurrentUser().updatePassword(newPass)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to exit?")
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
