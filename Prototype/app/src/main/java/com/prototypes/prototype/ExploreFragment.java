package com.prototypes.prototype;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
import com.prototypes.prototype.story.Story;
import com.prototypes.prototype.story.StoryCluster;
import com.prototypes.prototype.story.StoryClusterRenderer;
import com.prototypes.prototype.story.StoryViewFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExploreFragment extends Fragment {

    private RecyclerView rvUserSearchResults;
    private UserSearchAdapter userSearchAdapter;

    private MapView mapView;
    private GoogleMap googleMap;
    private CurrentLocationViewModel currentLocationViewModel;
    private Marker gpsMarker;
    private Circle pulsatingCircle;
    private ValueAnimator pulseAnimator;
    private FirebaseFirestore db;
    private ClusterManager<StoryCluster> clusterManager;
    private boolean isFirstLocationUpdate = true;
    private ListenerRegistration mediaListener;
    private Map<String, StoryCluster> allMarkers = new HashMap<>();
    private Polyline routePolyline;
    private PlacesClient placesClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(requireContext());
        db = FirebaseFirestore.getInstance();
        currentLocationViewModel = new ViewModelProvider(requireActivity()).get(CurrentLocationViewModel.class);

        mapView.getMapAsync(map -> {
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

            clusterManager = new ClusterManager<>(requireContext(), googleMap);
            clusterManager.setRenderer(new StoryClusterRenderer(requireContext(), googleMap, clusterManager));
            googleMap.setOnCameraIdleListener(clusterManager);
            googleMap.setOnMarkerClickListener(clusterManager);
            NonHierarchicalDistanceBasedAlgorithm<StoryCluster> algorithm = new NonHierarchicalDistanceBasedAlgorithm<>();
            algorithm.setMaxDistanceBetweenClusteredItems(30);
            clusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<>(algorithm));

            clusterManager.setOnClusterItemClickListener(storyCluster -> true);
            clusterManager.setOnClusterClickListener(cluster -> {
                List<StoryCluster> clusterItems = new ArrayList<>(cluster.getItems());
                ArrayList<Story> storyList = new ArrayList<>();
                for (StoryCluster storyCluster : clusterItems) {
                    storyList.add(new Story(storyCluster.getId(), storyCluster.getUserId(), storyCluster.getCaption(), storyCluster.getMediaUrl(), storyCluster.getPosition(), storyCluster.getMediaType()));
                }
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, StoryViewFragment.newInstance(storyList, 0));
                transaction.addToBackStack(null);
                transaction.commit();
                return true;
            });

            listenToMarkersData();
        });

        EditText etSearch = view.findViewById(R.id.etSearch);
        rvUserSearchResults = view.findViewById(R.id.rvSearchResults);
        rvUserSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        userSearchAdapter = new UserSearchAdapter(new ArrayList<>(), username -> {
            Toast.makeText(getContext(), "Clicked on " + username, Toast.LENGTH_SHORT).show();
        });
        rvUserSearchResults.setAdapter(userSearchAdapter);

        // Near me button
        View locationButton = view.findViewById(R.id.floatingActionButton3);
        locationButton.setOnClickListener(v -> {
            Location currentLocation = currentLocationViewModel.getCurrentLocation().getValue();
            if (googleMap != null && currentLocation != null) {
                LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
            } else {
                Toast.makeText(requireContext(), "Current location unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();

                if (query.startsWith("@")) {
                    String usernameQuery = query.substring(1).toLowerCase();

                    db.collection("Users")
                            .orderBy("username")
                            .startAt(usernameQuery)
                            .endAt(usernameQuery + "\uf8ff")
                            .limit(10)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                List<String> matched = new ArrayList<>();
                                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                    String username = doc.getString("username");
                                    if (username != null) matched.add(username);
                                }
                                userSearchAdapter.updateData(matched);
                                rvUserSearchResults.setVisibility(matched.isEmpty() ? View.GONE : View.VISIBLE);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("UserSearch", "Error fetching users", e);
                                rvUserSearchResults.setVisibility(View.GONE);
                            });
                } else if (!query.isEmpty()) {
                    RectangularBounds bounds = RectangularBounds.newInstance(
                            new LatLng(1.1304753, 103.6920359),
                            new LatLng(1.4504753, 104.0120359)
                    );

                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(query)
                            .setLocationBias(bounds)
                            .setTypeFilter(TypeFilter.ADDRESS)
                            .build();

                    placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener(response -> {
                                List<String> locationNames = new ArrayList<>();
                                List<String> locationPlaceIds = new ArrayList<>();

                                for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                                    locationNames.add(prediction.getPrimaryText(null).toString());
                                    locationPlaceIds.add(prediction.getPlaceId());
                                }

                                // Set new adapter with location click handling
                                userSearchAdapter = new UserSearchAdapter(locationNames, selectedName -> {
                                    int index = locationNames.indexOf(selectedName);
                                    if (index != -1) {
                                        String placeId = locationPlaceIds.get(index);
                                        fetchAndZoomToPlace(placeId);
                                    }
                                });
                                rvUserSearchResults.setAdapter(userSearchAdapter);
                                rvUserSearchResults.setVisibility(locationNames.isEmpty() ? View.GONE : View.VISIBLE);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("PlaceSearch", "Places API failed", e);
                                rvUserSearchResults.setVisibility(View.GONE);
                            });

                } else {
                    rvUserSearchResults.setVisibility(View.GONE);
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void listenToMarkersData() {
        mediaListener = db.collection("media")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Listen failed.", e);
                        return;
                    }
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
        clusterManager.cluster();
    }

    private void removeMarkerById(String id) {
        StoryCluster marker = allMarkers.remove(id);
        if (marker != null) {
            clusterManager.removeItem(marker);
        }
    }

    private void updateGpsMarker(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        /*
        if (gpsMarker == null) {
            gpsMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            gpsMarker.setPosition(latLng);
        }
         */
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

    private void fetchAndZoomToPlace(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);

        placesClient.fetchPlace(
                com.google.android.libraries.places.api.net.FetchPlaceRequest.builder(placeId, placeFields).build()
        ).addOnSuccessListener(fetchPlaceResponse -> {
            Place place = fetchPlaceResponse.getPlace();
            LatLng latLng = place.getLatLng();
            if (latLng != null && googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
            }
        }).addOnFailureListener(e -> {
            Log.e("PlaceZoom", "Failed to fetch place", e);
        });
    }


    private void startPulsatingEffect() {
        pulseAnimator = ValueAnimator.ofFloat(10, 20);
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
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
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