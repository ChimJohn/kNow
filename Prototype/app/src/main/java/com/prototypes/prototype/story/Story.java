package com.prototypes.prototype.story;

public class Story {
    private String id;
    private String userId;
    private String caption;
    private String mediaUrl;

    public Story(String id, String userId, String caption, String mediaUrl) {
        this.id = id;
        this.userId = userId;
        this.caption = caption;
        this.mediaUrl = mediaUrl;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getCaption() { return caption; }
    public String getMediaUrl() { return mediaUrl; }
}
