package com.example.securiodb;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ContactGuardActivity extends AppCompatActivity {

    private RecyclerView rvGuards;
    private GuardContactAdapter adapter;
    private List<User> guardList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_guard);

        rvGuards = findViewById(R.id.rvGuards);
        rvGuards.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GuardContactAdapter(guardList);
        rvGuards.setAdapter(adapter);

        fetchGuards();
    }

    private void fetchGuards() {
        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("role", "guard")
                .get()
                .addOnSuccessListener(value -> {
                    guardList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        guardList.add(doc.toObject(User.class));
                    }
                    adapter.notifyDataSetChanged();
                });
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
            holder.tvName.setText(guard.getName());
            holder.tvPhone.setText(guard.getPhone());
            
            holder.btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + guard.getPhone()));
                startActivity(intent);
            });
            
            holder.btnSms.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("sms:" + guard.getPhone()));
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone;
            MaterialButton btnCall, btnSms;
            public ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvGuardName);
                tvPhone = v.findViewById(R.id.tvGuardPhone);
                btnCall = v.findViewById(R.id.btnCall);
                btnSms = v.findViewById(R.id.btnSms);
            }
        }
    }
}
