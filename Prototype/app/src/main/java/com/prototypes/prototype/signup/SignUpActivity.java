package com.prototypes.prototype.signup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.prototypes.prototype.R;

public class SignUpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Reference UI elements
        EditText etUsername = findViewById(R.id.etUsername);
        Button btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();

                Intent intent = new Intent(SignUpActivity.this, SignUpFinal.class);
                intent.putExtra("username", username);
                startActivity(intent);
            }
        });
    }
}