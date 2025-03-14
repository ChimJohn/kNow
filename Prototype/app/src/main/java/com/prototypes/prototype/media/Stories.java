package com.prototypes.prototype.media;

import java.util.List;

public class Stories {
    private String imageUrl;
    private String caption;
    private String creator;
    private String coordinate;
    private List<String> maps;

    public Stories() {
    }

    public Stories(String imageUrl, String caption, String creator, String coordinate, List<String> maps) {
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.creator = creator;
        this.coordinate = coordinate;
        this.maps = maps;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public List<String> getMaps() {
        return maps;
    }

    public void setMaps(List<String> maps) {
        this.maps = maps;
    }

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

}
