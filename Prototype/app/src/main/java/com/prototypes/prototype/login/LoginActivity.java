package com.prototypes.prototype.login;

import static android.content.ContentValues.TAG;
import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView; // Make sure to import TextView
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.prototypes.prototype.MainActivity;
import com.prototypes.prototype.R;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.signup.SignUpActivity;

import com.google.firebase.auth.GoogleAuthProvider;
import com.prototypes.prototype.user.User;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    EditText etEmail, etPassword;
    Button btnLogin;
    TextView btnSignUp, btnForgotPassword;
    SignInButton btnGoogleSignIn;
//    private CredentialManager credentialManager;

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
        setContentView(R.layout.login);
        // Reference UI elements
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp); // Fix: Change Button to TextView
        btnForgotPassword = findViewById(R.id.btnForgotPassword); // Fix: Change Button to TextView
        btnGoogleSignIn = findViewById(R.id.sign_in_button);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
//        credentialManager = CredentialManager.create(getBaseContext());

        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(this);
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

//        btnGoogleSignIn.setOnClickListener(v -> {
//            launchCredentialManager();
//        });
//    }

//    private void launchCredentialManager() {
//        // [START create_credential_manager_request]
//        // Instantiate a Google sign-in request
//        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
//                .setFilterByAuthorizedAccounts(true)
//                .setServerClientId(getString(R.string.default_web_client_id))
//                .build();
//        // Create the Credential Manager request
//        GetCredentialRequest request = new GetCredentialRequest.Builder()
//                .addCredentialOption(googleIdOption)
//                .build();
//        // [END create_credential_manager_request]
//        // Launch Credential Manager UI
//        credentialManager.getCredentialAsync(
//                getBaseContext(),
//                request,
//                new CancellationSignal(),
//                Executors.newSingleThreadExecutor(),
//                new CredentialManagerCallback<>() {
//                    @Override
//                    public void onResult(GetCredentialResponse result) {
//                        // Extract credential from the result returned by Credential Manager
//                        handleSignIn(result.getCredential());
//                    }
//                    @Override
//                    public void onError(@NonNull GetCredentialException e) {
//                        Log.e(TAG, "Couldn't retrieve user's credentials: " + e.getLocalizedMessage());
//                    }
//                }
//        );
//    }
//    private void handleSignIn(Credential credential) {
//        // Check if credential is of type CustomCredential and type is Google ID
//        if (credential instanceof CustomCredential) {
//            CustomCredential customCredential = (CustomCredential) credential;
//            if (customCredential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
//                // Create Google ID Token
//                Bundle credentialData = customCredential.getData();
//                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);
//                // Sign in to Firebase using the token
//                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
//            } else {
//                Log.w(TAG, "Credential type is not Google ID Token!");
//            }
//        } else {
//            Log.w(TAG, "Credential is not of type CustomCredential!");
//        }
//    }
//    private void firebaseAuthWithGoogle(String idToken) {
//        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
//        mAuth.signInWithCredential(credential)
//                .addOnCompleteListener(this, task -> {
//                    if (task.isSuccessful()) {
//                        // Sign in success, update UI with the signed-in user's information
//                        Log.d(TAG, "signInWithCredential:success");
//                        FirebaseUser user = mAuth.getCurrentUser();
//                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//                        startActivity(intent);
//                        finish();
//                    } else {
//                        // If sign in fails, display a message to the user
//                        Log.w(TAG, "signInWithCredential:failure", task.getException());
////                        updateUI(null);
//                    }
//                });
//    }

//    private void signOut() {
//        // Firebase sign out
//        mAuth.signOut();
//
//        // When a user signs out, clear the current user credential state from all credential providers.
//        ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
//        credentialManager.clearCredentialStateAsync(
//                clearRequest,
//                new CancellationSignal(),
//                Executors.newSingleThreadExecutor(),
//                new CredentialManagerCallback<>() {
//                    @Override
//                    public void onResult(@NonNull Void result) {
//                        updateUI(null);
//                    }
//
//                    @Override
//                    public void onError(@NonNull ClearCredentialException e) {
//                        Log.e(TAG, "Couldn't clear user credentials: " + e.getLocalizedMessage());
//                    }
//                });
    }
}
