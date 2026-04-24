package com.example.securiodb.ui;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.securiodb.LoginActivity;
import com.example.securiodb.R;
import com.example.securiodb.VolleyMultipartRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvPhone, tvFlat;
    private ImageView ivOwnerAvatar;
    private FloatingActionButton fabEditPhoto;
    private MaterialButton btnChangePassword, btnLogout;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String CLOUD_NAME = "dgvlx2nad";
    private static final String CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";
    private static final String UPLOAD_PRESET = "visitor_upload";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();
        }

        initViews(view);
        fetchProfile();

        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnChangePassword.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
                mAuth.sendPasswordResetEmail(mAuth.getCurrentUser().getEmail())
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show());
            }
        });

        fabEditPhoto.setOnClickListener(v -> openGallery());

        return view;
    }

    private void initViews(View v) {
        tvName = v.findViewById(R.id.tvOwnerNameProfile);
        tvEmail = v.findViewById(R.id.tvEmail);
        tvPhone = v.findViewById(R.id.tvPhone);
        tvFlat = v.findViewById(R.id.chipFlatNumber);
        ivOwnerAvatar = v.findViewById(R.id.ivOwnerAvatar);
        fabEditPhoto = v.findViewById(R.id.fabEditPhoto);
        btnChangePassword = v.findViewById(R.id.btnChangePassword);
        btnLogout = v.findViewById(R.id.btnLogout);
        progressBar = v.findViewById(R.id.progressBar);
    }

    private void fetchProfile() {
        if (uid == null) return;
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                tvName.setText(doc.getString("name"));
                tvEmail.setText(doc.getString("email"));
                tvPhone.setText(doc.getString("phone"));
                tvFlat.setText("Flat " + doc.getString("flatNumber"));

                String profileUrl = doc.getString("profileImageUrl");
                if (profileUrl != null && !profileUrl.isEmpty()) {
                    Glide.with(this).load(profileUrl).placeholder(android.R.drawable.ic_menu_report_image).circleCrop().into(ivOwnerAvatar);
                }
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                // Instantly show the selected image locally for better UX
                ivOwnerAvatar.setImageURI(imageUri);
                
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
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
                        Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
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
        Volley.newRequestQueue(requireContext()).add(multipartRequest);
    }

    private void updateProfileImageInFirestore(String imageUrl) {
        db.collection("users").document(uid)
                .update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (isAdded()) {
                        Glide.with(this).load(imageUrl).circleCrop().into(ivOwnerAvatar);
                        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to update Firestore", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
