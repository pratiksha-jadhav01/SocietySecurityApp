package com.example.securiodb;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securiodb.adapter.UserAdapter;
import com.example.securiodb.models.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageUsersActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {

    private TabLayout tabLayout;
    private RecyclerView rvUsers;
    private FloatingActionButton fabAddUser;
    private ProgressBar progressBar;
    
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private UserAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private String currentRoleFilter = "guard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupTabs();
        loadUsers(currentRoleFilter);

        fabAddUser.setOnClickListener(v -> showAddUserBottomSheet());
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        rvUsers = findViewById(R.id.rvUsers);
        fabAddUser = findViewById(R.id.fabAddUser);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(userList, this);
        rvUsers.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentRoleFilter = tab.getPosition() == 0 ? "guard" : "owner";
                loadUsers(currentRoleFilter);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadUsers(String role) {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("users").orderByChild("role").equalTo(role)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        userList.clear();
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            User user = ds.getValue(User.class);
                            if (user != null) {
                                userList.add(user);
                            }
                        }
                        adapter.updateList(userList);
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void showAddUserBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_user, null);
        dialog.setContentView(view);

        TextInputEditText etName = view.findViewById(R.id.etName);
        TextInputEditText etEmail = view.findViewById(R.id.etEmail);
        TextInputEditText etPass = view.findViewById(R.id.etPassword);
        TextInputEditText etPhone = view.findViewById(R.id.etPhone);
        TextInputEditText etFlat = view.findViewById(R.id.etFlat);
        TextInputLayout tilFlat = view.findViewById(R.id.tilFlat);
        AutoCompleteTextView spRole = view.findViewById(R.id.spRole);
        MaterialButton btnAdd = view.findViewById(R.id.btnAddUser);

        // Setup Role Dropdown
        String[] roles = {"Guard", "Owner"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roles);
        spRole.setAdapter(roleAdapter);
        spRole.setOnItemClickListener((parent, v, position, id) -> {
            tilFlat.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        });

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String role = spRole.getText().toString().toLowerCase();
            String flat = etFlat.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            
            // Note: Since Firebase Auth doesn't allow creating multiple users easily from one client
            // without logging out the admin, usually this is done via a Firebase Cloud Function.
            // However, to fix the "email already in use" issue (which might be because it's checking Firestore
            // but the user exists in Auth, or vice versa), we should ensure consistency.
            
            // For now, let's keep the client-side Auth creation but use Realtime Database
            // to match what RegisterActivity and LoginActivity use.
            
            mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = task.getResult().getUser().getUid();
                    Map<String, Object> user = new HashMap<>();
                    user.put("userId", uid);
                    user.put("name", name);
                    user.put("email", email);
                    user.put("phone", phone);
                    user.put("role", role);
                    user.put("flatNumber", flat);
                    user.put("createdAt", ServerValue.TIMESTAMP);

                    mDatabase.child("users").child(uid).setValue(user).addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        dialog.dismiss();
                        Toast.makeText(this, "User Added Successfully", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    @Override
    public void onDeleteClick(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() + "?\nNote: Auth account must be deleted manually or via Cloud Functions.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    mDatabase.child("users").child(user.getUserId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "User record removed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
