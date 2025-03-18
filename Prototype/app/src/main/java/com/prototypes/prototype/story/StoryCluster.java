package com.prototypes.prototype.story;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class StoryCluster implements ClusterItem {
    private final LatLng position;
    private final String caption;
    private final String imageUrl;
    private final String category;

    public StoryCluster(double lat, double lng, String caption, String category, String imageUrl) {
        position = new LatLng(lat, lng);
        this.caption = caption;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return position;
    }

    @Nullable
    @Override
    public String getTitle() {
        return "";
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String getSnippet() {
        return category;
    }

    @Nullable
    @Override
    public Float getZIndex() {
        return 0f;
    }

}
