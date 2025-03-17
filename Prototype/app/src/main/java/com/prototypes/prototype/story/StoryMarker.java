package com.prototypes.prototype.story;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Glide;
import com.prototypes.prototype.R;

public class StoryMarker extends LinearLayout{
    private TextView textView;
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
//        textView = findViewById(R.id.tv_marker_text);
        imageView = findViewById(R.id.iv_marker_icon);
    }

    // Asynchronously load image into the ImageView and return a bitmap
    // Directly set the image to the marker
    public void setMarkerImage(Context context, String imageUrl, CustomTarget<Bitmap> target) {
        Glide.with(context)
                .asBitmap()
                .override(250, 250) // Resize the image to your desired size
                .load(imageUrl)
                .into(target);
    }

    // Convert the view into a bitmap (used for the marker icon)
//    public Bitmap getMarkerBitmap() {
//        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
//        layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
//        Bitmap bitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        draw(canvas);
//        return bitmap;
//    }
}
