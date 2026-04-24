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
import com.example.securiodb.models.NoticeModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * NoticeBoardFragment displays the society's notices in a fragment, 
 * typically used within a dashboard or a dedicated notice board section.
 */
public class NoticeBoardFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView recycler;
    private List<NoticeModel> noticeList = new ArrayList<>();
    private NoticeAdapter adapter;
    private ListenerRegistration listenerReg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notice_board, container, false);

        db = FirebaseFirestore.getInstance();
        recycler = view.findViewById(R.id.recyclerNotices);

        // Initialize the adapter with NoticeModel list
        adapter = new NoticeAdapter(requireContext(), noticeList);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        loadNotices();
        return view;
    }

    /**
     * Loads notices from Firestore in real-time, ordered by timestamp.
     */
    private void loadNotices() {
        listenerReg = db.collection("notices")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                
                noticeList.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    String id = doc.getId();
                    
                    // Extract fields with null safety
                    String title = doc.getString("title");
                    if (title == null) title = "No Title";
                    
                    String message = doc.getString("message");
                    if (message == null) message = "";
                    
                    String createdBy = doc.getString("createdBy");
                    if (createdBy == null) createdBy = "admin";
                    
                    Timestamp ts = doc.getTimestamp("timestamp");
                    long timestampMillis = (ts != null) ? ts.toDate().getTime() : 0;

                    // Add to list using the standard NoticeModel
                    noticeList.add(new NoticeModel(id, title, message, createdBy, timestampMillis));
                }
                adapter.notifyDataSetChanged();
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the listener to avoid memory leaks
        if (listenerReg != null) {
            listenerReg.remove();
        }
    }
}
