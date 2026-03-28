package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() != null) {
                // If user is logged in, check their role in Firestore
                String uid = mAuth.getCurrentUser().getUid();
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String role = documentSnapshot.getString("role");
                                navigateToDashboard(role);
                            } else {
                                // If user record doesn't exist, go to login
                                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                                finish();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(SplashActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                            finish();
                        });
            } else {
                // Not logged in, go to login screen
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, 2000); // 2 seconds delay
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
        startActivity(intent);
        finish();
    }
}
