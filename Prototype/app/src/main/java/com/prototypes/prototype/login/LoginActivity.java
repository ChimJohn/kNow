package com.prototypes.prototype.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView; // Make sure to import TextView
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.prototypes.prototype.MainActivity;
import com.prototypes.prototype.R;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.signup.SignUpActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView btnSignUp, btnForgotPassword;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(this);
        if(firebaseAuthManager.getCurrentUser() != null){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login); // Ensure the correct XML file is set
        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(this);

        // Reference UI elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp); // Fix: Change Button to TextView
        btnForgotPassword = findViewById(R.id.btnForgotPassword); // Fix: Change Button to TextView

        // Handle Login Button Click
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                firebaseAuthManager.loginUser(email, password, new FirebaseAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Handle Sign Up Click
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        // Handle Forgot Password Click (Optional)
        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, "Forgot Password Clicked!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
