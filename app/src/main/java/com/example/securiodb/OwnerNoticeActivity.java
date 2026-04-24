package com.example.securiodb;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.NoticeAdapter;
import com.example.securiodb.models.Notice;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class OwnerNoticeActivity extends AppCompatActivity {

    private RecyclerView rvNotices;
    private NoticeAdapter adapter;
    private List<Notice> noticeList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_notice);

        db = FirebaseFirestore.getInstance();
        rvNotices = findViewById(R.id.rvNotices);
        rvNotices.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoticeAdapter(noticeList);
        rvNotices.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        fetchNotices();
    }

    private void fetchNotices() {
        db.collection("notices")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("OwnerNoticeActivity", "Listen failed", error);
                        return;
                    }

                    if (value != null) {
                        noticeList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Notice notice = doc.toObject(Notice.class);
                            if (notice != null) {
                                noticeList.add(notice);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
