package com.prototypes.prototype.custommap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.prototypes.prototype.MainActivity;
import com.prototypes.prototype.R;


public class CreateCustomMap extends AppCompatActivity {
    CardView cvExit;
    EditText etMapName;
    TextView tvAdd, tvSetCover;
    ImageView imgCover;
    Uri image;
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
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_custom_map);
        Log.d(TAG, "User page.");

        cvExit = findViewById(R.id.cvExit);
        etMapName = findViewById(R.id.etMapName);
        tvAdd = findViewById(R.id.tvAdd);
        tvSetCover = findViewById(R.id.tvSetCover);
        imgCover = findViewById(R.id.ivCustomMap);

        // Destory activity when x button is pressed
        cvExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Creates Custom Map
        tvAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etMapName.getText().toString().trim().equals("")){
                    Toast.makeText(CreateCustomMap.this, "Fill in map name!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Select picture for cover img in gallery
        tvSetCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Set cover clicked.");
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                activityResultLauncher.launch(intent);
            }
        });
    }
}