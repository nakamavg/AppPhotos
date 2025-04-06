package com.example.nemergentprueba.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
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

public abstract class Camera {
    private static final String TAG = "Camera";

    protected Context context;
    protected PreviewView viewFinder;
    protected ImageCapture imageCapture;
    protected ProcessCameraProvider cameraProvider;
    protected int lensFacing;
    protected boolean initialized = false;

    public Camera(Context context, PreviewView viewFinder, int lensFacing) {
        this.context = context;
        this.viewFinder = viewFinder;
        this.lensFacing = lensFacing;
        
        // Configuración más básica para evitar problemas
        this.imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(viewFinder.getDisplay().getRotation())
                .build();
    }

    public void startCamera(LifecycleOwner lifecycleOwner) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(lifecycleOwner);
                initialized = true;
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
                initialized = false;
            }
        }, ContextCompat.getMainExecutor(context));
    }

    protected void bindCameraUseCases(LifecycleOwner lifecycleOwner) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        cameraProvider.unbindAll();

        try {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera use cases: " + e.getMessage());
            initialized = false;
        }
    }

    /**
     * Método simplificado para capturar foto, enfocado en compatibilidad máxima
     */
    public void capturePhoto(File outputDirectory, Executor executor) {
        if (imageCapture == null) {
            Toast.makeText(context, "La cámara no está inicializada", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File photoFile = createTempJpegFile();
            
            ImageCapture.OutputFileOptions outputOptions = 
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            Toast.makeText(context, "Procesando imagen...", Toast.LENGTH_SHORT).show();
            
            imageCapture.takePicture(
                outputOptions,
                executor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        try {
                            saveImageToGallery(photoFile);
                            
                            if (photoFile.exists()) {
                                photoFile.delete();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error guardando en galería: " + e.getMessage(), e);
                            Toast.makeText(context, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Error al capturar la imagen: " + exception.getMessage(), exception);
                        Toast.makeText(context, "Error al tomar la foto", Toast.LENGTH_SHORT).show();
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error general al tomar foto: " + e.getMessage(), e);
            Toast.makeText(context, "Error al preparar la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Crea un archivo temporal para almacenar la foto
     */
    private File createTempJpegFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "JPEG_" + timeStamp + "_";
        
        File storageDir = context.getCacheDir();
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    /**
     * Guarda la imagen en la galería del dispositivo desde un archivo
     */
    private void saveImageToGallery(File imageFile) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        if (bitmap == null) {
            throw new IOException("No se pudo decodificar la imagen desde el archivo");
        }

        String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera");
            
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (imageUri == null) {
                throw new IOException("No se pudo crear la URI para guardar la imagen");
            }
            
            try (OutputStream outputStream = resolver.openOutputStream(imageUri)) {
                if (outputStream == null) {
                    throw new IOException("No se pudo abrir el flujo de salida para la URI");
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            }
            
            String successMsg = "Foto guardada en la galería";
            Log.d(TAG, successMsg + ": " + imageUri);
            Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show();
        } else {
            File pictureFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File destinationFile = new File(pictureFolder, "Camera/" + fileName);
            
            try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            }
            
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(destinationFile);
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);
            
            String successMsg = "Foto guardada en la galería";
            Log.d(TAG, successMsg + ": " + destinationFile.getAbsolutePath());
            Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show();
        }
    }

    public void shutdown() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        initialized = false;
    }
    
    public boolean isInitialized() {
        return initialized && cameraProvider != null;
    }
}