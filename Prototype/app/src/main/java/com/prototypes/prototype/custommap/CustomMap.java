package com.prototypes.prototype.custommap;

import java.security.acl.Owner;

public class CustomMap {
    private String name;
    private String owner;
    private String imageUrl;

    public CustomMap() {}
    public CustomMap(String name, String owner, String imageUrl) {
        this.name = name;
        this.owner = owner;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
