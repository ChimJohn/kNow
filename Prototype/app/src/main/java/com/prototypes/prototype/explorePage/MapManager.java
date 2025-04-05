package com.prototypes.prototype.explorePage;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
import com.prototypes.prototype.R;
import com.prototypes.prototype.story.Story;
import com.prototypes.prototype.story.StoryCluster;
import com.prototypes.prototype.story.StoryClusterRenderer;
import com.prototypes.prototype.story.StoryViewFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MapManager {
    private final Context context;
    private final Activity activity;
    private GoogleMap googleMap;
    private ClusterManager<StoryCluster> clusterManager;
    private final Map<String, StoryCluster> allMarkers = new HashMap<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration mediaListener;
    private RouteHandler routeHandler;
    private CurrentLocationMarker currentLocationMarker;
    FragmentManager parentFragmentManager;
    private boolean isFirstLocationUpdate = true;
    AtomicLong lastRouteFetchTime = new AtomicLong(); // Stores last fetch timestamp

    public MapManager(Activity activity, Context context, FragmentManager parentFragmentManager) {
        this.activity = activity;
        this.context = context;
        this.parentFragmentManager = parentFragmentManager;
    }
    public void initMap(GoogleMap map){
        this.googleMap = map;
        this.routeHandler = new RouteHandler(context, map);
        this.currentLocationMarker = new CurrentLocationMarker(activity, map);
        setupMap();
    }
    public GoogleMap getMap(){
        return this.googleMap;
    }
    public RouteHandler getRouteHandler(){
        return this.routeHandler;
    }
    public CurrentLocationMarker getCurrentLocationMarker(){
        return this.currentLocationMarker;
    }
    public void animateCamera(LatLng latLng, Integer zoom){
        if (this.googleMap != null) {
            this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        }
    }
    public void setupMap(){
        LatLng singapore = new LatLng(1.3521, 103.8198);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore, 20));
        this.googleMap.getUiSettings().setMapToolbarEnabled(false);
        this.googleMap.getUiSettings().setCompassEnabled(false);
        this.googleMap.getUiSettings().setRotateGesturesEnabled(false);
        this.googleMap.getUiSettings().setTiltGesturesEnabled(false);
        this.clusterManager = new ClusterManager<>(context, this.googleMap);
        this.clusterManager.setRenderer(new StoryClusterRenderer(context, this.googleMap, this.clusterManager));
        this.googleMap.setOnCameraIdleListener(this.clusterManager);
        NonHierarchicalDistanceBasedAlgorithm<StoryCluster> algorithm = new NonHierarchicalDistanceBasedAlgorithm<>();
        algorithm.setMaxDistanceBetweenClusteredItems(30);
        this.clusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<>(algorithm));
        this.clusterManager.setOnClusterItemClickListener(storyCluster -> {
            Story story = new Story(
                    storyCluster.getId(),
                    storyCluster.getUserId(),
                    storyCluster.getCaption(),
                    storyCluster.getCategory(),
                    storyCluster.getMediaUrl(),
                    storyCluster.getLatitude(),
                    storyCluster.getLongitude(),
                    storyCluster.getMediaType()
            );
            ArrayList<Story> storyList = new ArrayList<>();
            storyList.add(story);
            FragmentTransaction transaction = parentFragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, StoryViewFragment.newInstance(storyList));
            transaction.addToBackStack(null);
            transaction.commit();
            if (activity != null) {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigationView);
                if (bottomNav != null) {
                    bottomNav.setVisibility(View.GONE);
                }
            }
            return true;
        });

        clusterManager.setOnClusterClickListener(cluster -> {
            List<StoryCluster> clusterItems = new ArrayList<>(cluster.getItems());
            ArrayList<Story> storyList = new ArrayList<>();
            for (StoryCluster storyCluster : clusterItems) {
                storyList.add(new Story(storyCluster.getId(), storyCluster.getUserId(), storyCluster.getCaption(), storyCluster.getCategory() ,storyCluster.getMediaUrl(), storyCluster.getLatitude(), storyCluster.getLongitude(), storyCluster.getMediaType()));
            }
            FragmentTransaction transaction = parentFragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, StoryViewFragment.newInstance(storyList));
            transaction.addToBackStack(null);
            transaction.commit();
            if (activity != null) {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigationView);
                if (bottomNav != null) {
                    bottomNav.setVisibility(View.GONE);
                }
            }
            return true;
        });
    }
    public void setOwnMarkerLocation(Location location){
        if (location != null) {
            getCurrentLocationMarker().updateGpsMarker(location);
            if (isFirstLocationUpdate) {
                animateCamera(new LatLng(location.getLatitude(), location.getLongitude()), 20);
            }
            isFirstLocationUpdate = false;
        }
    }

    public void routeToDestination(Location location, Double destinationLatitude, Double destinationLongitude){
        long currentTime = System.currentTimeMillis();
        LatLng latLng = new LatLng(destinationLatitude, destinationLongitude);
        if (currentTime - lastRouteFetchTime.get() >= 30000) { // 30 Seconds
            getRouteHandler().fetchRoute(location, latLng);
            lastRouteFetchTime.set(currentTime);
        } else {
            Log.d("routeToDestination", "Skipping fetch, waiting for next interval.");
        }
    }

    private void removeMarker(String id) {
        StoryCluster existing = allMarkers.remove(id);
        if (existing != null) {
            clusterManager.removeItem(existing);
        }
    }


    public void clear() {
        if (this.mediaListener != null) {
            this.mediaListener.remove();
            this.mediaListener = null;
        }
        if (this.googleMap != null){
            this.googleMap.setOnMapClickListener(null);
            this.googleMap.setOnMarkerClickListener(null);
            this.googleMap.setOnCameraIdleListener(null);
            this.googleMap.clear();
            this.googleMap = null;
        }
        if (this.clusterManager != null){
            this.clusterManager.clearItems();
        }
        if (this.currentLocationMarker != null){
            this.currentLocationMarker.stopPulsatingEffect();
        }
        this.allMarkers.clear();
    }

    public void listenToStoryMarkersData() {
        this.mediaListener = db.collection("media")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("listenToStoryMarkersData()", "Listen failed.", e);
                        return;
                    }
                    if (snapshots == null) {
                        Log.e("listenToStoryMarkersData()", "No snapshots");
                        return;
                    };
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
                    clusterManager.cluster();
                });
    }
    private void removeMarkerById(String id) {
        StoryCluster marker = allMarkers.remove(id);
        if (marker != null) {
            clusterManager.removeItem(marker);
        }
    }

}
