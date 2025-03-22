package com.prototypes.prototype.story;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.prototypes.prototype.R;

public class StoryViewFragment extends Fragment {

    public static StoryViewFragment newInstance(String userId, String caption, String mediaUrl) {
        StoryViewFragment fragment = new StoryViewFragment();
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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

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


}
