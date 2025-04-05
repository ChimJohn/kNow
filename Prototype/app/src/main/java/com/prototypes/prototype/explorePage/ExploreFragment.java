package com.prototypes.prototype.explorePage;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.maps.android.clustering.ClusterManager;
import com.prototypes.prototype.CurrentLocationViewModel;
import com.prototypes.prototype.R;
import com.prototypes.prototype.UserSearchAdapter;
import com.prototypes.prototype.story.StoryCluster;
import com.prototypes.prototype.user.UserProfileFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ExploreFragment extends Fragment {
    private RecyclerView rvUserSearchResults;
    private UserSearchAdapter userSearchAdapter;
    private MapView mapView;
    private GoogleMap googleMap;
    private CurrentLocationViewModel currentLocationViewModel;
    private Double latitude, longitude;
    private FirebaseFirestore db;
    private ClusterManager<StoryCluster> clusterManager;
    private boolean isFirstLocationUpdate = true;
    private ListenerRegistration mediaListener;
    private final Map<String, StoryCluster> allMarkers = new HashMap<>();
    private PlacesClient placesClient;
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private RouteHandler routeHandler;
    private CurrentLocationMarker currentLocationMarker;
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private float currentBearing = 0f;
    private boolean suppressSearch = false;
    private MapManager mapManager;

    public static ExploreFragment newInstance(double latitude, double longitude) {
        ExploreFragment fragment = new ExploreFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
        }
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

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
            mapManager = new MapManager(requireActivity(), requireContext(), getParentFragmentManager(), googleMap);
            mapManager.setupMap();
            mapManager.listenToMarkersData();
            routeHandler = new RouteHandler(getContext(), googleMap);
            currentLocationMarker = new CurrentLocationMarker(requireActivity(), googleMap);
            final long FETCH_INTERVAL = 30000; // 30 seconds
            AtomicLong lastFetchTime = new AtomicLong(); // Stores last fetch timestamp
            currentLocationViewModel.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
                if (location != null) {
                    currentLocationMarker.updateGpsMarker(location);
                    if (isFirstLocationUpdate) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 20));
                    }
                    isFirstLocationUpdate = false;

                    if (latitude != null && longitude != null){
                        long currentTime = System.currentTimeMillis(); // Get current time
                        LatLng latLng = new LatLng(latitude, longitude);
                        if (currentTime - lastFetchTime.get() >= FETCH_INTERVAL) {
                            routeHandler.fetchRoute(location, latLng); // Call fetch function
                            lastFetchTime.set(currentTime); // Update last fetch time
                            Log.d("API_CALL", "Fetching route at: " + currentTime);
                        } else {
                            Log.d("API_CALL", "Skipping fetch, waiting for next interval.");
                        }
                    }
                }
            });
        });

        EditText etSearch = view.findViewById(R.id.etSearch);
        rvUserSearchResults = view.findViewById(R.id.rvSearchResults);
        rvUserSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        userSearchAdapter = new UserSearchAdapter(new ArrayList<>(), this::handleUserClick);
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
                if (suppressSearch) return;

                String query = s.toString().trim();
                if (query.startsWith("@")) {
                    String usernameQuery = query.substring(1).toLowerCase();
                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    db.collection("Users")
                            .orderBy("username")
                            .startAt(usernameQuery)
                            .endAt(usernameQuery + "\uf8ff")
                            .limit(10)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                List<UserWithFollowers> matchedUsers = new ArrayList<>();
                                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                    if (doc.getId().equals(currentUserId)) continue; // Skip current user

                                    String username = doc.getString("username");
                                    List<?> followersList = (List<?>) doc.get("followers");
                                    int followerCount = (followersList != null) ? followersList.size() : 0;

                                    if (username != null) {
                                        matchedUsers.add(new UserWithFollowers(username, followerCount));
                                    }
                                }

                                List<UserWithFollowers> sortedUsers = mergeSort(matchedUsers);
                                List<String> sortedUsernames = new ArrayList<>();
                                for (UserWithFollowers user : sortedUsers) {
                                    sortedUsernames.add(user.username);
                                }

                                userSearchAdapter.updateData(sortedUsernames);
                                rvUserSearchResults.setVisibility(sortedUsernames.isEmpty() ? View.GONE : View.VISIBLE);
                            });

                } else if (!query.isEmpty()) {
                    RectangularBounds bounds = RectangularBounds.newInstance(
                            new LatLng(1.1304753, 103.6920359),
                            new LatLng(1.4504753, 104.0120359)
                    );
                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(query)
                            .setLocationRestriction(bounds)
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
                                userSearchAdapter.updateData(locationNames, selectedName -> {
                                    int index = locationNames.indexOf(selectedName);
                                    if (index != -1) {
                                        String placeId = locationPlaceIds.get(index);

                                        // Hide keyboard
                                        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

                                        // Prevent TextWatcher logic from being triggered
                                        suppressSearch = true;
                                        etSearch.setText(selectedName);
                                        etSearch.clearFocus();
                                        suppressSearch = false;

                                        rvUserSearchResults.setVisibility(View.GONE);
                                        fetchAndZoomToPlace(placeId);
                                    }
                                });

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
    @Override
    public void onResume() {
        super.onResume();
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] rotationMatrix = new float[9];
            float[] orientationAngles = new float[3];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            currentBearing = (float) Math.toDegrees(orientationAngles[0]);
            currentBearing = (currentBearing + 360) % 360;
            if (currentLocationMarker != null && currentLocationMarker.getGpsMarker() != null) {
                currentLocationMarker.getGpsMarker().setRotation(currentBearing);
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void fetchAndZoomToPlace(String placeId) {
        List<Place.Field> placeFields = List.of(Place.Field.LAT_LNG);
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

    private void handleUserClick(String username) {
        // Query Firestore to get the user ID for this username
        db.collection("Users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String userId = document.getId();

                        // Navigate to the user's profile
                        UserProfileFragment userProfileFragment = UserProfileFragment.newInstance(userId);
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, userProfileFragment)
                                .addToBackStack(null)
                                .commit();

                        // Clear search results
                        rvUserSearchResults.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserSearch", "Error finding user", e);
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        sensorManager.unregisterListener(sensorEventListener);
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
        placesClient = null;
        if (clusterManager != null) {
            clusterManager.clearItems();
            clusterManager = null;
        }
        googleMap.setOnMapClickListener(null);
        googleMap.setOnMarkerClickListener(null);
        googleMap.setOnCameraIdleListener(null);
        googleMap.clear();
        googleMap = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
        currentLocationMarker.stopPulsatingEffect();
        sensorManager.unregisterListener(sensorEventListener);
        if (placesClient != null) {
            placesClient = null; // This shuts down the client and associated resources
        }
    }

    private static class UserWithFollowers {
        String username;
        int followerCount;

        UserWithFollowers(String username, int followerCount) {
            this.username = username;
            this.followerCount = followerCount;
        }
    }

    private List<UserWithFollowers> mergeSort(List<UserWithFollowers> users) {
        if (users.size() <= 1) return users;

        int mid = users.size() / 2;
        List<UserWithFollowers> left = mergeSort(users.subList(0, mid));
        List<UserWithFollowers> right = mergeSort(users.subList(mid, users.size()));

        return merge(left, right);
    }

    private List<UserWithFollowers> merge(List<UserWithFollowers> left, List<UserWithFollowers> right) {
        List<UserWithFollowers> merged = new ArrayList<>();
        int i = 0, j = 0;

        while (i < left.size() && j < right.size()) {
            if (left.get(i).followerCount >= right.get(j).followerCount) {
                merged.add(left.get(i++));
            } else {
                merged.add(right.get(j++));
            }
        }

        while (i < left.size()) merged.add(left.get(i++));
        while (j < right.size()) merged.add(right.get(j++));

        return merged;
    }
}