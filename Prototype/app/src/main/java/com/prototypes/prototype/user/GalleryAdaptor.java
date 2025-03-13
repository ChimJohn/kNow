package com.prototypes.prototype.user;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.prototypes.prototype.ProfileFragment;
import com.prototypes.prototype.R;
import com.prototypes.prototype.media.Stories;

import java.util.ArrayList;

public class GalleryAdaptor extends RecyclerView.Adapter<GalleryAdaptor.GalleryViewHolder> {

    Context context;
    ArrayList<Stories> storiesArrayList;

    public GalleryAdaptor(Context context, ArrayList<Stories> storiesArrayList) {
        this.context = context;
        this.storiesArrayList = storiesArrayList;
    }

    @NonNull
    @Override
    public GalleryAdaptor.GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.gallery_item, parent, false);
        return new GalleryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryAdaptor.GalleryViewHolder holder, int position) {
        Stories story = storiesArrayList.get(position);
        int totalWidth = holder.itemView.getContext().getResources().getDisplayMetrics().widthPixels;
        int itemWidth = totalWidth / 3;

        int spacing = (int) (6 * holder.itemView.getContext().getResources().getDisplayMetrics().density);


        ViewGroup.LayoutParams layoutParams = holder.galleryImg.getLayoutParams();
        layoutParams.width = itemWidth;

        // Set margins for spacing between items
        RecyclerView.LayoutParams recyclerViewParams = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
        recyclerViewParams.setMargins(spacing, spacing, spacing, spacing);  // Add margins

        holder.galleryImg.setLayoutParams(layoutParams);
        holder.itemView.setLayoutParams(recyclerViewParams);


        Glide.with(context)
                .load(story.getImageUrl())
                .into(holder.galleryImg); //TODO: add buffering img
    }

    @Override
    public int getItemCount() {
        return storiesArrayList.size();
    }

    public static class GalleryViewHolder extends  RecyclerView.ViewHolder{
        ImageView galleryImg;
        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            galleryImg = itemView.findViewById(R.id.ivGallery);
        }
    }
}
