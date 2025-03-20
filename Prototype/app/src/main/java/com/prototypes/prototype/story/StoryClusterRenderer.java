    package com.prototypes.prototype.story;

    import android.content.Context;
    import android.graphics.Bitmap;
    import android.graphics.BitmapShader;
    import android.graphics.Canvas;
    import android.graphics.Color;
    import android.graphics.Paint;
    import android.graphics.PorterDuff;
    import android.graphics.PorterDuffXfermode;
    import android.graphics.Rect;
    import android.graphics.RectF;
    import android.graphics.Shader;
    import android.graphics.drawable.Drawable;
    import android.os.Build;
    import android.util.Log;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;

    import com.bumptech.glide.Glide;
    import com.bumptech.glide.load.engine.DiskCacheStrategy;
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

    import java.util.ArrayList;
    import java.util.Collection;
    import java.util.HashMap;
    import java.util.List;
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
            getClusterIcon(cluster.getItems(), bitmapDescriptor -> markerOptions.icon(bitmapDescriptor));
        }

        interface OnClusterIconReady {
            void onIconReady(BitmapDescriptor bitmapDescriptor);
        }

        @Override
        protected void onClusterUpdated(@NonNull Cluster<StoryCluster> cluster, AdvancedMarker marker) {
            getClusterIcon(cluster.getItems(), marker::setIcon);
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
            return cluster.getSize() > 3; // Cluster when at least 2 markers exist
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
        private BitmapDescriptor createPieChart(List<Bitmap> thumbnails, int size, int clusterSize) {
            Bitmap output;
            Canvas canvas;
            Paint paint = new Paint();
            paint.setAntiAlias(true);

            // Create the bitmap to represent the cluster icon
            output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(output);

            // Darken the background by adding a gray circle
            paint.setColor(Color.argb(150, 169, 169, 169));  // Gray color (with 50% opacity)
            paint.setTextSize(size / 3f * 1.5f); // Adjust text size to 1.5x the previous size
            paint.setTextAlign(Paint.Align.CENTER);

            float radius = size / 2f;  // Radius of the circle (half of the size)
            canvas.drawCircle(radius, radius, radius, paint);  // Draw gray circle background

            // If there are thumbnails, draw the pie chart sections
            if (!thumbnails.isEmpty()) {
                // Darken the background by adding a semi-transparent black circle
                paint.setColor(Color.argb(150, 0, 0, 0));  // RGBA (150 alpha = 50% opacity)
                canvas.drawCircle(radius, radius, radius, paint);  // Draw dark circle background

                // Draw the pie chart section (thumbnails) on top of the dark circle
                float startAngle = 0;
                float sweepAngle = 360f / thumbnails.size();
                RectF bounds = new RectF(0, 0, size, size);  // Circle bounds
                for (Bitmap thumb : thumbnails) {
                    BitmapShader shader = new BitmapShader(thumb, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    paint.setShader(shader);
                    canvas.drawArc(bounds, startAngle, sweepAngle, true, paint);
                    startAngle += sweepAngle;
                }
            }

            // Draw the cluster size number in the center (this should be the last thing drawn)
            paint.setShader(null);  // Reset shader to ensure the text is white
            paint.setColor(Color.WHITE);  // Set the number color to white
            paint.setTextSize(size / 3f * 1.5f); // Adjust text size
            paint.setTextAlign(Paint.Align.CENTER);

            // Calculate the vertical alignment for the text
            float textY = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f);

            // Show the expected cluster size number (not the actual thumbnails.size())
            String clusterSizeText = String.valueOf(clusterSize); // Use clusterSize from parameters

            // Draw the text in the center
            canvas.drawText(clusterSizeText, size / 2f, textY, paint);

            return BitmapDescriptorFactory.fromBitmap(output);
        }


        private void getClusterIcon(Collection<StoryCluster> clusterItems, OnClusterIconReady callback) {
            int baseSize = 200; // Minimum size
            int maxSize = 400; // Maximum size
            int clusterSize = clusterItems.size();

            // Scale size between baseSize and maxSize
            int iconSize = Math.min(baseSize + (clusterSize * 10), maxSize);

            List<Bitmap> thumbnails = new ArrayList<>();
            List<StoryCluster> items = new ArrayList<>(clusterItems);

            // Keep track of how many images are loaded
            final int[] loadedCount = {0};

            for (StoryCluster item : items) {
                Glide.with(context)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(false)
                        .load(item.getThumbnailUrl())
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                thumbnails.add(resource);
                                loadedCount[0]++;  // Increment the loaded count

                                // If all thumbnails are loaded, update the cluster icon
                                if (loadedCount[0] == clusterSize) {
                                    callback.onIconReady(createPieChart(thumbnails, iconSize, clusterSize));
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                // Handle cleanup if needed
                            }
                        });
            }

            // If some images are still loading, show a fallback (gray circle with number)
            if (loadedCount[0] < clusterSize) {
                // Show a fallback icon while images are still loading
                callback.onIconReady(createPieChart(new ArrayList<>(), iconSize, clusterSize));
            }
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
