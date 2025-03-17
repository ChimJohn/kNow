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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;
import com.prototypes.prototype.story.StoryCluster;
import com.prototypes.prototype.story.StoryClusterRenderer;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;


public class ExploreFragment extends Fragment {

    private MapView mapView;
    private GoogleMap googleMap;
    private CurrentLocationViewModel currentLocationViewModel;
    private Marker gpsMarker;
    private Circle pulsatingCircle;
    private ValueAnimator pulseAnimator;
    private FirebaseFirestore db;
    private ClusterManager<StoryCluster> clusterManager;
    private boolean isFirstLocationUpdate = true;

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
        db = FirebaseFirestore.getInstance();

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap map) {

                googleMap = map;
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.map_style));

                LatLng singapore = new LatLng(1.3521, 103.8198);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore, 15));
                currentLocationViewModel.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
                    if (location != null) {
                        updateGpsMarker(location);
                        if (isFirstLocationUpdate) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));
                        }
                        isFirstLocationUpdate = false;
                    }
                });
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                googleMap.getUiSettings().setCompassEnabled(false);
                googleMap.getUiSettings().setRotateGesturesEnabled(false);
                googleMap.getUiSettings().setTiltGesturesEnabled(false);

                // Initialize the ClusterManager
                clusterManager = new ClusterManager<>(requireContext(), googleMap);
                clusterManager.setRenderer(new StoryClusterRenderer(requireContext(), googleMap, clusterManager));
                googleMap.setOnCameraIdleListener(clusterManager);
                googleMap.setOnMarkerClickListener(clusterManager);

                clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<StoryCluster>() {
                    @Override
                    public boolean onClusterItemClick(StoryCluster storyCluster) {
                        Log.d("ClusterItemClicked", "Cluster item clicked: " + storyCluster.getTitle());
                        // Create and show the story view dialog
                        StoryViewDialogFragment dialogFragment = StoryViewDialogFragment.newInstance(
                                storyCluster.getTitle(),
                                storyCluster.getSnippet(),
                                storyCluster.getImageUrl()
                        );
                        dialogFragment.show(getChildFragmentManager(), "story_view");

                        return true; // Consume the event
                        //return false;  // Allow normal click behavior
                    }
                });
                NonHierarchicalDistanceBasedAlgorithm<StoryCluster> algorithm = new NonHierarchicalDistanceBasedAlgorithm<>();
                algorithm.setMaxDistanceBetweenClusteredItems(30); // Adjust clustering sensitivity
                clusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<>(algorithm));
                // Observe location updates from ViewModel

                // Fetch the data from Firebase
                fetchMarkersData();
            }
        });
        return rootView;
    }

    private void fetchMarkersData() {
        db.collection("media")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot documentSnapshots = task.getResult();
                        if (documentSnapshots != null) {
                            for (QueryDocumentSnapshot documentSnapshot : documentSnapshots) {
                                String caption = documentSnapshot.getString("caption");
                                String category = documentSnapshot.getString("category");
                                String imageUrl = documentSnapshot.getString("imageUrl");
                                String thumbnailUrl = documentSnapshot.getString("thumbnailUrl");
                                double latitude = documentSnapshot.getDouble("latitude");
                                double longitude = documentSnapshot.getDouble("longitude");
                                // Create a StoryCluster for each document
                                StoryCluster storyCluster = new StoryCluster(latitude, longitude, caption, category, thumbnailUrl);
                                // Add the cluster item
                                clusterManager.addItem(storyCluster);
                            }
                        }
                    } else {
                        Log.e("ExploreFragment", "Error getting documents: ", task.getException());
                    }
                });
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
                    .radius(30) // Initial radius in meters
                    .strokeWidth(0f)
                    .fillColor(Color.argb(70, 0, 0, 255)));

            startPulsatingEffect();
        } else {
            pulsatingCircle.setCenter(latLng);
        }
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
}