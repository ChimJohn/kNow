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
import com.prototypes.prototype.login.LoginActivity;
import com.prototypes.prototype.R;
import com.prototypes.prototype.user.User;

public class SignUpFinal extends AppCompatActivity {
    TextView tvEmail, tvPassword;
    FirebaseAuth mAuth;
    Button btnSignUp;
    String username;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_final);
        mAuth = FirebaseAuth.getInstance();
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

                if (TextUtils.isEmpty(email)){
                    Toast.makeText(SignUpFinal.this, "Enter email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)){
                    Toast.makeText(SignUpFinal.this, "Enter password", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(SignUpFinal.this, "Account created.",
                                            Toast.LENGTH_SHORT).show();

                                    User newuser = new User(username, email);
                                    // Insert new user to Firestore
                                    db.collection("Users").document(user.getUid()).set(newuser);


                                    Intent intent = new Intent(SignUpFinal.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(SignUpFinal.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });


            }
        });

    }
}