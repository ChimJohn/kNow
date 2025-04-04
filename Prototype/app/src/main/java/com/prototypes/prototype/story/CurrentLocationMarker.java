package com.prototypes.prototype.story;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;

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
    private float currentBearing = 0f;
    private final SensorManager sensorManager;
    private final Sensor rotationVectorSensor;
    ValueAnimator pulseAnimator;
    public CurrentLocationMarker(Activity activity, GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        this.rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationVectorSensor != null) {
            Log.d("SensorCheck", "Rotation Vector Sensor is available.");
        } else {
            Log.d("SensorCheck", "Rotation Vector Sensor is NOT available.");
        }


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

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] rotationMatrix = new float[9];
            float[] orientationAngles = new float[3];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            currentBearing = (float) Math.toDegrees(orientationAngles[0]);
            currentBearing = (currentBearing + 360) % 360;
            Log.d("HEHEHE", String.valueOf(currentBearing));
            if (gpsMarker != null) {
                gpsMarker.setRotation(currentBearing);
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    public void registerSensorListener() {
        if (rotationVectorSensor != null) {
            Log.d("HEHEHE", "HAHA");
            sensorManager.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }
    public void unregisterSensorListener() {
        sensorManager.unregisterListener(sensorEventListener);
        pulseAnimator.cancel();
    }
}
