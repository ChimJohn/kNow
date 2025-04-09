package com.prototypes.prototype.explorePage;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.prototypes.prototype.R;

public class StoryMarker extends LinearLayout {
    public StoryMarker(Context context) {
        super(context);
        init(context);
    }
    public StoryMarker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.story_marker, this, true);
    }
    public void setMarkerImage(Context context, String imageUrl, CustomTarget<Bitmap> target) {
        Glide.with(context)
                .asBitmap()
                .override(250, 250) // Resize the image to fit marker
                .load(imageUrl)
                .into(target);
    }
}
