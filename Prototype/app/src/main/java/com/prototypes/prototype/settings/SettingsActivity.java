package com.prototypes.prototype.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.prototypes.prototype.R;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.login.LoginActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private FirebaseAuthManager authManager;
    TextView tvLogout, tvDeleteAccount;
    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // Set up Firebase auth
        authManager = new FirebaseAuthManager(this);

        // Set up Toolbar with back arrow and title
        toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings and activity"); // Optional: Ensure title appears
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);   // Show back arrow
            getSupportActionBar().setDisplayShowHomeEnabled(true);   // Ensure click works
        }

        toolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish(); // Return to ProfileFragment
        });

        // Handle Notifications Click
        TextView tvNotifications = findViewById(R.id.tvNotifications);
        tvNotifications.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Enable Notifications")
                    .setMessage("Do you want to enable notifications?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                        // Here you could request permissions or toggle internal settings
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // Handle Blocked Click
        TextView tvBlocked = findViewById(R.id.tvBlocked);
        tvBlocked.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, BlockedUsersActivity.class);
            startActivity(intent);
            Log.d(TAG, "Navigated to BlockedUsersActivity.");
        });

        // Handle Logout Click
        tvLogout = findViewById(R.id.tvLogout);
        tvLogout.setOnClickListener(v -> {
            authManager.logoutUser();
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();

            // Clear back stack and return to login screen
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Log.d(TAG, "Navigated to login screen after logout.");
        });

        // Handle Delete Account Click
        tvDeleteAccount = findViewById(R.id.tvDeleteAccount);
        tvDeleteAccount.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        authManager.deleteUser(SettingsActivity.this, () -> {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                Log.d(TAG, "Navigated to login screen after account deletion.");
                            });
                        });
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        });


    }
}
