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
 * Fragment that displays the society notice board for residents.
 * Listen to Firestore for real-time updates and displays them in a list.
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

        // Initialize adapter with the notice list
        adapter = new NoticeAdapter(requireContext(), noticeList);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        loadNotices();
        return view;
    }

    /**
     * Loads notices from Firestore and updates the list in real-time.
     */
    private void loadNotices() {
        listenerReg = db.collection("notices")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                
                noticeList.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    // Manual mapping to handle different timestamp types if necessary
                    String id = doc.getId();
                    String title = doc.getString("title");
                    String message = doc.getString("message");
                    String createdBy = doc.getString("createdBy");
                    
                    Timestamp firestoreTs = doc.getTimestamp("timestamp");
                    long tsMillis = (firestoreTs != null) ? firestoreTs.toDate().getTime() : 0;

                    noticeList.add(new NoticeModel(id, title, message, createdBy, tsMillis));
                }
                adapter.notifyDataSetChanged();
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener to prevent memory leaks
        if (listenerReg != null) {
            listenerReg.remove();
        }
    }
}
