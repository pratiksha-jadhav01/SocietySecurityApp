package com.example.securiodb;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * AdminNoticeActivity allows the administrator to post new notices to the society board.
 * It validates inputs and saves the data to Firebase Firestore.
 */
public class AdminNoticeActivity extends AppCompatActivity {

    private TextInputLayout tilTitle, tilMessage;
    private TextInputEditText etTitle, etMessage;
    private MaterialButton btnPostNotice;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notice);

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();

        // Bind UI components to variables
        tilTitle   = findViewById(R.id.tilTitle);
        tilMessage = findViewById(R.id.tilMessage);
        etTitle    = findViewById(R.id.etTitle);
        etMessage  = findViewById(R.id.etMessage);
        btnPostNotice = findViewById(R.id.btnPostNotice);

        // Setup back button click listener to finish activity
        findViewById(R.id.btnBack)
            .setOnClickListener(v -> finish());

        // Setup post button click listener
        btnPostNotice.setOnClickListener(v -> validateAndPost());
    }

    /**
     * Validates the title and message fields before attempting to post to Firestore.
     */
    private void validateAndPost() {
        String title = etTitle.getText() != null
            ? etTitle.getText().toString().trim() : "";
        String message = etMessage.getText() != null
            ? etMessage.getText().toString().trim() : "";

        // Validate title - cannot be empty
        if (TextUtils.isEmpty(title)) {
            tilTitle.setError("Notice title is required");
            return;
        } else {
            tilTitle.setError(null);
        }

        // Validate message - cannot be empty
        if (TextUtils.isEmpty(message)) {
            tilMessage.setError("Notice message is required");
            return;
        } else {
            tilMessage.setError(null);
        }

        // Disable button to prevent double submission during network call
        btnPostNotice.setEnabled(false);
        btnPostNotice.setText("Posting...");

        postNoticeToFirestore(title, message);
    }

    /**
     * Posts the validated notice data to the 'notices' collection in Firebase Firestore.
     * @param title The title of the notice.
     * @param message The body content of the notice.
     */
    private void postNoticeToFirestore(String title, String message) {

        // Build the notice data map
        Map<String, Object> notice = new HashMap<>();
        notice.put("title",     title);
        notice.put("message",   message);
        notice.put("createdBy", "admin");
        // Use ServerTimestamp for consistent timing across all clients
        notice.put("timestamp", FieldValue.serverTimestamp());

        // Add a new document to the "notices" collection
        db.collection("notices")
            .add(notice)
            .addOnSuccessListener(documentReference -> {
                // Inform user of success
                Toast.makeText(this, "Notice posted successfully!", Toast.LENGTH_SHORT).show();
                
                // Clear the input fields for a clean state
                etTitle.setText("");
                etMessage.setText("");
                
                // Reset button state and finish activity
                btnPostNotice.setEnabled(true);
                btnPostNotice.setText("POST NOTICE");
                finish();
            })
            .addOnFailureListener(e -> {
                // Inform user of failure
                Toast.makeText(this, "Failed to post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                
                // Re-enable the button so user can try again
                btnPostNotice.setEnabled(true);
                btnPostNotice.setText("POST NOTICE");
            });
    }
}
