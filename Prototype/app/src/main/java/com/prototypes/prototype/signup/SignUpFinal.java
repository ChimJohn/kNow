package com.prototypes.prototype.signup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.login.LoginActivity;
import com.prototypes.prototype.R;
import com.prototypes.prototype.user.User;

public class SignUpFinal extends AppCompatActivity {
    TextView tvEmail, tvPassword;
    Button btnSignUp;
    String username;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_final);

        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(this);
        FirestoreManager firestoreManager = new FirestoreManager(db, User.class);

//        UI elements
        tvEmail = findViewById(R.id.etSignUpEmail);
        tvPassword = findViewById(R.id.etSignUpPassword);
        btnSignUp = findViewById(R.id.btnSignUp);

//        Get username from previous activity
        Intent intent = getIntent();
        username = intent.getStringExtra("username");

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = tvEmail.getText().toString();
                String password = tvPassword.getText().toString().trim();

                // Check if email is empty
                if (TextUtils.isEmpty(email)){
                    Toast.makeText(SignUpFinal.this, "Enter email", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Check if password is empty
                if (TextUtils.isEmpty(password)){
                    Toast.makeText(SignUpFinal.this, "Enter password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Register user
                firebaseAuthManager.registerUser(email, password, new FirebaseAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        Toast.makeText(SignUpFinal.this, "Account created.",
                                Toast.LENGTH_SHORT).show();

                        // Create user object and add to firestore
                        User newuser = new User(username, email);
                        firestoreManager.writeDocument("Users", firebaseAuthManager.getCurrentUser().getUid(), newuser, new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                // Navigate
                                Intent intent = new Intent(SignUpFinal.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(SignUpFinal.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(SignUpFinal.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

    }
}