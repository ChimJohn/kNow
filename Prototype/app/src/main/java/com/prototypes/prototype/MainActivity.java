package com.prototypes.prototype;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.prototypes.prototype.user.ProfileFragment;

import android.Manifest;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<String[]> multiplePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                    if (!entry.getValue()) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show();
                }
            });

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private CurrentLocationViewModel currentLocationViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        currentLocationViewModel = new ViewModelProvider(this).get(CurrentLocationViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestPermissions();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ExploreFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.explore) {
                selectedFragment = new ExploreFragment();
            } else if (item.getItemId() == R.id.upload) {
                selectedFragment = new TakePhotoFragment();
            } else if (item.getItemId() == R.id.profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }
    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                    if (!entry.getValue()) {
                        allGranted = false;
                        Log.e("Permissions", "Permission Denied: " + entry.getKey());
                    }
                }
                if (allGranted) {
                    Log.d("Permissions", "All permissions granted!");
                }
            });

    private void requestPermissions() {
        requestPermissionsLauncher.launch(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
    }
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permissions granted, start location tracking
        LocationRequest locationRequest = new LocationRequest.Builder(5000)  // Interval in milliseconds (10 seconds)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)  // Set priority for accuracy
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (!locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();
                    assert location != null;
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    currentLocationViewModel.setCurrentLocation(location);

                    Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start location updates
                startLocationUpdates();
            } else {
                // Permission denied, show message to user
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop location updates when activity is stopped
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
