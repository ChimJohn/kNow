package com.prototypes.prototype.firebase;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseAuthManager {
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore firestore;
    private final Activity activity;
    private static final String TAG = "FirebaseAuthManager";

    public FirebaseAuthManager(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    // Register a new user with email and password
    public void registerUser(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "User registered: " + (user != null ? user.getUid() : "null"));
                        callback.onSuccess(user);
                    } else {
                        Log.w(TAG, "User registration failed", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }

    // Sign in user with email and password
    public void loginUser(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "User logged in: " + (user != null ? user.getEmail() : "null"));
                        callback.onSuccess(user);
                    } else {
                        Log.w(TAG, "User login failed", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }

    // Sign out the current user
    public void logoutUser() {
        mAuth.signOut();
        Log.d(TAG, "User signed out");
    }

    // Delete the current user from Firebase Auth + Firestore
    public void deleteUser(Activity context, Runnable onSuccess) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "No user signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        firestore.collection("Users").document(uid)
                .delete()
                .addOnSuccessListener(unused -> {
                    currentUser.delete()
                            .addOnSuccessListener(unused1 -> {
                                Log.d(TAG, "User deleted successfully.");
                                onSuccess.run();  // Trigger callback
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete Firebase user", e);
                                Toast.makeText(context, "Failed to delete user from Auth", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete Firestore document", e);
                    Toast.makeText(context, "Failed to delete user data", Toast.LENGTH_SHORT).show();
                });
    }


    // Get the current signed-in user
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    // Interface for authentication callbacks
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }
}
