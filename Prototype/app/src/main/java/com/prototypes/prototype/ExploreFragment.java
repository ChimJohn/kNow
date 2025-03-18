    package com.prototypes.prototype;

    import android.animation.ValueAnimator;
    import android.graphics.Color;
    import android.location.Location;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;

    import androidx.annotation.NonNull;
    import androidx.fragment.app.Fragment;
    import androidx.lifecycle.ViewModelProvider;

    import com.google.android.gms.maps.GoogleMap;
    import com.google.android.gms.maps.OnMapReadyCallback;
    import com.google.android.gms.maps.MapView;
    import com.google.android.gms.maps.MapsInitializer;
    import com.google.android.gms.maps.CameraUpdateFactory;
    import com.google.android.gms.maps.model.BitmapDescriptorFactory;
    import com.google.android.gms.maps.model.Circle;
    import com.google.android.gms.maps.model.CircleOptions;
    import com.google.android.gms.maps.model.LatLng;
    import com.google.android.gms.maps.model.MapStyleOptions;
    import com.google.android.gms.maps.model.Marker;
    import com.google.android.gms.maps.model.MarkerOptions;
    import com.google.android.material.chip.Chip;
    import com.google.android.material.chip.ChipGroup;
    import com.google.firebase.firestore.EventListener;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.ListenerRegistration;
    import com.google.firebase.firestore.QueryDocumentSnapshot;
    import com.google.firebase.firestore.QuerySnapshot;
    import com.google.firebase.firestore.FirebaseFirestoreException;
    import com.google.maps.android.clustering.ClusterManager;
    import com.prototypes.prototype.story.StoryCluster;
    import com.prototypes.prototype.story.StoryClusterRenderer;
    import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
    import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;

    import java.util.ArrayList;
    import java.util.List;

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

        private ListenerRegistration mediaListener;  // Firestore listener reference
        private static final long UPDATE_INTERVAL = 10000; // 20 seconds
        private long lastUpdateTime = 0;
        private List<StoryCluster> allMarkers = new ArrayList<>();
        ChipGroup chipGroupFilters;
        Chip chipFood, chipAttraction;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_explore, container, false);
            Log.d("Debug", "Explore page.");
            mapView = rootView.findViewById(R.id.mapView);
            mapView.onCreate(savedInstanceState);
            mapView.onResume();
            chipGroupFilters = rootView.findViewById(R.id.chipGroupFilters);
            chipFood = rootView.findViewById(R.id.chipFood);
            chipAttraction = rootView.findViewById(R.id.chipAttraction);
            chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilters());
            try {
                MapsInitializer.initialize(requireActivity().getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentLocationViewModel = new ViewModelProvider(requireActivity()).get(CurrentLocationViewModel.class);
            db = FirebaseFirestore.getInstance();
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull GoogleMap map) {
                    googleMap = map;
                    googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.map_style));
                    LatLng singapore = new LatLng(1.3521, 103.8198);
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore, 20));
                    currentLocationViewModel.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
                        if (location != null) {
                            updateGpsMarker(location);
                            if (isFirstLocationUpdate) {
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 20));
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
                    clusterManager.setOnClusterItemClickListener(storyCluster -> {
                        Log.d("ClusterItemClicked", "Cluster item clicked: " + storyCluster.getTitle());
                        StoryViewDialogFragment dialogFragment = StoryViewDialogFragment.newInstance(
                                storyCluster.getTitle(),
                                storyCluster.getSnippet(),
                                storyCluster.getImageUrl()
                        );
                        dialogFragment.show(getChildFragmentManager(), "story_view");
                        return true; // Consume the event
                    });
                    NonHierarchicalDistanceBasedAlgorithm<StoryCluster> algorithm = new NonHierarchicalDistanceBasedAlgorithm<>();
                    algorithm.setMaxDistanceBetweenClusteredItems(30); // Adjust clustering sensitivity
                    clusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<>(algorithm));
                    // Listen for real-time updates
                    listenToMarkersData();
                }
            });
            return rootView;
        }

        private void listenToMarkersData() {
            mediaListener = db.collection("media")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                            if (e  != null) {
                                Log.e("Firestore", "Listen failed.", e);
                                return;
                            }
                            long currentTime = System.currentTimeMillis();
                            if (snapshots != null && (currentTime - lastUpdateTime >= UPDATE_INTERVAL)) {
                                lastUpdateTime = currentTime; // Update last update time
                                Log.d("Firestore", "Ok its updating.");
                                updateMarkers(snapshots);
                            } else {
                                Log.d("Firestore", "Update ignored to prevent frequent updates.");
                            }
                        }
                    });
        }

        private void updateMarkers(QuerySnapshot snapshots) {
            clusterManager.clearItems(); // Clear old markers
            Log.d("HAH", "updating markers");
            for (QueryDocumentSnapshot documentSnapshot : snapshots) {
                String caption = documentSnapshot.getString("caption");
                String category = documentSnapshot.getString("category");
                String imageUrl = documentSnapshot.getString("imageUrl");
                String thumbnailUrl = documentSnapshot.getString("thumbnailUrl");
                double latitude = documentSnapshot.getDouble("latitude");
                double longitude = documentSnapshot.getDouble("longitude");
                StoryCluster storyCluster = new StoryCluster(latitude, longitude, caption, category, thumbnailUrl);
                clusterManager.addItem(storyCluster);
                allMarkers.add(storyCluster);
            }
            applyFilters();
        }
        private void applyFilters() {
            if (clusterManager == null) return;
            clusterManager.clearItems(); // Clear the map markers
            List<String> selectedCategories = new ArrayList<>();
            if (chipFood.isChecked()) selectedCategories.add("Food");
            if (chipAttraction.isChecked()) selectedCategories.add("Attractions");

            if (selectedCategories.isEmpty()) {
                // No filters selected, show everything
                clusterManager.addItems(allMarkers);
            } else {
                // Filter markers based on selected categories
                for (StoryCluster story : allMarkers) {
                    if (selectedCategories.contains(story.getCategory())) {
                        clusterManager.addItem(story);
                    }
                }
            }
            clusterManager.cluster(); // Refresh map clusters
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

            if (pulsatingCircle == null) {
                pulsatingCircle = googleMap.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(1)
                        .strokeWidth(0f)
                        .fillColor(Color.argb(70, 0, 0, 255)));
                startPulsatingEffect();
            } else {
                pulsatingCircle.setCenter(latLng);
            }
        }

        private void startPulsatingEffect() {
            pulseAnimator = ValueAnimator.ofFloat(10, 20 );
            pulseAnimator.setDuration(1000);
            pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
            pulseAnimator.addUpdateListener(animation -> pulsatingCircle.setRadius((float) animation.getAnimatedValue()));
            pulseAnimator.start();
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (mediaListener != null) {
                mediaListener.remove();
                mediaListener = null;
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
