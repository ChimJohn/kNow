package com.prototypes.prototype.user;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.prototypes.prototype.R;
import com.prototypes.prototype.firebase.FirestoreManager;

import java.util.ArrayList;
import java.util.List;

public class FollowersListAdapter extends RecyclerView.Adapter<FollowersListAdapter.UserViewHolder> {

    private final List<String> userIds;
    private final FirestoreManager<User> firestoreManager;
    private final FragmentActivity activity;

    public FollowersListAdapter(List<String> userIds, FirestoreManager<User> manager, FragmentActivity activity) {
        this.userIds = userIds;
        this.firestoreManager = manager;
        this.activity = activity;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_follow, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        String userId = userIds.get(position);

        firestoreManager.readDocument("Users", userId, new FirestoreManager.FirestoreReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                holder.username.setText("@" + user.getUsername());
                holder.name.setText(user.getName());
                Glide.with(holder.itemView.getContext())
                        .load(user.getProfile() == null ? R.drawable.default_profile : user.getProfile())
                        .into(holder.profileImage);

                holder.itemView.setOnClickListener(v -> {
                    activity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, UserProfileFragment.newInstance(userId))
                            .addToBackStack(null)
                            .commit();
                });
            }

            @Override
            public void onFailure(Exception e) {
                // handle error
            }
        });
    }

    @Override
    public int getItemCount() {
        return userIds.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView username, name;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.imageProfile);
            username = itemView.findViewById(R.id.tvUsername);
            name = itemView.findViewById(R.id.tvName);
        }
    }
}