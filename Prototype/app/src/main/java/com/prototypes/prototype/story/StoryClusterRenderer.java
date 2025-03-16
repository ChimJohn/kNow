package com.prototypes.prototype.story;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.prototypes.prototype.R;

import java.util.HashMap;
import java.util.Map;

public class StoryClusterRenderer extends DefaultClusterRenderer<StoryCluster> {
    private final Context context;
    private GoogleMap googleMap; // Store the GoogleMap reference
    private Map<StoryCluster, Marker> markerMap = new HashMap<>(); // To store references to markers

    public StoryClusterRenderer(Context context, GoogleMap map, ClusterManager<StoryCluster> clusterManager) {
        super(context, map, clusterManager);
        this.context = context;
        this.googleMap = map; // Initialize the GoogleMap reference
    }

    @Override
    protected void onBeforeClusterItemRendered(StoryCluster item, @NonNull MarkerOptions markerOptions) {
        Log.d("HEHE", "onBeforeClusterItemRendered called");

        // Set a default blue marker icon while the image is being loaded
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        // Create the custom StoryMarker view
        StoryMarker storyMarker = new StoryMarker(context);

        // Load the image from the URL and wait for it to be converted to a bitmap
        storyMarker.setMarkerImage(context, item.getImageUrl(), new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                // After the image is loaded, create the marker with the correct icon
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resource));

                // Create the marker and add it to the map (using the markerOptions)
                Marker marker = googleMap.addMarker(markerOptions); // Add the marker to the map

                // Store the marker reference to be updated later
                markerMap.put(item, marker);

                // Once the image is loaded and the marker is ready, update the cluster renderer
                StoryClusterRenderer.super.onBeforeClusterItemRendered(item, markerOptions);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                // Handle placeholder if needed
            }
        });
    }
}
