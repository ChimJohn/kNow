package com.prototypes.prototype;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.prototypes.prototype.story.StoryCluster;
import com.prototypes.prototype.story.StoryClusterRenderer;

public class ExploreFragment extends Fragment {

    private MapView mapView;
    private GoogleMap googleMap;
    private CurrentLocationViewModel currentLocationViewModel;
    private Marker gpsMarker;
    private Circle pulsatingCircle;
    private ValueAnimator pulseAnimator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explore, container, false);
        Log.d("Debug", "Explore page.");

        mapView = rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        try {
            MapsInitializer.initialize(requireActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize ViewModel
        currentLocationViewModel = new ViewModelProvider(requireActivity()).get(CurrentLocationViewModel.class);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap map) {
                googleMap = map;
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.map_style));

                LatLng singapore = new LatLng(1.3521, 103.8198);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore, 12));

                googleMap.getUiSettings().setMapToolbarEnabled(false);
                googleMap.getUiSettings().setCompassEnabled(false);
                googleMap.getUiSettings().setRotateGesturesEnabled(false);
                googleMap.getUiSettings().setTiltGesturesEnabled(false);

                // Observe location updates from ViewModel
                currentLocationViewModel.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
                    if (location != null) {
                        updateGpsMarker(location);
                    }
                });
                ClusterManager<StoryCluster> clusterManager = new ClusterManager<>(requireContext(), map);
                clusterManager.setRenderer(new StoryClusterRenderer(requireContext(), map, clusterManager));
                map.setOnCameraIdleListener(clusterManager);
                map.setOnMarkerClickListener(clusterManager);
                clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<StoryCluster>() {
                    @Override
                    public boolean onClusterItemClick(StoryCluster storyCluster) {
                        // Handle the marker click
                        String title = storyCluster.getTitle();

                        // Example: Show a Toast message when the marker is clicked
                        Toast.makeText(requireActivity(), "Clicked: " + title, Toast.LENGTH_SHORT).show();

                        // Return true if you've handled the click, false otherwise
                        return true;
                    }
                });
                for (int i = 0; i < 20; i++) {
                    double lat = 1.3521 + (Math.random() * 0.1 - 0.05);
                    double lng = 103.8198 + (Math.random() * 0.1 - 0.05);
                    StoryCluster item = new StoryCluster(lat, lng, "Marker " + i, "Snippet " + i);
                    clusterManager.addItem(item);
                }

                clusterManager.cluster(); // Update clusters
            }
        });

        return rootView;
    }

    private void updateGpsMarker(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (gpsMarker == null) {
            gpsMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            gpsMarker.setPosition(latLng);
        }

        // Add pulsating effect
        if (pulsatingCircle == null) {
            pulsatingCircle = googleMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(50) // Initial radius in meters
                    .strokeWidth(0f)
                    .fillColor(Color.argb(70, 0, 0, 255)));

            startPulsatingEffect();
        } else {
            pulsatingCircle.setCenter(latLng);
        }

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
    }

    private void startPulsatingEffect() {
        pulseAnimator = ValueAnimator.ofFloat(50, 100);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            pulsatingCircle.setRadius(animatedValue);
        });
        pulseAnimator.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }

    private BitmapDescriptor createBitmapDescriptorFromTextView(TextView textView) {
        // Measure and layout the TextView
        textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());

        // Create a Bitmap from the TextView
        Bitmap bitmap = Bitmap.createBitmap(textView.getMeasuredWidth(), textView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        textView.draw(canvas);

        // Return a BitmapDescriptor from the Bitmap
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
