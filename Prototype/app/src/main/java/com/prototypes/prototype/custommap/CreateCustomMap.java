package com.prototypes.prototype.custommap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.prototypes.prototype.R;


public class CreateCustomMap extends AppCompatActivity {
    ImageButton btnExit;
    EditText etMapName;
    TextView tvAdd, tvSetCover;
    ImageView imgCover;
    Uri image;
    LinearLayout loadingLayout;
    LinearLayout formLayout;
    private static final String TAG = "Create Custom Map Activity";

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                if (result.getData() != null) {
                    image = result.getData().getData();
                    Glide.with(getApplicationContext()).load(image).into(imgCover);
                }
            } else {
                Toast.makeText(CreateCustomMap.this, "Please select an image", Toast.LENGTH_SHORT).show();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_custom_map);
        btnExit = findViewById(R.id.btnExit);
        etMapName = findViewById(R.id.etMapName);
        tvAdd = findViewById(R.id.tvAdd);
        tvSetCover = findViewById(R.id.tvSetCover);
        imgCover = findViewById(R.id.ivCustomMap);
        formLayout = findViewById(R.id.formLayout);
        loadingLayout = findViewById(R.id.loadingLayout);

        btnExit.setOnClickListener(v -> finish());

        tvAdd.setOnClickListener(v -> {
            String mapName = etMapName.getText().toString().trim();
            if (mapName.isEmpty()) {
                Toast.makeText(CreateCustomMap.this, "Fill in map name!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (image == null) {
                Toast.makeText(CreateCustomMap.this, "Select cover image!", Toast.LENGTH_SHORT).show();
                return;
            }

            formLayout.setVisibility(View.GONE);
            loadingLayout.setVisibility(View.VISIBLE);
            CustomMap.createMap(CreateCustomMap.this, image, mapName, success -> {
                if (success) {
                    Toast.makeText(CreateCustomMap.this, "Map created!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateCustomMap.this, "Failed to create map.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        tvSetCover.setOnClickListener(v -> {
            Log.d(TAG, "Set cover clicked.");
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            activityResultLauncher.launch(intent);
        });
    }
}



