package com.prototypes.prototype.story;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class StoryCluster implements ClusterItem {
    private final LatLng position;
    private final String title;
    private final String snippet;
    private final String imageUrl;

    public StoryCluster(double lat, double lng, String title, String snippet, String imageUrl) {
        position = new LatLng(lat, lng);
        this.title = title;
        this.snippet = snippet;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    @Nullable
    @Override
    public Float getZIndex() {
        return 0f;
    }

}
