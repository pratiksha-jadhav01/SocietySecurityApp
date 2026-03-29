package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OwnerProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvFlat;
    private MaterialButton btnChangePassword, btnLogout;
    private BottomNavigationView bottomNav;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        fetchProfile();
        setupBottomNav();

        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnChangePassword.setOnClickListener(v -> {
            // Placeholder for password reset email or dialog
            mAuth.sendPasswordResetEmail(mAuth.getCurrentUser().getEmail())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show());
        });
    }

    private void initViews() {
        tvName = findViewById(R.id.tvOwnerNameProfile);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvFlat = findViewById(R.id.chipFlatNumber);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNav = findViewById(R.id.bottomNav);
    }

    private void fetchProfile() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvName.setText(doc.getString("name"));
                tvEmail.setText(doc.getString("email"));
                tvPhone.setText(doc.getString("phone"));
                tvFlat.setText("Flat " + doc.getString("flatNumber"));
            }
        });
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, OwnerDashboardActivity.class));
                finish();
            } else if (id == R.id.nav_requests) {
                startActivity(new Intent(this, ApprovalsActivity.class));
                finish();
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, VisitorHistoryActivity.class));
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
                    startActivity(new Intent(this, LoginActivity.class));
                    finishAffinity();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
