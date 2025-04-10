package com.prototypes.prototype.explorePage;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
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
import com.prototypes.prototype.classes.Story;
import com.prototypes.prototype.storyView.StoryViewFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MapManager {
    private final Context context;
    private final Activity activity;
    private GoogleMap googleMap;
    private ClusterManager<RouteHandler.StoryCluster> clusterManager;
    private final Map<String, RouteHandler.StoryCluster> allMarkers = new HashMap<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration mediaListener;
    private RouteHandler routeHandler;
    private CurrentLocationMarker currentLocationMarker;
    FragmentManager parentFragmentManager;
    private boolean isFirstLocationUpdate = true;
    AtomicLong lastRouteFetchTime = new AtomicLong(); // Stores last fetch timestamp
    private final ArrayList<RouteHandler.StoryCluster> allStoryClusters = new ArrayList<>();

    public MapManager(Activity activity, Context context, FragmentManager parentFragmentManager) {
        this.activity = activity;
        this.context = context;
        this.parentFragmentManager = parentFragmentManager;
    }
    public void initMap(GoogleMap map){
        this.googleMap = map;
        this.routeHandler = new RouteHandler(context, map);
        this.currentLocationMarker = new CurrentLocationMarker(activity, map);
        LatLng singapore = new LatLng(1.3521, 103.8198);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore, 12));
        this.googleMap.getUiSettings().setMapToolbarEnabled(false);
        this.googleMap.getUiSettings().setCompassEnabled(false);
        this.googleMap.getUiSettings().setRotateGesturesEnabled(false);
        this.googleMap.getUiSettings().setTiltGesturesEnabled(false);
        this.clusterManager = new ClusterManager<>(context, this.googleMap);
        this.clusterManager.setRenderer(new MarkerHandler.StoryClusterRenderer(context, this.googleMap, this.clusterManager));
        this.googleMap.setOnCameraIdleListener(this.clusterManager);
        NonHierarchicalDistanceBasedAlgorithm<RouteHandler.StoryCluster> algorithm = new NonHierarchicalDistanceBasedAlgorithm<>();
        algorithm.setMaxDistanceBetweenClusteredItems(30);
        this.clusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<>(algorithm));
        this.clusterManager.setOnClusterItemClickListener(storyCluster -> {
            ArrayList<Story> storyList = new ArrayList<>();
            storyList.add(storyCluster);
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
            ArrayList<Story> storyList = new ArrayList<>(cluster.getItems());
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
        });    }
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
    public void setOwnMarkerLocation(Location location){
        if (location != null) {
            getCurrentLocationMarker().updateGpsMarker(location);
            if (isFirstLocationUpdate) {
                animateCamera(new LatLng(location.getLatitude(), location.getLongitude()), 16);
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
        RouteHandler.StoryCluster existing = allMarkers.remove(id);
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
                        Log.e("listenToStoryMarkersData", "Listen failed.", e);
                        return;
                    }
                    if (snapshots == null) {
                        Log.e("listenToStoryMarkersData", "No snapshots");
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
                        RouteHandler.StoryCluster storyCluster = new RouteHandler.StoryCluster(id, userId, latitude, longitude, caption, category, thumbnailUrl, mediaUrl, mediaType);
                        switch (change.getType()) {
                            case ADDED:
                                allStoryClusters.add(storyCluster);
                                allMarkers.put(id, storyCluster);
                                break;
                            case MODIFIED:
                                removeMarkerById(id);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    allStoryClusters.removeIf(marker -> marker.getId().equals(id));
                                }
                                allStoryClusters.add(storyCluster);
                                allMarkers.put(id, storyCluster);
                                break;
                            case REMOVED:
                                removeMarkerById(id);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    allStoryClusters.removeIf(marker -> marker.getId().equals(id));
                                }
                                break;
                        }
                    }
                    clusterManager.cluster();
                    filterMarkers(null);
                });
    }
    public void filterMarkers(@Nullable List<String> filters) {
        clusterManager.clearItems();
        if (filters == null || filters.isEmpty()) {
            clusterManager.addItems(allMarkers.values());
        } else {
            for (RouteHandler.StoryCluster marker : allMarkers.values()) {
                if (filters.contains(marker.getCategory())) {
                    clusterManager.addItem(marker);
                }
            }
        }
        clusterManager.cluster();
    }
    private void removeMarkerById(String id) {
        RouteHandler.StoryCluster marker = allMarkers.remove(id);
        if (marker != null) {
            clusterManager.removeItem(marker);
        }
    }

    public static class StoryMarker extends LinearLayout {
        public StoryMarker(Context context) {
            super(context);
            init(context);
        }
        public StoryMarker(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            init(context);
        }
        private void init(Context context) {
            LayoutInflater.from(context).inflate(R.layout.story_marker, this, true);
        }
        public void setMarkerImage(Context context, String imageUrl, CustomTarget<Bitmap> target) {
            Glide.with(context)
                    .asBitmap()
                    .override(250, 250) // Resize the image to fit marker
                    .load(imageUrl)
                    .into(target);
        }
    }
}
