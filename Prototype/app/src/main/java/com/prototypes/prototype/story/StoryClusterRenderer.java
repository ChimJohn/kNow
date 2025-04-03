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
    import android.util.LruCache;

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
    import com.google.maps.android.clustering.Cluster;
    import com.google.maps.android.clustering.ClusterManager;
    import com.google.maps.android.clustering.view.DefaultAdvancedMarkersClusterRenderer;

    import java.util.ArrayList;
    import java.util.Collection;
    import java.util.HashMap;
    import java.util.HashSet;
    import java.util.LinkedHashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.Set;

    public class StoryClusterRenderer extends DefaultAdvancedMarkersClusterRenderer<StoryCluster> {
        private final Context context;
        private final ClusterManager<StoryCluster> clusterManager;

        private Map<String, Marker> markerMap = new HashMap<>();

        private final LruCache<String, BitmapDescriptor> iconCache = new LruCache<>(200); // Max 50 items
        private final Map<Set<StoryCluster>, BitmapDescriptor> clusterIconCache =
                new LinkedHashMap<Set<StoryCluster>, BitmapDescriptor>(100, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Set<StoryCluster>, BitmapDescriptor> eldest) {
                        return size() > 100;  // Limit cache size to 100
                    }
                };
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
            BitmapDescriptor cachedIcon = iconCache.get(item.getId());
            if (cachedIcon != null) {
                marker.setIcon(cachedIcon);
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
        private void getClusterIcon(Collection<StoryCluster> clusterItems, OnClusterIconReady callback) {
            int baseSize = 200;
            int maxSize = 400;
            int clusterSize = clusterItems.size();
            int iconSize = Math.min(baseSize + (clusterSize * 10), maxSize);
            // Check cache first
            if (clusterIconCache.containsKey(clusterItems)) {
                callback.onIconReady(clusterIconCache.get(clusterItems));
                return;
            }
            List<Bitmap> thumbnails = new ArrayList<>();
            List<StoryCluster> items = new ArrayList<>(clusterItems);
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
                                loadedCount[0]++;
                                if (loadedCount[0] == clusterSize) {
                                    BitmapDescriptor icon = createPieChart(thumbnails, iconSize, clusterSize);
                                    clusterIconCache.put(new HashSet<>(clusterItems), icon);  // Cache result
                                    callback.onIconReady(icon);
                                }
                            }
                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {}
                        });
            }
            if (loadedCount[0] < clusterSize) {
                callback.onIconReady(createPieChart(new ArrayList<>(), iconSize, clusterSize));
            }
        }
        private BitmapDescriptor createPieChart(List<Bitmap> thumbnails, int size, int clusterSize) {
            // Define min and max size constraints
            int minSize = 50;  // Minimum circle size
            int maxSize = 300; // Maximum circle size
            size = Math.max(minSize, Math.min(size, maxSize)); // Clamp the size
            if (thumbnails.size() > 6) {
                thumbnails = thumbnails.subList(0, 6);
            }
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            // Create the bitmap to represent the cluster icon
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            // Darken the background by adding a gray circle
            paint.setColor(Color.argb(150, 169, 169, 169));  // Gray color (50% opacity)
            float radius = size / 2f;  // Radius of the circle
            canvas.drawCircle(radius, radius, radius, paint);  // Draw gray circle background

            // If there are thumbnails, draw them in a pie chart
            if (!thumbnails.isEmpty()) {
                paint.setColor(Color.argb(150, 0, 0, 0));  // Semi-transparent black background
                canvas.drawCircle(radius, radius, radius, paint);  // Draw dark circle

                // Draw pie chart sections (thumbnails)
                float startAngle = 0;
                float sweepAngle = 360f / thumbnails.size();
                RectF bounds = new RectF(0, 0, size, size);  // Circle bounds
                for (Bitmap thumb : thumbnails) {
                    Bitmap scaledThumb = Bitmap.createScaledBitmap(thumb, size / 2, size / 2, true);
                    BitmapShader shader = new BitmapShader(scaledThumb, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    paint.setShader(shader);
                    canvas.drawArc(bounds, startAngle, sweepAngle, true, paint);
                    startAngle += sweepAngle;
                }
            }

            // Reset shader to ensure the text is white
            paint.setShader(null);
            paint.setColor(Color.WHITE);
            paint.setTextSize(size / 3f * 1.1f);
            paint.setTextAlign(Paint.Align.CENTER);

            // Calculate the vertical alignment for the text
            float textY = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f);
            String clusterSizeText = String.valueOf(clusterSize);

            // Draw cluster size number in the center
            canvas.drawText(clusterSizeText, size / 2f, textY, paint);
            Bitmap finalBitmap = getCircularBitmapWithBorder(output, 8, Color.WHITE);
            output.recycle();
            return BitmapDescriptorFactory.fromBitmap(finalBitmap);
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
