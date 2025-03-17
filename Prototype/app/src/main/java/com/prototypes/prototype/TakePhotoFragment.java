package com.prototypes.prototype;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.firestore.FirebaseFirestore;


public class TakePhotoFragment extends Fragment {

    private Preview preview;
    private ImageCapture imageCapture;
    private CameraSelector cameraSelector;
    private Camera camera;
    private ExecutorService cameraExecutor;

    private androidx.camera.view.PreviewView cameraPreview;
    private ImageButton btnCapture, btnFlash, btnFlip, btnClose;

    private boolean isFlashOn = false;
    private boolean isFrontCamera = false;

    private ActivityResultLauncher<String> requestPermissionLauncher;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_upload, container, false);

        cameraPreview = rootView.findViewById(R.id.cameraPreview);
        btnCapture = rootView.findViewById(R.id.btnCapture);
        btnFlash = rootView.findViewById(R.id.btnFlash);
        btnFlip = rootView.findViewById(R.id.btnFlip);
        btnClose = rootView.findViewById(R.id.btnClose);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Register Permission Request
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Request Camera Permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera();
        }

        // Capture Photo
        btnCapture.setOnClickListener(v -> takePhoto());

        // Toggle Flash
        btnFlash.setOnClickListener(v -> toggleFlash());

        // Flip Camera
        btnFlip.setOnClickListener(v -> flipCamera());

        // Close Camera
        btnClose.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return rootView;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireActivity());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Create a ResolutionSelector with 16:9 aspect ratio
                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        // Optional: If you want more control over resolution
                        // .setResolutionStrategy(new ResolutionStrategy(
                        //     new Size(1280, 720),
                        //     ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build();

                // Apply the same ResolutionSelector to both preview and image capture
                preview = new Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build();

                imageCapture = new ImageCapture.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(isFrontCamera ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();

                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                if (cameraPreview.getSurfaceProvider() != null) {
                    preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                } else {
                    Log.e("UploadFragment", "Camera Preview SurfaceProvider is null");
                }

            } catch (Exception e) {
                Log.e("UploadFragment", "CameraX initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(requireActivity()));
    }


    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(requireContext(), "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        File photoFile = new File(requireContext().getExternalFilesDir(null),
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        openStoryUploadFragment(savedUri);
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("UploadFragment", "Photo capture failed: " + exception.getMessage());
                    }
                }
        );
    }

    private void openStoryUploadFragment(Uri photoUri) {
        StoryUploadFragment previewFragment = StoryUploadFragment.newInstance(photoUri);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, previewFragment) // Change this to your actual container ID
                .addToBackStack(null) // Allows going back to the previous screen
                .commit();
    }

    //Actual image (not url) saved in Firebase Storage
    private void toggleFlash() {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            Toast.makeText(requireContext(), "Flash not available", Toast.LENGTH_SHORT).show();
            return;
        }
        isFlashOn = !isFlashOn;
        camera.getCameraControl().enableTorch(isFlashOn);
    }

    private void flipCamera() {
        isFrontCamera = !isFrontCamera;
        startCamera();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
    }
}