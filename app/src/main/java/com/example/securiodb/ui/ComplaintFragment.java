package com.example.securiodb.ui;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.securiodb.R;
import com.example.securiodb.VolleyMultipartRequest;
import com.example.securiodb.adapter.ComplaintAdapter;
import com.example.securiodb.models.ComplaintModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplaintFragment extends Fragment {

    private static final String TAG = "ComplaintFragment";
    private static final int PICK_IMAGE_REQUEST = 101;
    
    // Cloudinary Config (Reusing from OwnerProfileActivity)
    private static final String CLOUD_NAME = "dgvlx2nad";
    private static final String CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";
    private static final String UPLOAD_PRESET  = "visitor_upload";

    private String flatNo;
    private FirebaseFirestore db;
    private RecyclerView recycler;
    private View layoutEmpty;
    private List<ComplaintModel> complaintList = new ArrayList<>();
    private ComplaintAdapter adapter;
    private ListenerRegistration listenerReg;

    // For Dialog
    private ImageView ivSelected;
    private ProgressBar pbUpload;
    private String uploadedImageUrl = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_complaint, container, false);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            flatNo = getArguments().getString("flatNo", "");
        }
        Log.d(TAG, "Fragment started for flat: " + flatNo);

        recycler = view.findViewById(R.id.rvComplaints);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        
        adapter = new ComplaintAdapter(requireContext(), complaintList);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fabAddComplaint);
        if (fab != null) {
            fab.setOnClickListener(v -> showRaiseComplaintDialog());
        }

        loadComplaints();
        return view;
    }

    private void loadComplaints() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        listenerReg = db.collection("complaints")
            .whereEqualTo("ownerUid", uid)
            .orderBy("raisedOn", Query.Direction.DESCENDING)
            .addSnapshotListener((snap, e) -> {
                if (e != null) {
                    Log.e(TAG, "Listen failed: " + e.getMessage());
                    loadComplaintsWithoutOrder(uid);
                    return;
                }
                if (snap == null || !isAdded()) return;
                updateList(snap.getDocuments());
            });
    }

    private void loadComplaintsWithoutOrder(String uid) {
        db.collection("complaints")
            .whereEqualTo("ownerUid", uid)
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                updateList(snap.getDocuments());
            });
    }

    private void updateList(List<DocumentSnapshot> docs) {
        complaintList.clear();
        for (DocumentSnapshot doc : docs) {
            ComplaintModel model = doc.toObject(ComplaintModel.class);
            if (model != null) {
                model.setComplaintId(doc.getId());
                complaintList.add(model);
            }
        }
        adapter.notifyDataSetChanged();
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(complaintList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showRaiseComplaintDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_raise_complaint, null);

        TextInputEditText etTitle = dialogView.findViewById(R.id.etComplaintTitle);
        TextInputEditText etDesc = dialogView.findViewById(R.id.etComplaintDesc);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        ivSelected = dialogView.findViewById(R.id.ivComplaintImage);
        pbUpload = dialogView.findViewById(R.id.pbUpload);
        View frameImage = dialogView.findViewById(R.id.frameComplaintImage);

        uploadedImageUrl = "";

        String[] categories = {"Plumbing", "Electrical", "Security", "Cleaning", "Parking", "Other"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        frameImage.setOnClickListener(v -> openGallery());

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Raise Complaint")
            .setView(dialogView)
            .setPositiveButton("Submit", (d, w) -> {
                String title = etTitle.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();
                String category = spinnerCategory.getSelectedItem().toString();

                if (title.isEmpty() || desc.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveComplaint(title, desc, category, uploadedImageUrl);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                uploadToCloudinary(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadToCloudinary(Bitmap bitmap) {
        if (pbUpload != null) pbUpload.setVisibility(View.VISIBLE);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        final byte[] imageBytes = baos.toByteArray();

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, CLOUDINARY_URL,
            response -> {
                try {
                    String result = new String(response.data);
                    JSONObject jsonResponse = new JSONObject(result);
                    uploadedImageUrl = jsonResponse.getString("secure_url");
                    if (isAdded()) {
                        if (pbUpload != null) pbUpload.setVisibility(View.GONE);
                        Glide.with(this).load(uploadedImageUrl).into(ivSelected);
                        Toast.makeText(requireContext(), "Image uploaded!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    if (pbUpload != null) pbUpload.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                if (pbUpload != null) pbUpload.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
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
                params.put("file", new DataPart("complaint_" + System.currentTimeMillis() + ".jpg", imageBytes, "image/jpeg"));
                return params;
            }
        };

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(60000, 0, 1f));
        Volley.newRequestQueue(requireContext()).add(multipartRequest);
    }

    private void saveComplaint(String title, String desc, String category, String imageUrl) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String ownerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> complaint = new HashMap<>();
        complaint.put("flatNo", flatNo != null ? flatNo : "");
        complaint.put("ownerUid", ownerUid);
        complaint.put("title", title);
        complaint.put("description", desc);
        complaint.put("category", category);
        complaint.put("status", "Open");
        complaint.put("imageUrl", imageUrl);
        complaint.put("raisedOn", System.currentTimeMillis());

        db.collection("complaints").add(complaint)
            .addOnSuccessListener(ref -> {
                if (isAdded()) Toast.makeText(requireContext(), "Complaint registered!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                if (isAdded()) Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerReg != null) listenerReg.remove();
    }
}
