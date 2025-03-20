package com.prototypes.prototype;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

public class StoryViewDialogFragment extends DialogFragment {

    public static StoryViewDialogFragment newInstance(String userId, String caption, String mediaUrl) {
        StoryViewDialogFragment fragment = new StoryViewDialogFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putString("caption", caption);
        args.putString("mediaUrl", mediaUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return inflater.inflate(R.layout.fragment_story_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView mediaView = view.findViewById(R.id.story_image);
        TextView captionTextView = view.findViewById(R.id.story_snippet);

        if (getArguments() != null) {
            String caption = getArguments().getString("caption");
            String imageUrl = getArguments().getString("mediaUrl");

            captionTextView.setText(caption);

            // Load image with Glide with improved configuration
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerInside();

            Glide.with(requireContext())
                    .load(imageUrl)
                    .apply(options)
                    .into(mediaView);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }
}
