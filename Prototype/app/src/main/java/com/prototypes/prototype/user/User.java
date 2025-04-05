package com.prototypes.prototype.user;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.R;
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.custommap.CustomMapAdaptor;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.story.Story;

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

    public static String getUid(Activity activity){
        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(activity);
        return firebaseAuthManager.getCurrentUser().getUid();
    }

    public static void  getUserData(Activity activity, FirestoreManager firestoreManager ,UserReadCallback callback){
        String TAG = "getUserData: User class";
//        FirestoreManager firestoreManager = new FirestoreManager(db, User.class);

        firestoreManager.readDocument("Users", User.getUid(activity), new FirestoreManager.FirestoreReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "firestoreManager failed: "+ e);
                callback.onFailure(e);
            }
        });


    };
    public static void getMaps(Activity activity, FirestoreManager firestoreMapManager,UserCallback callback){
        String TAG = "getMaps: User class";
//        FirestoreManager firestoreMapManager = new FirestoreManager(db, CustomMap.class);

        firestoreMapManager.queryDocuments("map", "owner", getUid(activity), new FirestoreManager.FirestoreQueryCallback<CustomMap>() {
            @Override
            public void onEmpty(ArrayList<CustomMap> customMaps) {
                Log.d(TAG, "Number of Custom Maps: 0");
                callback.onMapsLoaded(customMaps);
            }
            @Override
            public void onSuccess(ArrayList<CustomMap> customMaps) {
                Log.d(TAG, "Number of Custom Maps: "+ customMaps.size());
                callback.onMapsLoaded(customMaps);
            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "firestoreMapManager failed: "+ e);
                callback.onError(e);
            }
        });
    }
    public static void getStories(Activity activity, FirestoreManager firestoreStoriesManager, UserCallback callback){
        String TAG = "getStories: User class";
        firestoreStoriesManager.queryDocuments("media", "userId", getUid(activity), new FirestoreManager.FirestoreQueryCallback<Story>() {
            @Override
            public void onEmpty(ArrayList<Story> storyList) {
                callback.onMapsLoaded(storyList);
            }
            @Override
            public void onSuccess(ArrayList<Story> storyList) {
                callback.onMapsLoaded(storyList);
            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "firestoreStoriesManager failed: " + e);
                callback.onError(e);
            }
        });
    };

    public interface UserReadCallback<T> {
        void onSuccess(T object);

        void onFailure(Exception e);
    }
    public interface UserCallback<T> {
        void onMapsLoaded(ArrayList<T> results);
        void onError(Exception e);
    }
}
