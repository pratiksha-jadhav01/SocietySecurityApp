package com.example.securiodb.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securiodb.ContactGuardActivity;
import com.example.securiodb.DeliveryManagementActivity;
import com.example.securiodb.OwnerApprovalsActivity;
import com.example.securiodb.OwnerComplaintActivity;
import com.example.securiodb.OwnerMaintenanceActivity;
import com.example.securiodb.OwnerNoticeActivity;
import com.example.securiodb.R;
import com.example.securiodb.adapter.NoticeAdapter;
import com.example.securiodb.models.Notice;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeDashboardFragment extends Fragment {

    private String flatNo;
    private FirebaseFirestore db;
    private TextView tvGreeting, tvFlatInfo, tvStatPending,
                     tvStatToday, tvStatHelpers, tvStatComplaints,
                     tvPendingSubtitle, tvBillStatus, tvStatDeliveries, tvRecentNoticesLabel;
    private ImageView ivSmallAvatar;
    private RecyclerView recyclerNoticesPreview;
    private NoticeAdapter noticeAdapter;
    private List<Notice> noticeList = new ArrayList<>();
    private List<ListenerRegistration> listeners = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(
            R.layout.fragment_home_dashboard, container, false);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null)
            flatNo = getArguments().getString("flatNo", "");

        bindViews(view);
        setupNoticePreview();
        setGreeting();
        fetchUserProfile();
        loadStats();
        setupQuickActions(view);
        return view;
    }

    private void bindViews(View v) {
        tvGreeting        = v.findViewById(R.id.tvGreeting);
        tvFlatInfo        = v.findViewById(R.id.tvFlatInfo);
        tvStatPending     = v.findViewById(R.id.tvStatPending);
        tvStatToday       = v.findViewById(R.id.tvStatToday);
        tvStatDeliveries  = v.findViewById(R.id.tvStatDeliveries);
        tvStatHelpers     = v.findViewById(R.id.tvStatHelpers);
        tvStatComplaints  = v.findViewById(R.id.tvStatComplaints);
        tvPendingSubtitle = v.findViewById(R.id.tvPendingSubtitle);
        tvBillStatus      = v.findViewById(R.id.tvBillStatus);
        ivSmallAvatar     = v.findViewById(R.id.ivSmallAvatar);
        
        tvRecentNoticesLabel = v.findViewById(R.id.tvRecentNoticesLabel);
        recyclerNoticesPreview = v.findViewById(R.id.recyclerNoticesPreview);
    }

    private void setupNoticePreview() {
        noticeAdapter = new NoticeAdapter(noticeList, false);
        recyclerNoticesPreview.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerNoticesPreview.setAdapter(noticeAdapter);

        ListenerRegistration noticeListener = db.collection("notices")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(3)
            .addSnapshotListener((snap, e) -> {
                if (snap != null && isAdded()) {
                    noticeList.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Notice n = doc.toObject(Notice.class);
                        if (n != null) noticeList.add(n);
                    }
                    noticeAdapter.notifyDataSetChanged();
                    
                    if (!noticeList.isEmpty()) {
                        tvRecentNoticesLabel.setVisibility(View.VISIBLE);
                        recyclerNoticesPreview.setVisibility(View.VISIBLE);
                    } else {
                        tvRecentNoticesLabel.setVisibility(View.GONE);
                        recyclerNoticesPreview.setVisibility(View.GONE);
                    }
                }
            });
        listeners.add(noticeListener);
    }

    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good Morning!" :
                          hour < 17 ? "Good Afternoon!" : "Good Evening!";
        if (tvGreeting != null) tvGreeting.setText(greeting);
        if (tvFlatInfo != null) tvFlatInfo.setText("Flat " + (flatNo != null ? flatNo : "---") + " · Owner");
    }

    private void fetchUserProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                String profileUrl = doc.getString("profileImageUrl");
                if (profileUrl != null && !profileUrl.isEmpty() && ivSmallAvatar != null) {
                    Glide.with(this)
                        .load(profileUrl)
                        .placeholder(android.R.drawable.ic_menu_report_image)
                        .circleCrop()
                        .into(ivSmallAvatar);
                }
            }
        });
    }

    private void loadStats() {
        if (flatNo == null || flatNo.isEmpty()) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        java.util.Date startOfDay = cal.getTime();

        // Stat 1: Pending visitors (Today only)
        ListenerRegistration r1 = db.collection("visitors")
            .whereEqualTo("flatNumber", flatNo)
            .whereEqualTo("status", "Pending")
            .whereEqualTo("purpose", "Visitor")
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                int count = snap.size();
                if (tvStatPending != null) tvStatPending.setText(String.valueOf(count));
                if (tvPendingSubtitle != null) tvPendingSubtitle.setText(count + " pending approvals");
            });
        listeners.add(r1);

        // Stat 2: Visitors today
        ListenerRegistration r2 = db.collection("visitors")
            .whereEqualTo("flatNumber", flatNo)
            .whereEqualTo("purpose", "Visitor")
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                if (tvStatToday != null) tvStatToday.setText(String.valueOf(snap.size()));
            });
        listeners.add(r2);

        // Stat 5: Deliveries today
        ListenerRegistration r5 = db.collection("visitors")
            .whereEqualTo("flatNumber", flatNo)
            .whereEqualTo("purpose", "Delivery")
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                if (tvStatDeliveries != null) tvStatDeliveries.setText(String.valueOf(snap.size()));
            });
        listeners.add(r5);

        // Stat 3: Active daily helpers
        ListenerRegistration r3 = db.collection("dailyHelpers")
            .whereEqualTo("flatNumber", flatNo)
            .whereEqualTo("isActive", true)
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                if (tvStatHelpers != null) tvStatHelpers.setText(String.valueOf(snap.size()));
            });
        listeners.add(r3);

        // Stat 4: Open complaints
        ListenerRegistration r4 = db.collection("complaints")
            .whereEqualTo("flatNo", flatNo)
            .whereEqualTo("status", "Open")
            .addSnapshotListener((snap, e) -> {
                if (snap == null || !isAdded()) return;
                if (tvStatComplaints != null) tvStatComplaints.setText(String.valueOf(snap.size()));
            });
        listeners.add(r4);

        // Bill status (Firestore "bills" collection)
        db.collection("bills")
            .whereEqualTo("flatNo", flatNo)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1).get()
            .addOnSuccessListener(snap -> {
                if (!snap.isEmpty() && isAdded()) {
                    String status = snap.getDocuments().get(0).getString("status");
                    if (tvBillStatus != null) {
                        tvBillStatus.setText(status);
                        tvBillStatus.setTextColor("Paid".equalsIgnoreCase(status)
                            ? Color.parseColor("#2E7D32")
                            : Color.parseColor("#C62828"));
                    }
                }
            });
    }

    private void setupQuickActions(View v) {
        // Card Approvals / Visitors
        View cardApprovals = v.findViewById(R.id.cardApprovals);
        if (cardApprovals != null) {
            cardApprovals.setOnClickListener(x -> startActivity(new Intent(getActivity(), OwnerApprovalsActivity.class)));
        }

        // Card Deliveries
        View cardDeliveries = v.findViewById(R.id.cardDeliveries);
        if (cardDeliveries != null) {
            cardDeliveries.setOnClickListener(x -> startActivity(new Intent(getActivity(), DeliveryManagementActivity.class)));
        }

        // Card Complaint
        View cardComplaint = v.findViewById(R.id.cardComplaint);
        if (cardComplaint != null) {
            cardComplaint.setOnClickListener(x -> startActivity(new Intent(getActivity(), OwnerComplaintActivity.class)));
        }

        // Card Helpers
        View cardHelpers = v.findViewById(R.id.cardHelpers);
        if (cardHelpers != null) {
            cardHelpers.setOnClickListener(x -> {
                if (getActivity() instanceof com.example.securiodb.OwnerDashboardActivity) {
                    com.example.securiodb.OwnerDashboardActivity activity = (com.example.securiodb.OwnerDashboardActivity) getActivity();
                    activity.loadFragment(new DailyHelperFragment());
                }
            });
        }

        // Card Maintenance
        View cardMaintenance = v.findViewById(R.id.cardMaintenance);
        if (cardMaintenance != null) {
            cardMaintenance.setOnClickListener(x -> {
                startActivity(new Intent(getActivity(), OwnerMaintenanceActivity.class));
            });
        }
        
        // Quick Action: Contact Guard
        View btnContactGuard = v.findViewById(R.id.btnContactGuard);
        if (btnContactGuard != null) {
            btnContactGuard.setOnClickListener(x -> startActivity(new Intent(getActivity(), ContactGuardActivity.class)));
        }

        // Quick Action: Notice Board
        View btnNoticeBoard = v.findViewById(R.id.btnNoticeBoard);
        if (btnNoticeBoard != null) {
            btnNoticeBoard.setOnClickListener(x -> startActivity(new Intent(getActivity(), OwnerNoticeActivity.class)));
        }

        // Quick Action: Raise Complaint
        View btnRaiseComplaint = v.findViewById(R.id.btnRaiseComplaint);
        if (btnRaiseComplaint != null) {
            btnRaiseComplaint.setOnClickListener(x -> startActivity(new Intent(getActivity(), OwnerComplaintActivity.class)));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ListenerRegistration r : listeners) r.remove();
        listeners.clear();
    }
}
