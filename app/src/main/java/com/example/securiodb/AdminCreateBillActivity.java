package com.example.securiodb;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.securiodb.models.Bill;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.UUID;

public class AdminCreateBillActivity extends AppCompatActivity {

    private TextInputEditText etFlatNo, etAmount, etDueDate;
    private AutoCompleteTextView spinnerMonth;
    private MaterialButton btnGenerateBill;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_create_bill);

        db = FirebaseFirestore.getInstance();

        etFlatNo = findViewById(R.id.etFlatNo);
        etAmount = findViewById(R.id.etAmount);
        etDueDate = findViewById(R.id.etDueDate);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        btnGenerateBill = findViewById(R.id.btnGenerateBill);
        progressBar = findViewById(R.id.progressBar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupMonthSpinner();
        setupDatePicker();

        btnGenerateBill.setOnClickListener(v -> generateBill());
    }

    private void setupMonthSpinner() {
        String[] months = {"January", "February", "March", "April", "May", "June", 
                          "July", "August", "September", "October", "November", "December"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, months);
        spinnerMonth.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etDueDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
                String date = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
                etDueDate.setText(date);
            }, year, month, day);
            datePickerDialog.show();
        });
    }

    private void generateBill() {
        String flatNo = etFlatNo.getText().toString().trim();
        String month = spinnerMonth.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();

        if (flatNo.isEmpty() || month.isEmpty() || amountStr.isEmpty() || dueDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String billId = UUID.randomUUID().toString();
        
        Bill bill = new Bill(billId, flatNo, month, amount, dueDate, "due", Timestamp.now());

        progressBar.setVisibility(View.VISIBLE);
        btnGenerateBill.setEnabled(false);

        db.collection("bills").document(billId)
                .set(bill)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdminCreateBillActivity.this, "Bill Generated Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnGenerateBill.setEnabled(true);
                    Toast.makeText(AdminCreateBillActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
