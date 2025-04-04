package com.prototypes.prototype;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
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
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.Quality;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoRecordEvent;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TakePhotoFragment extends Fragment {

    private Preview preview;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture; // VideoCapture instance
    private CameraSelector cameraSelector;
    private Camera camera;
    private ExecutorService cameraExecutor;

    private androidx.camera.view.PreviewView cameraPreview;
    private ImageButton btnCapture, btnFlash, btnFlip, btnRecord;

    private boolean isFlashOn = false;
    private boolean isFrontCamera = false;
    private boolean isRecording = false;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    private final Handler videoHandler = new Handler(); // Handler for running video timer
    private TextView timerText;

    private static final long MAX_VIDEO_DURATION = 60000; // 60 seconds
    private File videoFile; // Define globally to access after stopping recording

    private long videoStartTime;
    private final Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private Recording currentRecording;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);

        cameraPreview = rootView.findViewById(R.id.cameraPreview);
        btnCapture = rootView.findViewById(R.id.btnCapture);
        btnFlash = rootView.findViewById(R.id.btnFlash);
        btnFlip = rootView.findViewById(R.id.btnFlip);
        btnRecord = rootView.findViewById(R.id.btnRecord); // Button to start/stop recording

        timerText = rootView.findViewById(R.id.timerText);

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

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startCamera();
        }


        // Capture Photo
        btnCapture.setOnClickListener(v -> takePhoto());

        // Toggle Flash
        btnFlash.setOnClickListener(v -> toggleFlash());

        // Flip Camera
        btnFlip.setOnClickListener(v -> flipCamera());

        // Start/Stop Video Recording
        btnRecord.setOnClickListener(v -> {
            if (isRecording) {
                stopVideoRecording();
            } else {
                startVideoRecording();
            }
        });

        return rootView;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireActivity());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder); // ✅ Correct

                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(isFrontCamera ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();

                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture); // Bind videoCapture

                cameraPreview.getSurfaceProvider();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

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

    private void startVideoRecording() {
        btnCapture.setVisibility(View.GONE);
        if (videoCapture == null) {
            Toast.makeText(requireContext(), "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        videoFile = new File(requireContext().getExternalFilesDir(null),
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".mp4");

        // Set up video output options
        FileOutputOptions outputOptions = new FileOutputOptions.Builder(videoFile).build();
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        currentRecording = videoCapture.getOutput()
                .prepareRecording(requireContext(), outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext()), videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                        Log.d("UploadFragment", "Video recording started.");
                        isRecording = true;
//                        btnRecord.setText("Stop Recording");

                    } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        Log.d("UploadFragment", "Video recording finalized.");
                        isRecording = false;
//                        btnRecord.setText("Start Recording");
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;
                        if (videoFile != null) {
                            openStoryUploadFragment(Uri.fromFile(videoFile)); // Transition to StoryUploadFragment
                        }
                    }
                });
        videoStartTime = System.currentTimeMillis();
        btnRecord.setImageResource(R.drawable.shutter_video);
        // Set button size to 66dp by 66dp
        ViewGroup.LayoutParams params = btnRecord.getLayoutParams();
        params.width = (int) (66 * getResources().getDisplayMetrics().density);
        params.height = (int) (66 * getResources().getDisplayMetrics().density);
        btnRecord.setLayoutParams(params);

        // Add app:layout_constraintStart_toStartOf="parent" programmatically
        if (btnRecord.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams constraintParams = (ConstraintLayout.LayoutParams) btnRecord.getLayoutParams();
            constraintParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;  // Set start constraint to parent
            constraintParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;  // Set end constraint to parent
            btnRecord.setLayoutParams(constraintParams);
        }

        // Change icon to stop button
        timerText.setVisibility(View.VISIBLE);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = (System.currentTimeMillis() - videoStartTime) / 1000;
                timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", elapsed / 60, elapsed % 60));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
        videoHandler.postDelayed(videoStopRunnable, MAX_VIDEO_DURATION);
    }


    private void stopVideoRecording() {
        if (currentRecording != null) {
            currentRecording.stop();
            videoHandler.removeCallbacks(videoStopRunnable);
            isRecording = false;
            if (videoFile != null) {
                openStoryUploadFragment(Uri.fromFile(videoFile)); // Open StoryUploadFragment with saved video
            }
            stopUIUpdates();
//            btnRecord.setText("Start Recording");
        }
    }
    private void stopUIUpdates() {
        isRecording = false;
        btnRecord.setImageResource(R.drawable.home_icon); // Change back to record button
        timerHandler.removeCallbacks(timerRunnable);
        timerText.setVisibility(View.GONE);
        btnCapture.setVisibility(View.VISIBLE);
    }

    private final Runnable videoStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopVideoRecording();
            Toast.makeText(requireContext(), "Video recording stopped after 60 seconds", Toast.LENGTH_SHORT).show();
        }
    };

    private void openStoryUploadFragment(Uri mediaUri) {
        StoryUploadFragment uploadFragment = StoryUploadFragment.newInstance(mediaUri);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, uploadFragment)
                .addToBackStack(null)
                .commit();
    }

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
        videoHandler.removeCallbacks(videoStopRunnable); // Remove any pending video stop runnable
    }
}
