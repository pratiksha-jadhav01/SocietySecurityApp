package com.example.securiodb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.securiodb.adapter.VisitorAdapter;
import com.example.securiodb.model.Visitor;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OwnerDashboardActivity extends AppCompatActivity implements VisitorAdapter.OnVisitorActionListener {

    private TextView tvFlatTitle, tvOwnerName, tvStatPending, tvStatApproved, tvStatRejected;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ImageView ivLogout;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userFlatNumber = "";

    private List<Visitor> pendingList = new ArrayList<>();
    private List<Visitor> historyList = new ArrayList<>();
    private VisitorAdapter pendingAdapter, historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupViewPager();
        fetchOwnerProfile();

        ivLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void initViews() {
        tvFlatTitle = findViewById(R.id.tvFlatTitle);
        tvOwnerName = findViewById(R.id.tvOwnerName);
        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatApproved = findViewById(R.id.tvStatApproved);
        tvStatRejected = findViewById(R.id.tvStatRejected);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        ivLogout = findViewById(R.id.ivLogout);
        progressBar = findViewById(R.id.progressBar);
    }

    private void fetchOwnerProfile() {
        progressBar.setVisibility(View.VISIBLE);
        String uid = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    userFlatNumber = snapshot.child("flatNumber").getValue(String.class);

                    tvOwnerName.setText(name);
                    tvFlatTitle.setText("My Apartment — Flat " + userFlatNumber);
                    
                    listenForVisitors();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void listenForVisitors() {
        mDatabase.child("visitors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingList.clear();
                historyList.clear();
                int pendingCount = 0, approvedCount = 0, rejectedCount = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Visitor visitor = data.getValue(Visitor.class);
                    if (visitor != null && userFlatNumber.equals(visitor.flatNumber)) {
                        if ("Pending".equalsIgnoreCase(visitor.status)) {
                            pendingList.add(visitor);
                            pendingCount++;
                        } else {
                            historyList.add(visitor);
                            if ("Approved".equalsIgnoreCase(visitor.status)) approvedCount++;
                            else if ("Rejected".equalsIgnoreCase(visitor.status)) rejectedCount++;
                        }
                    }
                }

                Collections.reverse(pendingList);
                Collections.reverse(historyList);

                tvStatPending.setText(String.valueOf(pendingCount));
                tvStatApproved.setText(String.valueOf(approvedCount));
                tvStatRejected.setText(String.valueOf(rejectedCount));

                if (pendingAdapter != null) pendingAdapter.notifyDataSetChanged();
                if (historyAdapter != null) historyAdapter.notifyDataSetChanged();
                
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter();
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "PENDING" : "HISTORY");
        }).attach();
    }

    @Override
    public void onApprove(Visitor visitor) {
        updateVisitorStatus(visitor.visitorId, "Approved");
    }

    @Override
    public void onReject(Visitor visitor) {
        updateVisitorStatus(visitor.visitorId, "Rejected");
    }

    private void updateVisitorStatus(String visitorId, String status) {
        mDatabase.child("visitors").child(visitorId).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> {
                    Snackbar.make(viewPager, "Visitor " + status, Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    // Simple Adapter for ViewPager2 to hold the two lists
    private class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RecyclerView recyclerView = new RecyclerView(parent.getContext());
            recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            recyclerView.setLayoutManager(new LinearLayoutManager(parent.getContext()));
            return new ViewHolder(recyclerView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position == 0) {
                pendingAdapter = new VisitorAdapter(pendingList, true, OwnerDashboardActivity.this);
                holder.recyclerView.setAdapter(pendingAdapter);
            } else {
                historyAdapter = new VisitorAdapter(historyList, false, OwnerDashboardActivity.this);
                holder.recyclerView.setAdapter(historyAdapter);
            }
        }

        @Override
        public int getItemCount() { return 2; }

        class ViewHolder extends RecyclerView.ViewHolder {
            RecyclerView recyclerView;
            ViewHolder(View itemView) {
                super(itemView);
                recyclerView = (RecyclerView) itemView;
            }
        }
    }
}
