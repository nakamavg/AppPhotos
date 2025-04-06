package com.example.nemergentprueba.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.nemergentprueba.R;
import com.example.nemergentprueba.database.PhotoEntity;
import com.example.nemergentprueba.database.PhotoRepository;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Camera {
    private static final String TAG = "Camera";

    protected Context context;
    protected PreviewView viewFinder;
    protected ImageCapture imageCapture;
    protected ProcessCameraProvider cameraProvider;
    protected int lensFacing;
    protected boolean initialized = false;
    protected final AtomicBoolean isCapturing = new AtomicBoolean(false);
    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    private PhotoRepository photoRepository;
    private Location currentLocation;

    public Camera(Context context, PreviewView viewFinder, int lensFacing) {
        this.context = context;
        this.viewFinder = viewFinder;
        this.lensFacing = lensFacing;

        photoRepository = new PhotoRepository(context);
        this.imageCapture = createImageCaptureUseCase();
    }

    private ImageCapture createImageCaptureUseCase() {
        ImageCapture.Builder builder = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(90);
                
        if (viewFinder != null && viewFinder.getDisplay() != null) {
            builder.setTargetRotation(viewFinder.getDisplay().getRotation());
        }
        
        return builder.build();
    }

    public void startCamera(LifecycleOwner lifecycleOwner) {
        isCapturing.set(false);

        if (viewFinder == null || viewFinder.getDisplay() == null) {
            Log.e(TAG, "ViewFinder no está disponible o no tiene display");
            Toast.makeText(context, R.string.camera_not_initialized, Toast.LENGTH_SHORT).show();
            initialized = false;
            return;
        }

        Log.d(TAG, "Iniciando la cámara...");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                if (cameraProvider != null) {
                    try {
                        cameraProvider.unbindAll();
                    } catch (Exception e) {
                        Log.e(TAG, "Error al liberar cameraProvider existente", e);
                    }
                }

                cameraProvider = cameraProviderFuture.get();

                if (imageCapture == null) {
                    imageCapture = createImageCaptureUseCase();
                }

                bindCameraUseCases(lifecycleOwner);
                initialized = true;
                Log.d(TAG, "Cámara iniciada correctamente");
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage(), e);
                initialized = false;
                Toast.makeText(context, R.string.camera_initialization_error, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error inesperado al iniciar la cámara: " + e.getMessage(), e);
                initialized = false;
                Toast.makeText(context, R.string.camera_initialization_error, Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    protected void bindCameraUseCases(LifecycleOwner lifecycleOwner) {
        try {
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build();

            Preview preview = new Preview.Builder()
                    .setTargetRotation(viewFinder.getDisplay().getRotation())
                    .build();

            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

            cameraProvider.unbindAll();

            cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture);

            Log.d(TAG, "Casos de uso de cámara vinculados correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera use cases: " + e.getMessage(), e);
            initialized = false;
            Toast.makeText(context, R.string.camera_binding_error, Toast.LENGTH_SHORT).show();
        }
    }

    public void updateLocation(Location location) {
        this.currentLocation = location;
        Log.d(TAG, context.getString(R.string.location_update_received, 
                location.getLatitude(), location.getLongitude()));
    }

    public void capturePhoto(File outputDirectory, Executor executor) {
        if (isCapturing.getAndSet(true)) {
            Log.d(TAG, "Ya hay una captura en proceso, ignorando esta solicitud");
            Toast.makeText(context, R.string.wait_for_processing, Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageCapture == null) {
            Log.e(TAG, "imageCapture es null, reiniciando componente");
            isCapturing.set(false);
            imageCapture = createImageCaptureUseCase();
            Toast.makeText(context, R.string.camera_not_initialized, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!initialized || cameraProvider == null) {
            Log.e(TAG, "Cámara no inicializada o provider es null");
            isCapturing.set(false);
            Toast.makeText(context, R.string.camera_not_initialized, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File photoFile = createTempJpegFile();
            if (photoFile == null) {
                Log.e(TAG, "No se pudo crear el archivo temporal");
                isCapturing.set(false);
                Toast.makeText(context, R.string.error_storage_preparation, Toast.LENGTH_SHORT).show();
                return;
            }

            ImageCapture.OutputFileOptions outputOptions =
                    new ImageCapture.OutputFileOptions.Builder(photoFile)
                    .build();

            Toast.makeText(context, R.string.processing_image, Toast.LENGTH_SHORT).show();

            final Date captureDate = new Date();

            imageCapture.takePicture(
                    outputOptions,
                    executor,
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                            try {
                                Log.d(TAG, "Imagen capturada correctamente, guardando en galería");
                                String relativePath = saveImageToGallery(photoFile, captureDate);

                                savePhotoInfoToDatabase(relativePath, captureDate);

                                cleanupTempFile(photoFile);

                                resetImageCaptureIfNeeded();
                            } catch (Exception e) {
                                Log.e(TAG, "Error en procesamiento post-captura: " + e.getMessage(), e);
                                Toast.makeText(context, R.string.error_processing_image, Toast.LENGTH_SHORT).show();
                            } finally {
                                isCapturing.set(false);
                            }
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Error al capturar imagen: " + exception.getMessage(), exception);
                            Toast.makeText(context, R.string.error_taking_photo, Toast.LENGTH_SHORT).show();

                            cleanupTempFile(photoFile);
                            resetImageCaptureIfNeeded();
                            isCapturing.set(false);
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error general al preparar captura: " + e.getMessage(), e);
            Toast.makeText(context, R.string.error_preparing_camera, Toast.LENGTH_SHORT).show();
            isCapturing.set(false);

            mainHandler.postDelayed(this::attemptRecovery, 500);
        }
    }

    private void resetImageCaptureIfNeeded() {
        try {
            mainHandler.post(() -> {
                try {
                    if (isCapturing.get()) {
                        Log.d(TAG, "Recreando ImageCapture para siguiente foto");
                        imageCapture = createImageCaptureUseCase();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al recrear ImageCapture", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error al programar recreación de ImageCapture", e);
        }
    }

    private void cleanupTempFile(File photoFile) {
        if (photoFile != null && photoFile.exists()) {
            try {
                if (!photoFile.delete()) {
                    Log.w(TAG, "No se pudo eliminar el archivo temporal: " + photoFile.getAbsolutePath());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al eliminar archivo temporal", e);
            }
        }
    }

    private void attemptRecovery() {
        mainHandler.post(() -> {
            Log.d(TAG, "Intentando recuperación de la cámara después de error");

            if (imageCapture != null) {
                imageCapture = createImageCaptureUseCase();
            }

            isCapturing.set(false);
            initialized = false;

            if (context != null && viewFinder != null && viewFinder.getDisplay() != null) {
                if (cameraProvider != null) {
                    try {
                        cameraProvider.unbindAll();
                    } catch (Exception e) {
                        Log.e(TAG, "Error en unbindAll durante recuperación", e);
                    }
                }
            }
        });
    }

    private void savePhotoInfoToDatabase(String relativePath, Date captureDate) {
        double latitude = 0.0;
        double longitude = 0.0;
        Float accuracy = null;

        if (currentLocation != null) {
            latitude = currentLocation.getLatitude();
            longitude = currentLocation.getLongitude();
            accuracy = currentLocation.hasAccuracy() ? currentLocation.getAccuracy() : null;
        }

        PhotoEntity photoEntity = new PhotoEntity(
                captureDate,
                relativePath,
                latitude,
                longitude,
                accuracy
        );

        photoRepository.insertPhoto(photoEntity, savedPhoto -> {
            Log.d(TAG, "Foto guardada en base de datos con ID: " + savedPhoto.getId());
        });
    }

    private File createTempJpegFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "JPEG_" + timeStamp + "_";

        File storageDir = context.getCacheDir();
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private String saveImageToGallery(File imageFile, Date captureDate) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        if (bitmap == null) {
            throw new IOException(context.getString(R.string.error_decoding_image));
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(captureDate);
        String fileName = "IMG_" + timeStamp + ".jpg";
        String relativePath = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera");

            relativePath = Environment.DIRECTORY_DCIM + "/Camera/" + fileName;

            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (imageUri == null) {
                throw new IOException(context.getString(R.string.error_creating_uri));
            }

            try (OutputStream outputStream = resolver.openOutputStream(imageUri)) {
                if (outputStream == null) {
                    throw new IOException(context.getString(R.string.error_opening_output_stream));
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            }

            Log.d(TAG, context.getString(R.string.photo_saved_gallery) + ": " + imageUri);
            Toast.makeText(context, R.string.photo_saved_gallery, Toast.LENGTH_SHORT).show();
        } else {
            File pictureFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File destinationFile = new File(pictureFolder, "Camera/" + fileName);
            relativePath = "DCIM/Camera/" + fileName;

            try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            }

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(destinationFile);
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);

            Log.d(TAG, context.getString(R.string.photo_saved_gallery) + ": " + destinationFile.getAbsolutePath());
            Toast.makeText(context, R.string.photo_saved_gallery, Toast.LENGTH_SHORT).show();
        }

        return relativePath;
    }

    public void shutdown() {
        Log.d(TAG, "Liberando recursos de la cámara");

        try {
            if (cameraProvider != null) {
                try {
                    cameraProvider.unbindAll();
                } catch (Exception e) {
                    Log.e(TAG, "Error al liberar cameraProvider", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error general en shutdown", e);
        } finally {
            isCapturing.set(false);
            initialized = false;
        }
    }

    public boolean isInitialized() {
        return initialized && cameraProvider != null && imageCapture != null;
    }

    public boolean isCapturing() {
        return isCapturing.get();
    }
}