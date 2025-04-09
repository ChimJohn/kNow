package com.prototypes.prototype.explorePage;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.prototypes.prototype.R;

public class CurrentLocationMarker {
    private Marker gpsMarker;
    private Circle pulsatingCircle;
    private GoogleMap googleMap;
    ValueAnimator pulseAnimator;
    public CurrentLocationMarker(Activity activity, GoogleMap googleMap) {
        this.googleMap = googleMap;
    }
    public Marker getGpsMarker(){
        return this.gpsMarker;
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
        updatePulsatingCircle(latLng);
    }

    private void updatePulsatingCircle(LatLng latLng) {
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
        if (pulseAnimator == null){
            return;
        }
        pulseAnimator = ValueAnimator.ofFloat(10, 20);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.addUpdateListener(animation -> {
            if (pulsatingCircle != null) {
                pulsatingCircle.setRadius((Float) animation.getAnimatedValue());
            }
        });
        pulseAnimator.start();
    }

    public void stopPulsatingEffect(){
        if (pulseAnimator != null){
            pulseAnimator.cancel();
        }
    }
}
