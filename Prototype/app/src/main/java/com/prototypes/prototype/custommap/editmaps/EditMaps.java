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

import java.util.ArrayList;

public class EditMaps extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Toolbar toolbar;
    RecyclerView mapsRecycler;
    private static final String TAG = "Edit Maps Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_maps);

        FirebaseAuthManager firebaseAuthManager = new FirebaseAuthManager(this);
        FirestoreManager firestoreMapManager = new FirestoreManager(db, CustomMap.class);

        toolbar = findViewById(R.id.toolbarEditMaps);
        mapsRecycler = findViewById(R.id.rvMaps);

        // Exit button
        toolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish(); // Return to ProfileFragment
        });

        firestoreMapManager.queryDocuments("map", "owner", firebaseAuthManager.getCurrentUser().getUid(), new FirestoreManager.FirestoreQueryCallback<CustomMap>() {
            @Override
            public void onEmpty(ArrayList results) {
                Log.d(TAG, "No Maps");
            }

            @Override
            public void onSuccess(ArrayList<CustomMap> results) {
                Log.d(TAG, "Success. Number of results: "+ results.size());
                Log.d(TAG, "Test: "+ results.get(1).getName());

                EditMapsAdaptor editMapsAdaptor = new EditMapsAdaptor(EditMaps.this, results);
                mapsRecycler.setAdapter(editMapsAdaptor);
            }

            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "Error getting maps: "+e.toString());
            }
        });




    }
}