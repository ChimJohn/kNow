package com.prototypes.prototype.user;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;

import java.util.List;

public class FollowManager {
    private FirebaseFirestore db;
    private FirebaseAuthManager authManager;
    private FirestoreManager<User> firestoreManager;

    public FollowManager(FirebaseFirestore db, FirebaseAuthManager authManager) {
        this.db = db;
        this.authManager = authManager;
        this.firestoreManager = new FirestoreManager<>(db, User.class);
    }

    public void toggleFollowStatus(String profileUserId, FollowCallback callback) {
        String currentUserId = authManager.getCurrentUser().getUid();
        checkFollowingStatus(currentUserId, profileUserId, new FollowStatusCallback() {
            @Override
            public void onResult(boolean isFollowing) {
                if (isFollowing) {
                    unfollowUser(currentUserId, profileUserId, callback);
                } else {
                    followUser(currentUserId, profileUserId, callback);
                }
            }
        });
    }

    public void checkFollowingStatus(String currentUserId, String profileUserId, FollowStatusCallback callback) {
        firestoreManager.readDocument("Users", currentUserId, new FirestoreManager.FirestoreReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                List<String> following = user.getFollowing();
                boolean isFollowing = following != null && following.contains(profileUserId);
                callback.onResult(isFollowing);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onResult(false);
            }
        });
    }
/*
    private void followUser(String followerId, String followedId, FollowCallback callback) {
        // Update followed user's followers array
        db.collection("Users").document(followedId)
                .update("followers", com.google.firebase.firestore.FieldValue.arrayUnion(followerId))
                .addOnSuccessListener(aVoid -> {
                    // Update follower's following array
                    db.collection("Users").document(followerId)
                            .update("following", com.google.firebase.firestore.FieldValue.arrayUnion(followedId))
                            .addOnSuccessListener(aVoid1 -> callback.onSuccess(true))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void unfollowUser(String followerId, String followedId, FollowCallback callback) {
        // Remove from followed user's followers array
        db.collection("Users").document(followedId)
                .update("followers", com.google.firebase.firestore.FieldValue.arrayRemove(followerId))
                .addOnSuccessListener(aVoid -> {
                    // Remove from follower's following array
                    db.collection("Users").document(followerId)
                            .update("following", com.google.firebase.firestore.FieldValue.arrayRemove(followedId))
                            .addOnSuccessListener(aVoid1 -> callback.onSuccess(false))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
*/
    private void followUser(String followerId, String followedId, FollowCallback callback) {
        // First add the follower to the followed user's followers array
        firestoreManager.addToArray("Users", followedId, "followers", followerId,
                new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        // Then add the followed user to the follower's following array
                        firestoreManager.addToArray("Users", followerId, "following", followedId,
                                new FirestoreManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onSuccess(true);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // If adding to following fails, we should roll back the first operation
                                        firestoreManager.removeFromArray("Users", followedId, "followers", followerId,
                                                new FirestoreManager.FirestoreCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        // Rollback successful, but original operation failed
                                                        callback.onFailure(e);
                                                    }

                                                    @Override
                                                    public void onFailure(Exception rollbackException) {
                                                        // Both operations failed, data is in inconsistent state
                                                        Log.e("FollowManager", "Failed to rollback after follow failure",
                                                                rollbackException);
                                                        callback.onFailure(e);
                                                    }
                                                });
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
    }

    private void unfollowUser(String followerId, String followedId, FollowCallback callback) {
        // First remove the follower from the followed user's followers array
        firestoreManager.removeFromArray("Users", followedId, "followers", followerId,
                new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        // Then remove the followed user from the follower's following array
                        firestoreManager.removeFromArray("Users", followerId, "following", followedId,
                                new FirestoreManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onSuccess(false);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // If removing from following fails, we should roll back the first operation
                                        firestoreManager.addToArray("Users", followedId, "followers", followerId,
                                                new FirestoreManager.FirestoreCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        // Rollback successful, but original operation failed
                                                        callback.onFailure(e);
                                                    }

                                                    @Override
                                                    public void onFailure(Exception rollbackException) {
                                                        // Both operations failed, data is in inconsistent state
                                                        Log.e("FollowManager", "Failed to rollback after unfollow failure",
                                                                rollbackException);
                                                        callback.onFailure(e);
                                                    }
                                                });
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
    }

    // Interfaces remain the same
    public interface FollowCallback {
        void onSuccess(boolean isFollowing);
        void onFailure(Exception e);
    }

    public interface FollowStatusCallback {
        void onResult(boolean isFollowing);
    }
}
