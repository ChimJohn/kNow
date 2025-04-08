package com.prototypes.prototype.story;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Objects;

public class StoryCluster extends Story implements ClusterItem {

    public StoryCluster(String id, String userId, double latitude, double longitude, String caption, String category, String thumbnailUrl, String mediaUrl, String mediaType) {
        super(id, userId, caption, category, mediaUrl, latitude, longitude, mediaType, thumbnailUrl);
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return new LatLng(getLatitude(), getLongitude());
    }

    @Nullable
    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public String getSnippet() {
        return getCategory();
    }
    @Nullable
    @Override
    public Float getZIndex() {
        return 0f;
    }

}
