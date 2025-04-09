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
import com.prototypes.prototype.CurrentLocationViewModel;
import com.prototypes.prototype.R;
import com.prototypes.prototype.UserSearchAdapter;
import com.prototypes.prototype.user.UserProfileFragment;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {
    private RecyclerView rvUserSearchResults;
    private UserSearchAdapter userSearchAdapter;
    private MapView mapView;
    private CurrentLocationViewModel currentLocationViewModel;
    private FirebaseFirestore db;
    private PlacesClient placesClient;
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private MapManager mapManager;
    private boolean suppressSearch = false;
    private static final String DESTINATION_LATITUDE = "destinationLatitude";
    private static final String DESTINATION_LONGITUDE = "destinationLongitude";
    private Double destinationLatitude, destinationLongitude;
    private int zoomInt = 16;

    public static ExploreFragment newInstance(double destinationLatitude, double destinationLongitude) {
        ExploreFragment fragment = new ExploreFragment();
        Bundle args = new Bundle();
        args.putDouble(DESTINATION_LATITUDE, destinationLatitude);
        args.putDouble(DESTINATION_LONGITUDE, destinationLongitude);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            destinationLatitude = getArguments().getDouble(DESTINATION_LATITUDE);
            destinationLongitude = getArguments().getDouble(DESTINATION_LONGITUDE);
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
            mapManager = new MapManager(requireActivity(), requireContext(), getParentFragmentManager());
            mapManager.initMap(map);
            mapManager.listenToStoryMarkersData();
            currentLocationViewModel.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
                mapManager.setOwnMarkerLocation(location);
                if (destinationLatitude != null && destinationLongitude != null) {
                    mapManager.routeToDestination(location, destinationLatitude, destinationLongitude);
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
            if (currentLocation != null) {
                mapManager.animateCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), zoomInt);
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
                }
                else if (!query.isEmpty()) {
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
            float currentBearing = (float) Math.toDegrees(orientationAngles[0]);
            currentBearing = (currentBearing + 360) % 360;
            if (mapManager != null && mapManager.getCurrentLocationMarker() != null && mapManager.getCurrentLocationMarker().getGpsMarker() != null) {
                mapManager.getCurrentLocationMarker().getGpsMarker().setRotation(currentBearing);
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
            if (latLng != null) {
                mapManager.animateCamera(latLng, zoomInt);
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
        if (mapView != null) {
            mapView.onDestroy();
        }
        mapManager.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(sensorEventListener);
        if (placesClient != null) {
            placesClient = null; // This shuts down the client and associated resources
        }
        mapManager.clear();
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