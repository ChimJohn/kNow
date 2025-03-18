package com.prototypes.prototype.story;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.AdvancedMarker;
import com.google.android.gms.maps.model.AdvancedMarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultAdvancedMarkersClusterRenderer;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.clustering.Cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StoryClusterRenderer extends DefaultAdvancedMarkersClusterRenderer<StoryCluster> {
    private final Context context;
    private final ClusterManager<StoryCluster> clusterManager;
    private final Map<StoryCluster, BitmapDescriptor> iconCache = new HashMap<>(); // Store loaded icons

    public StoryClusterRenderer(Context context, GoogleMap map, ClusterManager<StoryCluster> clusterManager) {
        super(context, map, clusterManager);
        this.context = context;
        this.clusterManager = clusterManager;
    }

    @Override
    protected void onBeforeClusterRendered(@NonNull Cluster<StoryCluster> cluster,
                                           @NonNull AdvancedMarkerOptions markerOptions) {
        BitmapDescriptor clusterIcon = getClusterIcon(cluster.getSize());
        markerOptions.icon(clusterIcon);
    }

    @Override
    protected void onClusterUpdated(@NonNull Cluster<StoryCluster> cluster, AdvancedMarker marker) {
        BitmapDescriptor clusterIcon = getClusterIcon(cluster.getSize());
        marker.setIcon(clusterIcon);
    }

    /**
     * Generates a cluster icon with the number of items in the cluster.
     */
    private BitmapDescriptor getClusterIcon(int clusterSize) {
        // Create a view dynamically or use a predefined layout
        StoryMarker storyMarker = new StoryMarker(context);
        Bitmap clusterBitmap = storyMarker.createClusterIcon(clusterSize);

        return BitmapDescriptorFactory.fromBitmap(clusterBitmap);
    }


    @Override
    protected void onClusterItemRendered(StoryCluster item, @NonNull Marker marker) {
        Log.d("MarkerStatus", "Rendering marker: " + item.getTitle());
        // After the image is loaded, set the correct icon
        if (iconCache.containsKey(item)) {
            marker.setIcon(iconCache.get(item));
        } else {
            loadMarkerImage(item); // If icon not cached yet, load the image and update
        }
    }
    /**
     * Loads the image and updates the cache.
     */
    private void loadMarkerImage(StoryCluster item) {
        StoryMarker storyMarker = new StoryMarker(context);
        storyMarker.setMarkerImage(context, item.getImageUrl(), new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                Log.d("HEHE", "Image Loaded for: " + item.getTitle());
                // Convert to circular bitmap
                Bitmap circularBitmap = getCircularBitmapWithBorder(resource, 8, Color.WHITE);
                BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(circularBitmap);
                // Cache the icon
                iconCache.put(item, icon);
                clusterManager.setAnimation(true); // Enable animation (if necessary)
                for (Marker marker : clusterManager.getMarkerCollection().getMarkers()) {
                    if (marker.getTitle() != null && marker.getTitle().equals(item.getTitle())) {
                        marker.setIcon(icon); // Set the correct icon
                    }
                }
                clusterManager.cluster(); // Trigger clustering again

            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                // Handle cleanup if needed
            }
        });
    }
    @Override
            protected boolean shouldRenderAsCluster(com.google.maps.android.clustering.Cluster<StoryCluster> cluster) {
        return cluster.getSize() > 1; // Cluster when at least 2 markers exist
    }

    /**
     * Converts a bitmap into a circular bitmap with an optional border.
     */
    private Bitmap getCircularBitmapWithBorder(Bitmap bitmap, int borderWidth, int borderColor) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int diameter = Math.min(width, height);

        Bitmap output = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, diameter, diameter);
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.BLACK);

        float radius = diameter / 2.0f;
        canvas.drawCircle(radius, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rectF, paint);

        // Draw border
        if (borderWidth > 0) {
            paint.setXfermode(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(borderColor);
            paint.setStrokeWidth(borderWidth);
            canvas.drawCircle(radius, radius, radius - (borderWidth / 2.0f), paint);
        }

        return output;
    }
}
