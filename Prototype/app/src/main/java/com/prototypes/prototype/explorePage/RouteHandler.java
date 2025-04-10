package com.prototypes.prototype.explorePage;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.Timestamp;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.clustering.ClusterItem;
import com.prototypes.prototype.R;
import com.prototypes.prototype.directions.DirectionsApiService;
import com.prototypes.prototype.directions.DirectionsResponse;
import com.prototypes.prototype.classes.Story;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RouteHandler {
    private final GoogleMap googleMap;
    private Polyline routePolyline, routeOutline;
    private final Context context;

    public RouteHandler(Context context, GoogleMap googleMap) {
        this.context = context;
        this.googleMap = googleMap;
    }

    public void fetchRoute(Location currentLocation, LatLng destination) {
        String origin = currentLocation.getLatitude() + "," + currentLocation.getLongitude();
        String dest = destination.latitude + "," + destination.longitude;
        String apiKey = context.getString(R.string.google_maps_key);

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

        if (routePolyline != null) routePolyline.remove();
        if (routeOutline != null) routeOutline.remove();

        PolylineOptions outlineOptions = new PolylineOptions()
                .addAll(points)
                .width(18f)
                .color(Color.argb(200, 0, 255, 255))
                .geodesic(true);

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(Color.BLUE)
                .geodesic(true);

        routeOutline = googleMap.addPolyline(outlineOptions);
        routePolyline = googleMap.addPolyline(polylineOptions);
    }

    public static class StoryCluster extends Story implements ClusterItem {

        public StoryCluster(String id, String userId, double latitude, double longitude, String caption, String category, String thumbnailUrl, String mediaUrl, String mediaType, Timestamp timestamp) {
            super(id, userId, caption, category, mediaUrl, latitude, longitude, mediaType, thumbnailUrl, timestamp);
        }

        @NonNull
        @Override
        public LatLng getPosition() {
            return new LatLng(getLatitude(), getLongitude());
        }

        @Nullable
        @Override
        public String getTitle() {
            return "";
        }

        @Override
        public String getSnippet() {
            return getCategory();
        }
        @Nullable
        @Override
        public Float getZIndex() {
            return 0f;
        }

    }
}
