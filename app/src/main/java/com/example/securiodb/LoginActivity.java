package com.example.securiodb;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseFirestore mFirestore;

    private MaterialCardView cardGuard, cardOwner, cardAdmin;
    private ImageView ivGuard, ivOwner, ivAdmin;
    private TextView tvGuard, tvOwner, tvAdmin;
    private String selectedRole = "guard"; // Default role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirestore = FirebaseFirestore.getInstance();

        // Bind Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);

        cardGuard = findViewById(R.id.cardRoleGuard);
        cardOwner = findViewById(R.id.cardRoleOwner);
        cardAdmin = findViewById(R.id.cardRoleAdmin);

        ivGuard = findViewById(R.id.ivGuard);
        ivOwner = findViewById(R.id.ivOwner);
        ivAdmin = findViewById(R.id.ivAdmin);

        tvGuard = findViewById(R.id.tvGuard);
        tvOwner = findViewById(R.id.tvOwner);
        tvAdmin = findViewById(R.id.tvAdmin);

        // Role selection listeners
        cardGuard.setOnClickListener(v -> selectRole("guard"));
        cardOwner.setOnClickListener(v -> selectRole("owner"));
        cardAdmin.setOnClickListener(v -> selectRole("admin"));

        // Initialize default selection
        selectRole("guard");

        btnLogin.setOnClickListener(v -> loginUser(selectedRole));

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void selectRole(String role) {
        selectedRole = role;

        // Reset all to unselected
        resetRoleUI(cardGuard, ivGuard, tvGuard);
        resetRoleUI(cardOwner, ivOwner, tvOwner);
        resetRoleUI(cardAdmin, ivAdmin, tvAdmin);

        // Apply selected style
        if ("guard".equals(role)) {
            setSelectedUI(cardGuard, ivGuard, tvGuard);
        } else if ("owner".equals(role)) {
            setSelectedUI(cardOwner, ivOwner, tvOwner);
        } else if ("admin".equals(role)) {
            setSelectedUI(cardAdmin, ivAdmin, tvAdmin);
        }
    }

    private void resetRoleUI(MaterialCardView card, ImageView iv, TextView tv) {
        card.setCardBackgroundColor(Color.WHITE);
        int softBrown = ContextCompat.getColor(this, R.color.soft_brown);
        card.setStrokeColor(ColorStateList.valueOf(softBrown));
        iv.setImageTintList(ColorStateList.valueOf(softBrown));
        tv.setTextColor(softBrown);
    }

    private void setSelectedUI(MaterialCardView card, ImageView iv, TextView tv) {
        int coffeeBrown = ContextCompat.getColor(this, R.color.coffee_brown);
        card.setCardBackgroundColor(ColorStateList.valueOf(coffeeBrown));
        card.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
        iv.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        tv.setTextColor(Color.WHITE);
    }

    private void loginUser(String expectedRole) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return;
        }

        showProgress(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkUserRoleAndRedirect(mAuth.getCurrentUser().getUid(), expectedRole);
                    } else {
                        showProgress(false);
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRoleAndRedirect(String uid, String expectedRole) {
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    
                    if (expectedRole != null && !expectedRole.equalsIgnoreCase(role)) {
                        showProgress(false);
                        Toast.makeText(LoginActivity.this, "Unauthorized access for " + expectedRole, Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        return;
                    }

                    // Force sync to Firestore to ensure permissions are correct
                    syncUserToFirestore(uid, snapshot, role != null ? role.toLowerCase() : expectedRole);
                } else {
                    showProgress(false);
                    mAuth.signOut();
                    Toast.makeText(LoginActivity.this, "User record not found in Database", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showProgress(false);
                Toast.makeText(LoginActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncUserToFirestore(String uid, DataSnapshot rtdbSnapshot, String role) {
        // Prepare user data from RTDB snapshot
        Map<String, Object> userData = new HashMap<>();
        for (DataSnapshot ds : rtdbSnapshot.getChildren()) {
            String key = ds.getKey();
            Object value = ds.getValue();
            if ("role".equals(key) && value instanceof String) {
                userData.put(key, ((String) value).toLowerCase());
            } else {
                userData.put(key, value);
            }
        }
        
        // Ensure role is definitely there and lowercase
        userData.put("role", role.toLowerCase());
        
        // Add/Update Firestore document
        mFirestore.collection("users").document(uid).set(userData)
                .addOnCompleteListener(task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        navigateToDashboard(role.toLowerCase());
                    } else {
                        Toast.makeText(LoginActivity.this, "Sync Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        // Still try to navigate if we have the role
                        navigateToDashboard(role.toLowerCase());
                    }
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("admin".equalsIgnoreCase(role)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if ("guard".equalsIgnoreCase(role)) {
            intent = new Intent(this, GuardDashboardActivity.class);
        } else {
            intent = new Intent(this, OwnerDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
    }
}
