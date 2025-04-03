package com.prototypes.prototype;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
import com.prototypes.prototype.directions.DirectionsApiService;
import com.prototypes.prototype.directions.DirectionsResponse;
import com.prototypes.prototype.story.Story;
import com.prototypes.prototype.story.StoryCluster;
import com.prototypes.prototype.story.StoryClusterRenderer;
import com.prototypes.prototype.story.StoryViewFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ExploreFragment extends Fragment {
    private RecyclerView rvUserSearchResults;
    private UserSearchAdapter userSearchAdapter;
    private MapView mapView;
    private GoogleMap googleMap;
    private CurrentLocationViewModel currentLocationViewModel;
    private Double latitude, longitude;
    private Circle pulsatingCircle;
    private ValueAnimator pulseAnimator;
    private FirebaseFirestore db;
    private ClusterManager<StoryCluster> clusterManager;
    private boolean isFirstLocationUpdate = true;
    private ListenerRegistration mediaListener;
    private final Map<String, StoryCluster> allMarkers = new HashMap<>();
    private Polyline routePolyline;
    private Polyline routeOutline;
    private PlacesClient placesClient;
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private float currentBearing = 0f;
    private Marker gpsMarker;

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
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.map_style));
            LatLng singapore = new LatLng(1.3521, 103.8198);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore, 20));

            final long FETCH_INTERVAL = 30000; // 30 seconds
            AtomicLong lastFetchTime = new AtomicLong(); // Stores last fetch timestamp

            currentLocationViewModel.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
                if (location != null) {
                    updateGpsMarker(location);
                    if (isFirstLocationUpdate) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 20));
                    }
                    isFirstLocationUpdate = false;

                    if (latitude != null && longitude != null){
                        long currentTime = System.currentTimeMillis(); // Get current time
                        LatLng latLng = new LatLng(latitude, longitude);
                        if (currentTime - lastFetchTime.get() >= FETCH_INTERVAL) {
                            fetchRoute(location, latLng); // Call fetch function
                            lastFetchTime.set(currentTime); // Update last fetch time
                            Log.d("API_CALL", "Fetching route at: " + currentTime);
                        } else {
                            Log.d("API_CALL", "Skipping fetch, waiting for next interval.");
                        }
                    }
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
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, StoryViewFragment.newInstance(storyList));
                transaction.addToBackStack(null);
                transaction.commit();
                if (getActivity() != null) {
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
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
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, StoryViewFragment.newInstance(storyList));
                transaction.addToBackStack(null);
                transaction.commit();
                if (getActivity() != null) {
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
                    if (bottomNav != null) {
                        bottomNav.setVisibility(View.GONE);
                    }
                }
                return true;
            });

            listenToMarkersData();
        });

        EditText etSearch = view.findViewById(R.id.etSearch);
        rvUserSearchResults = view.findViewById(R.id.rvSearchResults);
        rvUserSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
//        userSearchAdapter = new UserSearchAdapter(new ArrayList<>(), this::handleUserClick);
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
            if (gpsMarker != null) {
                gpsMarker.setRotation(currentBearing);
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };



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
        if (gpsMarker == null) {
            gpsMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow_up_gps))
                    .anchor(0.5f, 0.5f)
                    .rotation(location.getBearing()));
        } else {
            gpsMarker.setPosition(latLng);
            gpsMarker.setRotation(location.getBearing());
        }

        if (pulsatingCircle == null) {
            pulsatingCircle = googleMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(1)
                    .strokeWidth(0f)
                    .fillColor(Color.argb(100, 10, 163, 223)));
            startPulsatingEffect();
        } else {
            pulsatingCircle.setCenter(latLng);
        }
    }

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

    private void startPulsatingEffect() {
        pulseAnimator = ValueAnimator.ofFloat(10, 20);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.addUpdateListener(animation -> pulsatingCircle.setRadius((float) animation.getAnimatedValue()));
        pulseAnimator.start();
    }

    private void fetchRoute(Location currentLocation, LatLng destination) {
        String origin = currentLocation.getLatitude() + "," + currentLocation.getLongitude(); // Replace with current location
        String dest = destination.latitude + "," + destination.longitude;
        String apiKey = getString(R.string.google_maps_key);;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        DirectionsApiService service = retrofit.create(DirectionsApiService.class);
        Call<DirectionsResponse> call = service.getWalkingDirections(origin, dest, "walking", apiKey);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().routes.isEmpty()) {
                    String polyline = response.body().routes.get(0).overviewPolyline.points;
                    drawRoute(polyline);
                }
            }
            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Log.e("Directions API", "Error fetching directions: " + t.getMessage());
            }
        });
    }

    private void drawRoute(String encodedPolyline) {
        List<LatLng> points = PolyUtil.decode(encodedPolyline);

        if (routePolyline != null) {
            routePolyline.remove();
        }
        if (routeOutline != null) {
            routeOutline.remove();
        }

        // Soft outline (wider, lighter color)
        PolylineOptions outlineOptions = new PolylineOptions()
                .addAll(points)
                .width(18f)
                .color(Color.argb(200, 0, 255, 255))
                .geodesic(true)
                .startCap(new RoundCap())
                .endCap(new RoundCap())
                .jointType(JointType.ROUND);

        // Main route (solid color)
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(Color.BLUE)
                .geodesic(true)
                .startCap(new RoundCap())
                .endCap(new RoundCap())
                .jointType(JointType.ROUND);

        if (googleMap != null) {
            routeOutline = googleMap.addPolyline(outlineOptions);
            routePolyline = googleMap.addPolyline(polylineOptions);
        }
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
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }
}