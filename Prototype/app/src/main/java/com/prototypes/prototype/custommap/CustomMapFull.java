package com.prototypes.prototype.custommap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.gms.maps.MapView;
import com.prototypes.prototype.R;
import com.prototypes.prototype.explorePage.MapManager;

public class CustomMapFull extends AppCompatActivity {
    ImageButton btnExit;
    private MapView mapView;
    private static final String TAG = "Custom Map Full";
    MapManager mapManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_custom_map_full);
        btnExit = findViewById(R.id.btnExit);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(map -> {
            mapManager.initMap(map);
        });
        mapManager = new MapManager(this, this, getSupportFragmentManager());

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}