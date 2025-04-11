package com.prototypes.prototype.user;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.R;
import com.prototypes.prototype.firebase.FirestoreManager;

import java.util.ArrayList;
import java.util.List;

public class FollowersListFragment extends Fragment {

    private static final String ARG_USER_IDS = "user_ids";
    private static final String ARG_TITLE = "title";

    private List<String> userIds;
    private String title;

    public static FollowersListFragment newInstance(ArrayList<String> userIds, String title) {
        FollowersListFragment fragment = new FollowersListFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_USER_IDS, userIds);
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userIds = getArguments().getStringArrayList(ARG_USER_IDS);
        title = getArguments().getString(ARG_TITLE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_followers_list, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbarFollowersList);
        toolbar.setTitle(title);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        RecyclerView recyclerView = view.findViewById(R.id.recyclerFollowersList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirestoreManager<User> manager = new FirestoreManager<>(db, User.class);
        FollowersListAdapter adapter = new FollowersListAdapter(userIds, manager, getActivity());
        recyclerView.setAdapter(adapter);

        return view;
    }
}