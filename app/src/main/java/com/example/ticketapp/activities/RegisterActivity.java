package com.example.ticketapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ticketapp.R;
import com.example.ticketapp.models.User;
import com.example.ticketapp.utils.FirebaseHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.DatabaseReference;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirm;
    private TextInputEditText etName, etEmail, etPassword, etConfirm;
    private Button btnRegister;
    private TextView tvHaveAccount;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirm = findViewById(R.id.tilConfirm);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvHaveAccount = findViewById(R.id.tvHaveAccount);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseHelper.getUsersRef();

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvHaveAccount.setOnClickListener(v -> {
            // open login activity
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister(){
        // Clear previous errors
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);

        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirm = etConfirm.getText() != null ? etConfirm.getText().toString() : "";

        boolean valid = true;

        if(name.isEmpty()){
            tilName.setError("Please enter your full name");
            valid = false;
        }

        if(email.isEmpty()){
            tilEmail.setError("Please enter your email");
            valid = false;
        } else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            tilEmail.setError("Enter a valid email address");
            valid = false;
        }

        // Password rules: min 8 chars and must contain at least one digit
        if(password.isEmpty()){
            tilPassword.setError("Please enter a password");
            valid = false;
        } else if(password.length() < 8){
            tilPassword.setError("Password must be at least 8 characters");
            valid = false;
        } else if(!password.matches(".*\\d.*")){
            tilPassword.setError("Password must contain at least one number");
            valid = false;
        }

        if(confirm.isEmpty()){
            tilConfirm.setError("Please confirm your password");
            valid = false;
        } else if(!confirm.equals(password)){
            tilConfirm.setError("Passwords do not match");
            valid = false;
        }

        if(!valid) return;

        // Proceed with Firebase registration
        btnRegister.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, (OnCompleteListener<AuthResult>) task -> {
                    btnRegister.setEnabled(true);
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        User user = new User(uid, name, email, "user"); // default role
                        usersRef.child(uid).setValue(user)
                                .addOnCompleteListener(task2 -> {
                                    if(task2.isSuccessful()){
                                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                        // go back to login
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Database error: " + task2.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        // Handle common Firebase Auth exceptions with friendly messages
                        Exception ex = task.getException();
                        if (ex instanceof FirebaseAuthWeakPasswordException) {
                            tilPassword.setError(((FirebaseAuthWeakPasswordException) ex).getReason());
                            tilPassword.requestFocus();
                        } else if (ex instanceof FirebaseAuthInvalidCredentialsException) {
                            tilEmail.setError("Invalid email");
                            tilEmail.requestFocus();
                        } else if (ex instanceof FirebaseAuthUserCollisionException) {
                            tilEmail.setError("This email is already registered");
                            tilEmail.requestFocus();
                        } else {
                            // generic fallback
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + (ex != null ? ex.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
