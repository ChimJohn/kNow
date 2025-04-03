package com.prototypes.prototype;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.util.Consumer;

import java.util.List;
//import java.util.function.Consumer;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(String username);
    }

    private List<String> usernames;
    private Consumer<String> listener;

    public UserSearchAdapter(List<String> usernames, Consumer<String> listener) {
        this.usernames = usernames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        String item = usernames.get(position);

        // If no spaces → assume it's a username → prefix with "@"
        if (!item.contains(" ")) {
            ((TextView) holder.itemView).setText("@" + item);
        } else {
            ((TextView) holder.itemView).setText(item);
        }

        holder.itemView.setOnClickListener(v -> listener.accept(item));
    }


    @Override
    public int getItemCount() {
        return usernames.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public void updateData(List<String> newUsernames) {
        this.usernames = newUsernames;
        notifyDataSetChanged();
    }
}

