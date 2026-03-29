package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private View btnChangePassword, btnAppSettings, btnPrivacy, btnLogout;
    private BottomNavigationView bottomNav;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadAdminData();
        setupBottomNav();
        setupClickListeners();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvAdminName);
        tvEmail = findViewById(R.id.tvAdminEmail);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnAppSettings = findViewById(R.id.btnAppSettings);
        btnPrivacy = findViewById(R.id.btnPrivacy);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNav = findViewById(R.id.adminBottomNav);
    }

    private void loadAdminData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvEmail.setText(user.getEmail());
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            tvName.setText(documentSnapshot.getString("name"));
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
                return true;
            } else if (id == R.id.nav_visitors) {
                startActivity(new Intent(this, VisitorLogsActivity.class));
                return true;
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(this, ManageUsersActivity.class));
                return true;
            }
            return true;
        });
    }

    private void setupClickListeners() {
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnAppSettings.setOnClickListener(v -> 
                Toast.makeText(this, "App Settings coming soon", Toast.LENGTH_SHORT).show());

        btnPrivacy.setOnClickListener(v -> 
                Toast.makeText(this, "Privacy Policy loaded", Toast.LENGTH_SHORT).show());

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
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
