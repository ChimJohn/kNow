package com.prototypes.prototype;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class ExploreFragment extends Fragment {

    private MapView mapView;
    private GoogleMap googleMap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explore, container, false);

        // Initialize MapView
        mapView = rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.onResume(); // Needed to display the map immediately

        try {
            MapsInitializer.initialize(requireActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set up the map
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap map) {

                // Apply minimal map styling
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.map_style));

                // Default location: Singapore
                LatLng singapore = new LatLng(1.3521, 103.8198);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore, 12));

                // Remove Google default POIs and labels
                map.getUiSettings().setMapToolbarEnabled(false);
                map.getUiSettings().setCompassEnabled(false);
                map.getUiSettings().setRotateGesturesEnabled(false);
                map.getUiSettings().setTiltGesturesEnabled(false);

                // Create a TextView and customize it
                TextView textView = new TextView(requireContext());
                textView.setText("Hello!!");
                textView.setTextSize(18);  // Set the text size
                textView.setBackgroundColor(Color.BLACK);
                textView.setTextColor(Color.YELLOW);

                // Convert TextView to BitmapDescriptor
                BitmapDescriptor bitmapDescriptor = createBitmapDescriptorFromTextView(textView);

                // Add marker with the custom icon
                map.addMarker(
                        new MarkerOptions()
                                .position(singapore)
                                .icon(bitmapDescriptor));
            }
        });

        return rootView;
    }

    // Method to convert TextView to BitmapDescriptor
    private BitmapDescriptor createBitmapDescriptorFromTextView(TextView textView) {
        // Measure and layout the TextView
        textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());

        // Create a Bitmap from the TextView
        Bitmap bitmap = Bitmap.createBitmap(textView.getMeasuredWidth(), textView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        textView.draw(canvas);

        // Return a BitmapDescriptor from the Bitmap
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    // Lifecycle Methods for Proper Map Handling
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }
}
