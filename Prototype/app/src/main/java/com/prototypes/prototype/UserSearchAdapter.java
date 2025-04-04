package com.prototypes.prototype;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.util.Consumer;

import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private List<String> items;
    private Consumer<String> clickListener;

    public UserSearchAdapter(List<String> items, Consumer<String> clickListener) {
        this.items = items;
        this.clickListener = clickListener;
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
        String item = items.get(position);
        TextView textView = (TextView) holder.itemView;

        // If no spaces → assume it's a username → prefix with "@"
        if (!item.contains(" ")) {
            textView.setText("@" + item);
        } else {
            textView.setText(item);
        }

        holder.itemView.setOnClickListener(v -> clickListener.accept(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // 🔄 New method: update both list & click behavior
    public void updateData(List<String> newItems, Consumer<String> newClickListener) {
        this.items = newItems;
        this.clickListener = newClickListener;
        notifyDataSetChanged();
    }

    // 👇 Optional helper if you only want to update data (not listener)
    public void updateData(List<String> newItems) {
        updateData(newItems, this.clickListener);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}