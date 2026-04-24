package com.example.securiodb.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.R;
import com.example.securiodb.adapter.NoticeAdapter;
import com.example.securiodb.models.Notice;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NoticeBoardFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView recycler;
    private List<Notice> noticeList = new ArrayList<>();
    private NoticeAdapter adapter;
    private ListenerRegistration listenerReg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notice_board, container, false);

        db = FirebaseFirestore.getInstance();
        recycler = view.findViewById(R.id.recyclerNotices);

        adapter = new NoticeAdapter(noticeList);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        loadNotices();
        return view;
    }

    private void loadNotices() {
        listenerReg = db.collection("notices")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                noticeList.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Notice notice = doc.toObject(Notice.class);
                    if (notice != null) {
                        noticeList.add(notice);
                    }
                }
                adapter.notifyDataSetChanged();
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerReg != null) listenerReg.remove();
    }
}
