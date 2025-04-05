package com.prototypes.prototype.custommap.editmaps;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.R;
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.custommap.CustomMapAdaptor;
import com.prototypes.prototype.firebase.FirebaseAuthManager;
import com.prototypes.prototype.firebase.FirestoreManager;
import com.prototypes.prototype.user.User;

import java.util.ArrayList;

public class EditMaps extends AppCompatActivity {
    Toolbar toolbar;
    RecyclerView mapsRecycler;
    private static final String TAG = "Edit Maps Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_maps);

        toolbar = findViewById(R.id.toolbarEditMaps);
        mapsRecycler = findViewById(R.id.rvMaps);

        // Exit button
        toolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish(); // Return to ProfileFragment
        });

        // Get maps retlated to the user
        User.getMaps(this, new User.UserCallback<CustomMap>() {
            @Override
            public void onMapsLoaded(ArrayList<CustomMap> customMaps) {
                if (customMaps.size() > 0) {
                    EditMapsAdaptor editMapsAdaptor = new EditMapsAdaptor(EditMaps.this, customMaps);
                    mapsRecycler.setAdapter(editMapsAdaptor);
                } else {
                    Log.d(TAG, "No Maps");
                }
            }
            @Override
            public void onError(Exception e) {
                Log.d(TAG, "Error");
            }
        });
    }
}