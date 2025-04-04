package com.prototypes.prototype.user;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String email;
    private String name;
    private String profile;
    private List<String> followers;
    private List<String> following;
    private List<String> stories;

    public User(){
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
    };

    public User(String name, String username, String email){
        this.name = name;
        this.username = username;
        this.email = email;
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
    }

    public User(String email, String name, String profile, String username){
        this.username = username;
        this.email = email;
        this.name = name;
        this.profile = profile;
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getFollowers() {
        return followers;
    }

    public void setFollowers(List<String> followers) {
        this.followers = followers;
    }

    public List<String> getFollowing() {
        return following;
    }

    public void setFollowing(List<String> following) {
        this.following = following;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public List<String> getStories() {
        return stories;
    }

    public void setStories(List<String> stories) {
        this.stories = stories;
    }

}
