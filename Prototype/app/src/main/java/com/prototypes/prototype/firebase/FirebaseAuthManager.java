package com.prototypes.prototype.firebase;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthManager {
    private FirebaseAuth mAuth;
    private Activity activity;
    private static final String TAG = "FirebaseAuthManager";

    public FirebaseAuthManager(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
    }

    // Register a new user with email and password
    public void registerUser(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "User registered: " + user.getUid());
                            callback.onSuccess(user);

                        } else {
                            Log.w(TAG, "User registration failed", task.getException());
                            callback.onFailure(task.getException());
                        }
                    }
                });
    }

    // Sign in user with email and password
    public void loginUser(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "User logged in: " + user.getEmail());
                            callback.onSuccess(user);
                        } else {
                            Log.w(TAG, "User login failed", task.getException());
                            callback.onFailure(task.getException());
                        }
                    }
                });
    }

    // Sign out the current user
    public void logoutUser() {
        mAuth.signOut();
        Log.d(TAG, "User signed out");
    }

    // Check if a user is currently signed in
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    // Interface for authentication callbacks
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }
}
