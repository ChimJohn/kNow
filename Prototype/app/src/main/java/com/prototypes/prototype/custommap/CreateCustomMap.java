package com.prototypes.prototype.custommap;

import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.prototypes.prototype.R;


public class CreateCustomMap extends AppCompatActivity {
    CardView cvExit;
    EditText etMapName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_custom_map);

    }
}