package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail, etPhone, etFlatNumber, etPassword, etConfirmPassword, etAdminKey;
    private TextInputLayout tilAdminKey;
    private ChipGroup chipGroupRole;
    private Chip chipGuard, chipOwner, chipAdmin;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private ViewGroup mainContainer;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseFirestore mFirestore;

    private static final String ADMIN_SECRET = "APT@Admin2024";
    private static final String GUARD_SECRET = "APT@Guard2024";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirestore = FirebaseFirestore.getInstance();

        // Bind Views
        mainContainer = findViewById(android.R.id.content);
        btnBack = findViewById(R.id.btnBack);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etFlatNumber = findViewById(R.id.etFlatNumber);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAdminKey = findViewById(R.id.etAdminKey);
        tilAdminKey = findViewById(R.id.tilAdminKey);
        chipGroupRole = findViewById(R.id.chipGroupRole);
        chipGuard = findViewById(R.id.chipGuard);
        chipOwner = findViewById(R.id.chipOwner);
        chipAdmin = findViewById(R.id.chipAdmin);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        progressBar = findViewById(R.id.progressBar);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Setup Role Selection
        setupRoleChips();

        btnRegister.setOnClickListener(v -> registerUser());

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setupRoleChips() {
        chipAdmin.setOnCheckedChangeListener((buttonView, isChecked) -> updateSecretKeyVisibility());
        chipGuard.setOnCheckedChangeListener((buttonView, isChecked) -> updateSecretKeyVisibility());
        chipOwner.setOnCheckedChangeListener((buttonView, isChecked) -> updateSecretKeyVisibility());
    }

    private void updateSecretKeyVisibility() {
        TransitionManager.beginDelayedTransition(mainContainer);
        if (chipAdmin.isChecked()) {
            tilAdminKey.setVisibility(View.VISIBLE);
            tilAdminKey.setHint("Admin Secret Key");
        } else if (chipGuard.isChecked()) {
            tilAdminKey.setVisibility(View.VISIBLE);
            tilAdminKey.setHint("Guard Secret Key");
        } else {
            tilAdminKey.setVisibility(View.GONE);
        }
    }

    private void registerUser() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String flat = etFlatNumber.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        int checkedChipId = chipGroupRole.getCheckedChipId();
        if (checkedChipId == View.NO_ID) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String selectedRole = "";
        if (checkedChipId == R.id.chipGuard) selectedRole = "Guard";
        else if (checkedChipId == R.id.chipOwner) selectedRole = "Owner";
        else if (checkedChipId == R.id.chipAdmin) selectedRole = "Admin";

        // Validation
        if (TextUtils.isEmpty(name)) { etFullName.setError("Required"); return; }
        if (TextUtils.isEmpty(email)) { etEmail.setError("Required"); return; }
        if (TextUtils.isEmpty(phone)) { etPhone.setError("Required"); return; }
        if (TextUtils.isEmpty(flat)) { etFlatNumber.setError("Required"); return; }
        if (password.length() < 6) { etPassword.setError("Min 6 characters"); return; }
        if (!password.equals(confirmPassword)) { etConfirmPassword.setError("Passwords don't match"); return; }

        // Secret Key Validation
        if ("Admin".equals(selectedRole)) {
            String enteredKey = etAdminKey.getText().toString().trim();
            if (!ADMIN_SECRET.equals(enteredKey)) {
                etAdminKey.setError("Invalid admin key");
                return;
            }
        } else if ("Guard".equals(selectedRole)) {
            String enteredKey = etAdminKey.getText().toString().trim();
            if (!GUARD_SECRET.equals(enteredKey)) {
                etAdminKey.setError("Invalid guard key");
                return;
            }
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // 1. Create User in Firebase Auth
        final String finalRole = selectedRole;
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToDatabase(uid, name, email, phone, finalRole, flat);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid, String name, String email, String phone, String role, String flat) {
        // 2. Prepare user data
        Map<String, Object> user = new HashMap<>();
        user.put("userId", uid);
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("role", role.toLowerCase());
        user.put("flatNumber", flat);
        user.put("createdAt", ServerValue.TIMESTAMP);

        // Save to Realtime Database
        mDatabase.child("users").child(uid).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    // Also save to Firestore for consistency across the app
                    Map<String, Object> firestoreUser = new HashMap<>(user);
                    firestoreUser.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    
                    mFirestore.collection("users").document(uid).set(firestoreUser)
                            .addOnSuccessListener(v -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                navigateToDashboard(role.toLowerCase());
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("admin".equals(role)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if ("guard".equals(role)) {
            intent = new Intent(this, GuardDashboardActivity.class);
        } else {
            intent = new Intent(this, OwnerDashboardActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
