package com.prototypes.prototype;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StoryUploadFragment extends Fragment {
    private ImageView imageView;
    private EditText captionEditText;
    private Button saveButton;
    private static final String ARG_PHOTO_URI = "photoUri";
    private Uri photoUri;

    public static StoryUploadFragment newInstance(Uri photoUri) {
        StoryUploadFragment fragment = new StoryUploadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_URI, photoUri.toString()); // Convert Uri to String
        fragment.setArguments(args);
        return fragment;
    }

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
            uploadImageToFirebaseStorage(photoUri, caption);
            Toast.makeText(requireContext(), "Uploaded!", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return rootView;
    }

    private void uploadImageToFirebaseStorage(Uri uri, String caption) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference("media/" + UUID.randomUUID().toString());

        storageReference.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storageReference.getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri downloadUri) {
                                        String url = downloadUri.toString();
                                        saveUrlToFirestore(url, caption);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("UploadFragment", "Upload failed: " + e.getMessage());
                    }
                });
    }

    //Image URL to be saved in Firestore
    private void saveUrlToFirestore(String url, String caption) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("imageUrl", url);
        data.put("caption", caption);

        db.collection("media").document()
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("UploadFragment", "Image URL saved to Firestore");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("UploadFragment", "Failed to save URL to Firestore: " + e.getMessage());
                    }
                });
    }

}
