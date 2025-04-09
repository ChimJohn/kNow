package com.prototypes.prototype.user;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.prototypes.prototype.R;
import com.prototypes.prototype.classes.Story;

import java.util.ArrayList;

public class GalleryAdaptor extends BaseAdapter {
    Context context;
    ArrayList<Story> storyArrayList;
    public GalleryAdaptor(Context context, ArrayList<Story> storyArrayList) {
        this.context = context;
        this.storyArrayList = storyArrayList;
    }
    @Override
    public int getCount() {
        return storyArrayList.size();
    }
    @Override
    public Object getItem(int position) {
        return storyArrayList.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (context == null) {
            return null;
        }
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.gallery_item, parent, false);
        }
        ImageView imageView = convertView.findViewById(R.id.ivGallery);
        Story story = storyArrayList.get(position);
        Glide.with(context)
                .load(story.getMediaUrl())
                .into(imageView);

        return convertView;
    }

}
