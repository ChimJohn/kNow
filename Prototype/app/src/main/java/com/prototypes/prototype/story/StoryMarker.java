package com.prototypes.prototype.story;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.Glide;
import com.prototypes.prototype.R;

public class StoryMarker extends LinearLayout {
    private ImageView imageView;

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
        imageView = findViewById(R.id.iv_marker_icon);
    }

    // Asynchronously load image into the ImageView and return a bitmap
    public void setMarkerImage(Context context, String imageUrl, CustomTarget<Bitmap> target) {
        Log.d("FIRESTORE", imageUrl);
        Glide.with(context)
                .asBitmap()
                .override(250, 250) // Resize the image to fit marker
                .load(imageUrl)
                .into(target);
    }

    // Generate a bitmap for cluster markers with a count label

}
