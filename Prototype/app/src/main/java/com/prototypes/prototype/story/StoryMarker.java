package com.prototypes.prototype.story;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

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

    public void setMarkerText(String text) {
//        textView.setText(text);
    }

    public void setMarkerImage(@DrawableRes int drawableRes) {
        imageView.setImageResource(drawableRes);
    }

    // Convert View to Bitmap (for efficient Google Maps rendering)
    public Bitmap getMarkerBitmap() {
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

}
