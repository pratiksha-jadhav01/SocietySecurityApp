package com.example.securiodb;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.NoticeAdapter;
import com.example.securiodb.models.Notice;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminNoticeActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etMessage;
    private MaterialButton btnPostNotice;
    private RecyclerView rvNotices;
    private NoticeAdapter adapter;
    private List<Notice> noticeList = new ArrayList<>();
    private LinearLayout layoutEmpty;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notice);

        db = FirebaseFirestore.getInstance();

        etTitle = findViewById(R.id.etTitle);
        etMessage = findViewById(R.id.etMessage);
        btnPostNotice = findViewById(R.id.btnPostNotice);
        rvNotices = findViewById(R.id.recyclerNotices);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvNotices.setLayoutManager(new LinearLayoutManager(this));
        // Pass true to indicate this is an Admin session for the delete icon
        adapter = new NoticeAdapter(noticeList, true);
        rvNotices.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPostNotice.setOnClickListener(v -> postNotice());
        
        fetchNotices();
    }

    private void fetchNotices() {
        db.collection("notices")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        noticeList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Notice notice = doc.toObject(Notice.class);
                            if (notice != null) {
                                noticeList.add(notice);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        if (noticeList.isEmpty()) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                            rvNotices.setVisibility(View.GONE);
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                            rvNotices.setVisibility(View.VISIBLE);
                        }
                    }
                });
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

        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setMessage(message);
        notice.setCreatedBy("Admin");
        notice.setTimestamp(Timestamp.now());

        btnPostNotice.setEnabled(false);
        db.collection("notices")
                .add(notice)
                .addOnSuccessListener(documentReference -> {
                    btnPostNotice.setEnabled(true);
                    etTitle.setText("");
                    etMessage.setText("");
                    Toast.makeText(this, "Notice Posted Successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnPostNotice.setEnabled(true);
                    Toast.makeText(this, "Failed to post notice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
