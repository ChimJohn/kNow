package com.prototypes.prototype.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.prototypes.prototype.R;
import com.prototypes.prototype.firebase.FirestoreManager;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";
    private static final int PICK_IMAGE_REQUEST = 1001;

    private CircleImageView imageEditProfile;
    private EditText editName, editUsername;
    private TextView tvChangePhoto;
    private MaterialToolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirestoreManager<User> firestoreManager;

    private String currentUserId;
    private String originalUsername;
    private Uri selectedImageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        initFirebase();
        bindViews(view);
        loadUserProfile();
        setupListeners();
        return view;
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        firestoreManager = new FirestoreManager<>(db, User.class);
    }

    private void bindViews(View view) {
        imageEditProfile = view.findViewById(R.id.imageEditProfile);
        editName = view.findViewById(R.id.editName);
        editUsername = view.findViewById(R.id.editUsername);
        tvChangePhoto = view.findViewById(R.id.tvChangePhoto);
        toolbar = view.findViewById(R.id.toolbarEditProfile);
    }

    private void loadUserProfile() {
        User.getUserData(getActivity(), firestoreManager, new User.UserReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                editName.setText(user.getName());
                editUsername.setText(user.getUsername());
                originalUsername = user.getUsername();

                String profileUrl = user.getProfile();
                Glide.with(EditProfileFragment.this)
                        .load(profileUrl != null ? profileUrl : R.drawable.default_profile)
                        .into(imageEditProfile);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch user data", e);
            }
        });
    }

    private void setupListeners() {
        editUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newUsername = s.toString().trim();
                if (!newUsername.equals(originalUsername)) checkUsernameAvailability(newUsername);
                else editUsername.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        toolbar.setNavigationOnClickListener(v -> {
            if (isInputValid()) updateUserProfile();
            else Log.w(TAG, "Invalid input; not saving.");
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        View.OnClickListener photoClickListener = v -> showSimplePhotoOptions();
        tvChangePhoto.setOnClickListener(photoClickListener);
        imageEditProfile.setOnClickListener(photoClickListener);
    }

    private void showSimplePhotoOptions() {
        String[] options = {"Choose from Library", "Remove Current Photo"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Profile Picture")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: openGalleryPicker(); break;
                        case 1: removeProfilePhoto(); break;
                    }
                })
                .show();
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void removeProfilePhoto() {
        imageEditProfile.setImageResource(R.drawable.default_profile);
        db.collection("Users").document(currentUserId)
                .update("profile", null)
                .addOnSuccessListener(unused -> Log.d(TAG, "Profile photo removed"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to remove photo", e));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            imageEditProfile.setImageURI(selectedImageUri);
            uploadImageToFirebase();
        }
    }

    private void uploadImageToFirebase() {
        if (selectedImageUri == null || getContext() == null) return;

        String filename = "profile_" + currentUserId + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("profile/" + filename);

        Log.d(TAG, "Uploading image to: " + storageRef.getPath());

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Image uploaded. Download URL: " + uri);
                    updateProfileImageUri(uri.toString());
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload failed", e);
                    Toast.makeText(getContext(), "Failed to upload profile image", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfileImageUri(String downloadUrl) {
        if (getContext() == null){
            return;
        }
        db.collection("Users").document(currentUserId)
                .update("profile", downloadUrl)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Profile picture URL updated in Firestore");
                    Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update profile URL", e);
                    Toast.makeText(getContext(), "Failed to update profile URL", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUsernameAvailability(String username) {
        db.collection("Users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    boolean isTaken = false;
                    for (DocumentSnapshot doc : querySnapshots) {
                        if (!doc.getId().equals(currentUserId)) {
                            isTaken = true;
                            break;
                        }
                    }
                    if (isTaken) editUsername.setError("Username is already taken");
                    else editUsername.setError(null);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to check username", e));
    }

    private boolean isInputValid() {
        return editUsername.getError() == null;
    }

    private void updateUserProfile() {
        String newName = editName.getText().toString().trim();
        String newUsername = editUsername.getText().toString().trim();

        db.collection("Users").document(currentUserId)
                .update("name", newName, "username", newUsername)
                .addOnSuccessListener(unused -> Log.d(TAG, "Profile updated successfully."))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update profile", e));
    }
}