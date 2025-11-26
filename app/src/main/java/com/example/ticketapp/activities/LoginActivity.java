package com.example.ticketapp.activities;

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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.DataSnapshot;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvNoAccount, tvForgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvNoAccount = findViewById(R.id.tvNoAccount);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvNoAccount.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                tilEmail.setError("Enter a valid email to reset password");
            } else {
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Toast.makeText(LoginActivity.this, "Reset link sent to your email", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void attemptLogin(){
        // Clear previous errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        boolean valid = true;

        if(email.isEmpty()){
            tilEmail.setError("Please enter your email");
            valid = false;
        } else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            tilEmail.setError("Enter a valid email");
            valid = false;
        }

        if(password.isEmpty()){
            tilPassword.setError("Please enter your password");
            valid = false;
        }

        if(!valid) return;

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            btnLogin.setEnabled(true);
            if(task.isSuccessful()){
                String uid = mAuth.getCurrentUser().getUid();
                // Fetch user role from database
                FirebaseHelper.getUsersRef().child(uid).get().addOnCompleteListener(snapshotTask -> {
                    if(snapshotTask.isSuccessful()){
                        DataSnapshot snapshot = snapshotTask.getResult();
                        if(snapshot.exists()){
                            String role = snapshot.child("role").getValue(String.class);
                            if("admin".equals(role)){
                                startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                            } else {
                                startActivity(new Intent(LoginActivity.this, UserDashboardActivity.class));
                            }
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "User data not found", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed to get user data: " +
                                (snapshotTask.getException() != null ? snapshotTask.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Exception ex = task.getException();
                if(ex instanceof FirebaseAuthInvalidUserException){
                    tilEmail.setError("No account found with this email");
                    tilEmail.requestFocus();
                } else if(ex instanceof FirebaseAuthInvalidCredentialsException){
                    tilPassword.setError("Incorrect password");
                    tilPassword.requestFocus();
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed: " + (ex != null ? ex.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
