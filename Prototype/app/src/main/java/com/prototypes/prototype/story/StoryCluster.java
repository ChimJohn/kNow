package com.prototypes.prototype.story;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class StoryCluster implements ClusterItem {
    private final LatLng position;
    private final String id, userId, mediaUrl, thumbnailUrl, caption, category, mediaType;

    public StoryCluster(String id, String userId, double lat, double lng, String caption, String category, String thumbnailUrl, String mediaUrl, String mediaType) {
        this.position = new LatLng(lat, lng);
        this.id = id;
        this.userId = userId;
        this.caption = caption;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
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
    public String getMediaUrl() {
        return mediaUrl;
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
