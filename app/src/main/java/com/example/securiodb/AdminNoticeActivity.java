package com.example.securiodb;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminNoticeActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etMessage;
    private MaterialButton btnPostNotice;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notice);

        db = FirebaseFirestore.getInstance();

        etTitle = findViewById(R.id.etTitle);
        etMessage = findViewById(R.id.etMessage);
        btnPostNotice = findViewById(R.id.btnPostNotice);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPostNotice.setOnClickListener(v -> postNotice());
    }

    private void postNotice() {
        String title = etTitle.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            return;
        }

        if (TextUtils.isEmpty(message)) {
            etMessage.setError("Message is required");
            return;
        }

        Map<String, Object> notice = new HashMap<>();
        notice.put("title", title);
        notice.put("message", message);
        notice.put("createdBy", "admin");
        notice.put("timestamp", FieldValue.serverTimestamp());

        db.collection("notices")
                .add(notice)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Notice Posted Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to post notice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
