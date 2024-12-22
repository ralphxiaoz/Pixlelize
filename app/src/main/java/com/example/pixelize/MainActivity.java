package com.example.pixelize;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.hardware.camera2.CameraCharacteristics;
import android.util.Size;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private CustomGLSurfaceView customGLSurfaceView;
    private SeekBar pixelationSeekBar;
    private boolean isPixelationEnabled = false;
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        customGLSurfaceView = findViewById(R.id.glSurfaceView);
        pixelationSeekBar = findViewById(R.id.pixelationSeekBar);
        MaterialButton btnTakePicture = findViewById(R.id.btn_take_picture);
        MaterialButton toggleButton = findViewById(R.id.togglePixelation);

        btnTakePicture.setOnClickListener(v -> takePicture());

        // Setup the image reader
        setupImageReader();

        // Start the camera
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        pixelationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float pixelSize = (progress + 1) / 2.0f;
                if (customGLSurfaceView != null) {
                    customGLSurfaceView.setPixelSize(pixelSize);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        toggleButton.addOnCheckedChangeListener((button, isChecked) -> {
            Log.d("ToggleButton", "Pixelation toggled: " + isChecked);
            isPixelationEnabled = isChecked;
            if (customGLSurfaceView != null) {
                customGLSurfaceView.setPixelationEnabled(isChecked);
            }
            pixelationSeekBar.setEnabled(isChecked);
        });

        pixelationSeekBar.setEnabled(false);
    }

    private void setupImageReader() {
        imageReader = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        Log.d("MainActivity", "Image received from camera");
                        Bitmap bitmap = imageToBitmap(image);
                        customGLSurfaceView.updateCameraFeed(bitmap);
                    } else {
                        Log.d("MainActivity", "No image available");
                    }
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }
        }, backgroundHandler);
    }

    private void createCameraPreview() {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(imageReader.getSurface());

            cameraDevice.createCaptureSession(
                Arrays.asList(imageReader.getSurface()),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        if (cameraDevice == null) return;
                        cameraCaptureSession = session;
                        updatePreview();
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        // Handle error
                    }

                    @Override
                    public void onClosed(@NonNull CameraCaptureSession session) {
                        super.onClosed(session);
                        Log.d("Camera", "Camera capture session closed");
                    }
                },
                backgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
            // Attempt to reopen the camera if disconnected
            if (e.getReason() == CameraAccessException.CAMERA_DISCONNECTED) {
                Log.e("Camera", "Camera disconnected, attempting to reopen");
                openCamera();
            }
        }
    }

    // You can find the logs in the Logcat window of Android Studio. 
    // Make sure to filter by the tag "MainActivity" to see the relevant log messages.
    private void openCamera() {
        Log.d("MainActivity", "Attempting to open camera");
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
            Log.d("MainActivity", "Camera opened successfully"); // Moved inside try block for clarity
        } catch (CameraAccessException e) {
            Log.e("MainActivity", "Camera access exception: " + e.getMessage()); // Changed to error log for exceptions
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void updatePreview() {
        if (cameraDevice == null) return;
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        if (customGLSurfaceView.getBitmap() != null) {
            try {
                File file = new File(getExternalFilesDir(null), "captured_image.png");
                FileOutputStream fos = new FileOutputStream(file);
                customGLSurfaceView.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (customGLSurfaceView.isAvailable()) {
            openCamera();
        } else {
            Log.d("MainActivity", "Surface is not available yet.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            }
        }
    }

    private void adjustAspectRatio(int viewWidth, int viewHeight) {
        if (customGLSurfaceView == null || cameraDevice == null) return;

        // Get the camera's aspect ratio
        int cameraWidth = 1280; // Replace with actual camera width
        int cameraHeight = 720;  // Replace with actual camera height

        float ratio = (float) cameraWidth / cameraHeight;
        if (viewWidth > viewHeight * ratio) {
            viewWidth = (int) (viewHeight * ratio);
        } else {
            viewHeight = (int) (viewWidth / ratio);
        }

        customGLSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }

    private void startCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            
            // Get the camera characteristics to determine the optimal preview size
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return;
            }

            // Choose an appropriate preview size
            Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
            int width = sizes[0].getWidth();   // Get the width from camera capabilities
            int height = sizes[0].getHeight(); // Get the height from camera capabilities

            // Now create the ImageReader with these dimensions
            ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        if (image != null) {
                            // Convert image to Bitmap
                            Bitmap bitmap = imageToBitmap(image);
                            // Pass the bitmap to the renderer
                            customGLSurfaceView.updateCameraFeed(bitmap);
                        }
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        
        // Create a YUV to RGB conversion
        YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Log.d("MainActivity", "Bitmap created with dimensions: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        return bitmap;
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Log.d("MainActivity", "Surface texture available, opening camera");
            openCamera(); // Call openCamera() here
            adjustAspectRatio(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            adjustAspectRatio(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            // No need to update anything here
        }
    };
} 