package com.prototypes.prototype.story;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.prototypes.prototype.ExploreFragment;
import com.prototypes.prototype.R;

import java.util.ArrayList;
import java.util.List;

public class StoryViewFragment extends Fragment implements StoryViewAdapter.OnGpsClickListener{
    private ArrayList<Story> storyList;
    private ViewPager2 viewPager2;
    private StoryViewAdapter storyViewAdapter;
    public static StoryViewFragment newInstance(List<Story> stories) {
        StoryViewFragment fragment = new StoryViewFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("stories", new ArrayList<>(stories));
        fragment.setArguments(bundle);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            storyList = getArguments().getParcelableArrayList("stories");
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_story_view, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewPager2 = view.findViewById(R.id.storyViewPager);
        storyViewAdapter = new StoryViewAdapter(getContext(), storyList, this);
        viewPager2.setAdapter(storyViewAdapter);
        viewPager2.setOffscreenPageLimit(2);
        preloadMedia();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }
        }
        if (storyViewAdapter != null) {
            storyViewAdapter.removeAutoScrollCallbacks();
        }
    }
    private void preloadMedia() {
        if (storyList == null || storyList.isEmpty()) return;
        for (Story story : storyList) {
            if (!story.isVideo()) {
                Glide.with(requireContext()).load(story.getMediaUrl()).preload();
            }
        }
    }
    @Override
    public void onGpsClick(double latitude, double longitude) {
        Log.d("StoryViewFragment", "GPS Clicked: " + latitude + ", " + longitude);
        ExploreFragment exploreFragment = ExploreFragment.newInstance(latitude, longitude);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, exploreFragment)
                .addToBackStack(null)
                .commit();
    }
}
