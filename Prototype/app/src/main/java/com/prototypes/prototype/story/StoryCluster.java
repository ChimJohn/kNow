package com.prototypes.prototype.story;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Objects;

public class StoryCluster implements ClusterItem {
    private final Double latitude;
    private final Double longitude;
    private final String id, userId, mediaUrl, thumbnailUrl, caption, category, mediaType;
    public StoryCluster(String id, String userId, double lat, double lng, String caption, String category, String thumbnailUrl, String mediaUrl, String mediaType) {
        this.latitude = lat;
        this.longitude = lng;
        this.id = id;
        this.userId = userId;
        this.caption = caption;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }

    @Nullable
    @Override
    public String getTitle() {
        return "";
    }
    @NonNull
    public Double getLatitude() {
        return latitude;
    }
    public Double getLongitude() {
        return longitude;
    }
    public String getId(){
        return id;
    }
    public String getUserId(){
        return userId;
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
    public String getMediaType() {
        return mediaType;
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
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoryCluster that = (StoryCluster) o;
        return Objects.equals(this.getId(), that.getId());
    }
    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getUserId());
    }
}
