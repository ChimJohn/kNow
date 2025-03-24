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
    import androidx.annotation.Nullable;
    import androidx.fragment.app.Fragment;
    import androidx.fragment.app.FragmentTransaction;
    import androidx.lifecycle.ViewModelProvider;

    import com.google.android.gms.maps.CameraUpdateFactory;
    import com.google.android.gms.maps.GoogleMap;
    import com.google.android.gms.maps.MapView;
    import com.google.android.gms.maps.MapsInitializer;
    import com.google.android.gms.maps.OnMapReadyCallback;
    import com.google.android.gms.maps.model.BitmapDescriptorFactory;
    import com.google.android.gms.maps.model.Circle;
    import com.google.android.gms.maps.model.CircleOptions;
    import com.google.android.gms.maps.model.LatLng;
    import com.google.android.gms.maps.model.MapStyleOptions;
    import com.google.android.gms.maps.model.Marker;
    import com.google.android.gms.maps.model.MarkerOptions;
    import com.google.android.gms.maps.model.Polyline;
    import com.google.android.gms.maps.model.PolylineOptions;
    import com.google.firebase.firestore.DocumentChange;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.ListenerRegistration;
    import com.google.firebase.firestore.QuerySnapshot;
    import com.google.maps.android.PolyUtil;
    import com.google.maps.android.clustering.Cluster;
    import com.google.maps.android.clustering.ClusterManager;
    import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
    import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
    import com.prototypes.prototype.story.Story;
    import com.prototypes.prototype.story.StoryCluster;
    import com.prototypes.prototype.story.StoryClusterRenderer;
    import com.prototypes.prototype.story.StoryViewFragment;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

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
        private Map<String, StoryCluster> allMarkers = new HashMap<>();
        private Polyline routePolyline;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_explore, container, false);
        }
        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            Log.d("Debug", "Explore page.");
            mapView = view.findViewById(R.id.mapView);
            mapView.onCreate(savedInstanceState);
            mapView.onResume();
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
                    NonHierarchicalDistanceBasedAlgorithm<StoryCluster> algorithm = new NonHierarchicalDistanceBasedAlgorithm<>();
                    algorithm.setMaxDistanceBetweenClusteredItems(30); // Adjust clustering sensitivity
                    clusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<>(algorithm));
                    clusterManager.setOnClusterItemClickListener(storyCluster -> {
                        // Handle click event
                        Log.d("ClusterItemClicked", "Cluster item clicked: " + storyCluster.getTitle());
                        // Assuming you want to open a dialog when a cluster item is clicked
//                        StoryViewFragment dialogFragment = StoryViewFragment.newInstance(storyCluster,
//                                1
//                        );
//                        dialogFragment.show(getChildFragmentManager(), "story_view");
                        return true; // Return true to indicate that the event was consumed
                    });
                    clusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<>() {
                        @Override
                        public boolean onClusterClick(Cluster<StoryCluster> cluster) {
                            List<StoryCluster> clusterItems = new ArrayList<>(cluster.getItems());
                            ArrayList<Story> storyList = new ArrayList<>();
                            int position = 0;
                            for (StoryCluster storyCluster : clusterItems) {
                                storyList.add(new Story(storyCluster.getId(), storyCluster.getUserId(), storyCluster.getCaption(), storyCluster.getMediaUrl(), storyCluster.getPosition(), storyCluster.getMediaType()));
                            }
                            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                            transaction.replace(R.id.fragment_container, StoryViewFragment.newInstance(storyList, 0));
                            transaction.addToBackStack(null);
                            transaction.commit();
                            return true;
                        }
                    });
                    listenToMarkersData();
                }
            });
        }
        public void onPause() {
            super.onPause();
            mapView.onPause();
        }

        @Override
        public void onLowMemory() {
            super.onLowMemory();
            mapView.onLowMemory();
        }
        private void listenToMarkersData() {
            mediaListener = db.collection("media")
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.e("Firestore", "Listen failed.", e);
                            return;
                        }
                        Log.d("Firestore", "Updating markers...");
                        updateMarkers(snapshots);
                    });
        }
        private void updateMarkers(QuerySnapshot snapshots) {
            if (snapshots == null) return;
            for (DocumentChange change : snapshots.getDocumentChanges()) {
                DocumentSnapshot documentSnapshot = change.getDocument();
                String id = documentSnapshot.getId();
                String userId = documentSnapshot.getString("userId");
                String caption = documentSnapshot.getString("caption");
                String category = documentSnapshot.getString("category");
                String mediaUrl = documentSnapshot.getString("mediaUrl");
                String thumbnailUrl = documentSnapshot.getString("thumbnailUrl");
                String mediaType = documentSnapshot.getString("mediaType");
                Double latitude = documentSnapshot.getDouble("latitude");
                Double longitude = documentSnapshot.getDouble("longitude");
                if (latitude == null || longitude == null) continue;
                StoryCluster storyCluster = new StoryCluster(id, userId, latitude, longitude, caption, category, thumbnailUrl, mediaUrl, mediaType);
                switch (change.getType()) {
                    case ADDED:
                        clusterManager.addItem(storyCluster);
                        allMarkers.put(id, storyCluster);
                        break;
                    case MODIFIED:
                        removeMarkerById(id);
                        clusterManager.addItem(storyCluster);
                        allMarkers.put(id, storyCluster);
                        break;
                    case REMOVED:
                        removeMarkerById(id);
                        break;
                }
            }
            clusterManager.cluster(); // Refresh clusters
        }
        private void removeMarkerById(String id) {
            StoryCluster marker = allMarkers.remove(id); // Remove from HashMap
            if (marker != null) {
                clusterManager.removeItem(marker);
            }
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

        private void drawRoute(String encodedPolyline) {
            if (routePolyline != null) {
                routePolyline.remove();
            }
            List<LatLng> points = PolyUtil.decode(encodedPolyline);
            routePolyline = googleMap.addPolyline(new PolylineOptions().addAll(points).width(10).color(0xFF2196F3));
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
