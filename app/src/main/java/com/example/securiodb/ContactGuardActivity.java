package com.example.securiodb;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ContactGuardActivity extends AppCompatActivity {

    private RecyclerView rvGuards;
    private GuardContactAdapter adapter;
    private List<User> guardList = new ArrayList<>();
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_guard);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        rvGuards = findViewById(R.id.rvGuards);
        rvGuards.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GuardContactAdapter(guardList);
        rvGuards.setAdapter(adapter);

        fetchGuards();
    }

    private void fetchGuards() {
        mDatabase.child("users")
                .orderByChild("role")
                .equalTo("guard")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        guardList.clear();
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            User guard = postSnapshot.getValue(User.class);
                            if (guard != null) {
                                guard.setUid(postSnapshot.getKey()); // Ensure UID is set for deletion
                                guardList.add(guard);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ContactGuardActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteGuard(User guard) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Guard")
                .setMessage("Are you sure you want to delete guard " + guard.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    mDatabase.child("users").child(guard.getUid()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(ContactGuardActivity.this, "Guard deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(ContactGuardActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class GuardContactAdapter extends RecyclerView.Adapter<GuardContactAdapter.ViewHolder> {
        private List<User> list;
        public GuardContactAdapter(List<User> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guard_contact, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User guard = list.get(position);
            holder.tvName.setText(guard.getName() != null ? guard.getName() : "Unknown Guard");
            holder.tvPhone.setText(guard.getPhone() != null ? guard.getPhone() : "No Phone Available");
            
            holder.btnCall.setOnClickListener(v -> {
                if (guard.getPhone() != null) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + guard.getPhone()));
                    startActivity(intent);
                }
            });
            
            holder.btnSms.setOnClickListener(v -> {
                if (guard.getPhone() != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("sms:" + guard.getPhone()));
                    startActivity(intent);
                }
            });

            holder.ivDelete.setOnClickListener(v -> deleteGuard(guard));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone;
            MaterialButton btnCall, btnSms;
            ImageView ivDelete;
            public ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvGuardName);
                tvPhone = v.findViewById(R.id.tvGuardPhone);
                btnCall = v.findViewById(R.id.btnCall);
                btnSms = v.findViewById(R.id.btnSms);
                ivDelete = v.findViewById(R.id.ivDeleteGuard);
            }
        }
    }
}
