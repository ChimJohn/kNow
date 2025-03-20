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
        private Map<String, Marker> markerMap = new HashMap<>();

        private final Map<String, BitmapDescriptor> iconCache = new HashMap<>(); // Store loaded icons
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
        @Override
        protected void onClusterItemRendered(StoryCluster item, @NonNull Marker marker) {
            marker.setTag(item.getId());
            markerMap.put(item.getId(), marker);
            if (iconCache.containsKey(item.getId())) {
                marker.setIcon(iconCache.get(item.getId()));
            } else {
                loadMarkerImage(item);
            }
        }
        @Override
        protected boolean shouldRenderAsCluster(com.google.maps.android.clustering.Cluster<StoryCluster> cluster) {
            return cluster.getSize() > 2; // Cluster when at least 2 markers exist
        }
        private void loadMarkerImage(StoryCluster item) {
            StoryMarker storyMarker = new StoryMarker(context);
            storyMarker.setMarkerImage(context, item.getThumbnailUrl(), new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    Bitmap circularBitmap = getCircularBitmapWithBorder(resource, 8, Color.WHITE);
                    BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(circularBitmap);
                    iconCache.put(item.getId(), icon);
                    // Directly update the specific marker using the map
                    Marker marker = markerMap.get(item.getId());
                    if (marker != null) {
                        marker.setIcon(icon);
                    }
                    clusterManager.cluster();
                }
                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    // Handle cleanup if needed
                }
            });
        }
        /**
         * Generates a cluster icon with the number of items in the cluster.
         */
        private BitmapDescriptor getClusterIcon(int clusterSize) {
            StoryMarker storyMarker = new StoryMarker(context);
            Bitmap clusterBitmap = storyMarker.createClusterIcon(clusterSize);
            return BitmapDescriptorFactory.fromBitmap(clusterBitmap);
        }
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
