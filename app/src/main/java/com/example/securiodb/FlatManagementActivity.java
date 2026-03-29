package com.example.securiodb;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.models.Flat;
import com.example.securiodb.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FlatManagementActivity extends AppCompatActivity {

    private RecyclerView rvFlats;
    private FloatingActionButton fabAddFlat;
    private ProgressBar progressBar;
    
    private FirebaseFirestore db;
    private FlatAdapter adapter;
    private List<Flat> flatList = new ArrayList<>();
    private ListenerRegistration flatsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flat_management);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        loadFlats();

        fabAddFlat.setOnClickListener(v -> showAddFlatDialog());
    }

    private void initViews() {
        rvFlats = findViewById(R.id.rvFlats);
        fabAddFlat = findViewById(R.id.fabAddFlat);
        progressBar = findViewById(R.id.progressBar);
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvFlats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FlatAdapter(flatList);
        rvFlats.setAdapter(adapter);
    }

    private void loadFlats() {
        progressBar.setVisibility(View.VISIBLE);
        flatsListener = db.collection("flats").orderBy("flatNumber")
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (value != null) {
                        flatList.clear();
                        flatList.addAll(value.toObjects(Flat.class));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showAddFlatDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_flat, null);
        TextInputEditText etNumber = view.findViewById(R.id.etFlatNumber);
        TextInputEditText etFloor = view.findViewById(R.id.etFloor);

        new AlertDialog.Builder(this)
                .setTitle("Add New Flat")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String number = etNumber.getText().toString().trim();
                    String floor = etFloor.getText().toString().trim();

                    if (!TextUtils.isEmpty(number) && !TextUtils.isEmpty(floor)) {
                        String id = db.collection("flats").document().getId();
                        Flat flat = new Flat(id, number, "Unassigned", null, true, floor);
                        db.collection("flats").document(id).set(flat);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAssignOwnerDialog(Flat flat) {
        db.collection("users").whereEqualTo("role", "owner").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<User> owners = queryDocumentSnapshots.toObjects(User.class);
            List<String> ownerNames = new ArrayList<>();
            for (User u : owners) ownerNames.add(u.getName() + " (" + u.getEmail() + ")");

            String[] ownerArray = ownerNames.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle("Assign Owner to " + flat.getFlatNumber())
                    .setItems(ownerArray, (dialog, which) -> {
                        User selectedOwner = owners.get(which);
                        db.collection("flats").document(flat.getFlatId())
                                .update("ownerName", selectedOwner.getName(), "ownerId", selectedOwner.getUserId());
                        
                        // Also update flat number in user record
                        db.collection("users").document(selectedOwner.getUserId())
                                .update("flatNumber", flat.getFlatNumber());
                    })
                    .show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (flatsListener != null) flatsListener.remove();
    }

    // Inner Adapter Class
    class FlatAdapter extends RecyclerView.Adapter<FlatAdapter.FlatViewHolder> {
        private List<Flat> flats;

        FlatAdapter(List<Flat> flats) { this.flats = flats; }

        @NonNull
        @Override
        public FlatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flat, parent, false);
            return new FlatViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull FlatViewHolder holder, int position) {
            Flat flat = flats.get(position);
            holder.tvNumber.setText(flat.getFlatNumber());
            holder.tvOwner.setText("Owner: " + flat.getOwnerName());
            holder.tvFloor.setText("Floor: " + flat.getFloor());
            holder.switchStatus.setChecked(flat.isActive());
            
            holder.switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                db.collection("flats").document(flat.getFlatId()).update("isActive", isChecked);
            });

            holder.btnAssign.setOnClickListener(v -> showAssignOwnerDialog(flat));
        }

        @Override
        public int getItemCount() { return flats.size(); }

        class FlatViewHolder extends RecyclerView.ViewHolder {
            TextView tvNumber, tvOwner, tvFloor;
            SwitchMaterial switchStatus;
            MaterialButton btnAssign;

            FlatViewHolder(View v) {
                super(v);
                tvNumber = v.findViewById(R.id.tvFlatNumber);
                tvOwner = v.findViewById(R.id.tvOwnerName);
                tvFloor = v.findViewById(R.id.tvFloor);
                switchStatus = v.findViewById(R.id.switchStatus);
                btnAssign = v.findViewById(R.id.btnAssignOwner);
            }
        }
    }
}
