package com.prototypes.prototype.story;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.prototypes.prototype.R;

public class MapManager {
    private final GoogleMap googleMap;
    private Marker gpsMarker;
    private Circle pulsatingCircle;
    private ValueAnimator pulseAnimator;
    private final Context context;

    public MapManager(Context context, GoogleMap googleMap) {
        this.context = context;
        this.googleMap = googleMap;
        setMapSettings();
    }

    private void setMapSettings() {
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style));
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);
    }

    public void updateGpsMarker(Location location) {
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

    private void startPulsatingEffect() {
        pulseAnimator = ValueAnimator.ofFloat(10, 20);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.addUpdateListener(animation -> pulsatingCircle.setRadius((float) animation.getAnimatedValue()));
        pulseAnimator.start();
    }

    public void cleanup() {
        if (pulseAnimator != null) pulseAnimator.cancel();
    }
}
