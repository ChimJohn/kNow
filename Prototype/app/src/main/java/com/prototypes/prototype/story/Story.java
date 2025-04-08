package com.prototypes.prototype.story;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

import java.util.List;

public class Story implements Parcelable {
    private String id, userId, caption, category, mediaType, mediaUrl, thumbnailUrl;
    private Timestamp timestamp;
    private double latitude, longitude;
    private List<String> mapsID;

    public Story(){}

    public Story(String id, String userId, String caption, String category, String mediaUrl, double latitude, double longitude, String mediaType, String thumbnailUrl) {
        this.id = id;
        this.userId = userId;
        this.caption = caption;
        this.category = category;
        this.mediaUrl = mediaUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mediaType = mediaType;
        this.thumbnailUrl = thumbnailUrl;
    }



    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getCaption() { return caption; }
    public String getMediaUrl() { return mediaUrl; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getMediaType() { return this.mediaType; }
    public String getCategory() { return category; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public Boolean isVideo(){ return mediaType.equals("video"); }
    protected Story(Parcel in) {
        id = in.readString();
        userId = in.readString();
        caption = in.readString();
        category = in.readString();
        mediaUrl = in.readString();
        thumbnailUrl = in.readString();
        mediaType = in.readString();
        timestamp = new Timestamp(in.readLong(), 0);
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userId);
        dest.writeString(caption);
        dest.writeString(category);
        dest.writeString(mediaUrl);
        dest.writeString(thumbnailUrl);
        dest.writeString(mediaType);
        dest.writeLong(timestamp.getSeconds()); // Firestore Timestamp workaround
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Story> CREATOR = new Creator<>() {
        @Override
        public Story createFromParcel(Parcel in) {
            return new Story(in);
        }
        @Override
        public Story[] newArray(int size) {
            return new Story[size];
        }
    };
}
