package com.prototypes.prototype;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

public class StoryUploadFragment extends Fragment {
    private ImageView imageView;
    private EditText captionEditText;
    private Button saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_story_upload, container, false);

        imageView = rootView.findViewById(R.id.imageView);
        captionEditText = rootView.findViewById(R.id.captionEditText);
        saveButton = rootView.findViewById(R.id.saveButton);

        // Get the photo URI from arguments
        String photoUriString = getArguments().getString("photoUri", "");
        Uri photoUri = Uri.parse(photoUriString);

        // Load the image into the ImageView
        Glide.with(requireContext()) // Using Glide to load the image
                .load(photoUri)
                .into(imageView);

        // Handle save button click
        saveButton.setOnClickListener(v -> {
            String caption = captionEditText.getText().toString();

            // Save the title and description (you can save it in shared preferences, a database, or send it to a server)
            savePhotoDetails(caption);

            // Optionally, show a Toast or confirmation dialog
            Toast.makeText(requireContext(), "Photo details saved", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return rootView;
    }

    private void savePhotoDetails(String caption) {
        // Logic to save the title and description (can be saved in SharedPreferences, database, etc.)
        // For example, you can store the data using SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("photoDetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("caption", caption);
        editor.apply();
    }
}
