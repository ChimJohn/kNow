package com.prototypes.prototype.custommap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.MapView;
import com.prototypes.prototype.R;
import com.prototypes.prototype.explorePage.MapManager;

public class CustomMapFragment extends Fragment {
    private ImageButton btnExit;
    private MapView mapView;
    private MapManager mapManager;

    private static final String ARG_MAP_ID = "mapId";

    public static CustomMapFragment newInstance(String mapId) {
        CustomMapFragment fragment = new CustomMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MAP_ID, mapId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_custom_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        btnExit = view.findViewById(R.id.btnExit);
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        mapManager = new MapManager(getActivity(), getActivity(), getChildFragmentManager());

        String mapId = getArguments() != null ? getArguments().getString(ARG_MAP_ID) : null;

        mapView.getMapAsync(map -> {
            mapManager.initMap(map);
            if (mapId != null) {
                mapManager.getStoriesByMapId(mapId);
            }
        });

        btnExit.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    }

    @Override public void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override public void onPause() {
        super.onPause();
        mapView.onPause();
    }
    @Override public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    @Override public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
