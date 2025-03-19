package com.prototypes.prototype.story;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class StoryCluster implements ClusterItem {
    private final LatLng position;
    private final String id, imageUrl, thumbnailUrl, caption, category;

    public StoryCluster(String id, double lat, double lng, String caption, String category, String thumbnailUrl, String imageUrl) {
        this.position = new LatLng(lat, lng);
        this.id = id;
        this.caption = caption;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
        this.imageUrl = imageUrl;
    }

    @Nullable
    @Override
    public String getTitle() {
        return "";
    }
    @NonNull
    @Override
    public LatLng getPosition() {
        return position;
    }
    public String getId(){
        return id;
    }
    public String getCaption(){
        return caption;
    }
    public String getThumbnailUrl() {
        return thumbnailUrl;
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
