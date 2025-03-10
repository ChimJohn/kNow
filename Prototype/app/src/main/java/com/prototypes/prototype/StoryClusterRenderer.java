package com.prototypes.prototype;

import android.content.Context;
import android.graphics.Bitmap;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.clustering.ClusterItem;
import com.prototypes.prototype.StoryCluster;

public class StoryClusterRenderer extends DefaultClusterRenderer<StoryCluster> {
    private final Context context;

    public StoryClusterRenderer(Context context, GoogleMap map, ClusterManager<StoryCluster> clusterManager) {
        super(context, map, clusterManager);
        this.context = context;
    }

    @Override
    protected void onBeforeClusterItemRendered(StoryCluster item, MarkerOptions markerOptions) {
        // Create custom StoryMarker view
        StoryMarker storyMarker = new StoryMarker(context);
        storyMarker.setMarkerText(item.getTitle());
        storyMarker.setMarkerImage(0);

        // Convert to Bitmap
        Bitmap markerBitmap = storyMarker.getMarkerBitmap();

        // Set custom icon
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerBitmap));
    }
}
