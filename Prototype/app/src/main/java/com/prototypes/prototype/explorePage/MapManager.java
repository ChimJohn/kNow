package com.prototypes.prototype.explorePage;

import android.app.Activity;
import android.content.Context;
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

public class MapManager {
    private final Context context;
    private final Activity activity;
    private final GoogleMap googleMap;
    private ClusterManager<StoryCluster> clusterManager;
    private final Map<String, StoryCluster> allMarkers = new HashMap<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration mediaListener;
    private RouteHandler routeHandler;
    FragmentManager parentFragmentManager;


    public MapManager(Activity activity, Context context, FragmentManager parentFragmentManager, GoogleMap map) {
        this.activity = activity;
        this.context = context;
        this.parentFragmentManager = parentFragmentManager;
        this.googleMap = map;
    }

    public void setupMap(){
        LatLng singapore = new LatLng(1.3521, 103.8198);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore, 20));
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);
        clusterManager = new ClusterManager<>(context, googleMap);
        clusterManager.setRenderer(new StoryClusterRenderer(context, googleMap, clusterManager));
        googleMap.setOnCameraIdleListener(clusterManager);
        NonHierarchicalDistanceBasedAlgorithm<StoryCluster> algorithm = new NonHierarchicalDistanceBasedAlgorithm<>();
        algorithm.setMaxDistanceBetweenClusteredItems(30);
        clusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<>(algorithm));

        clusterManager.setOnClusterItemClickListener(storyCluster -> {
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

    private void removeMarker(String id) {
        StoryCluster existing = allMarkers.remove(id);
        if (existing != null) {
            clusterManager.removeItem(existing);
        }
    }


    public void clear() {
        if (mediaListener != null) {
            mediaListener.remove();
            mediaListener = null;
        }
        clusterManager.clearItems();
        allMarkers.clear();
    }

    public void listenToMarkersData() {
        mediaListener = db.collection("media")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("listenToMarkersData()", "Listen failed.", e);
                        return;
                    }
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
