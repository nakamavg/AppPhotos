package com.example.nemergentprueba.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.nemergentprueba.R;
import com.example.nemergentprueba.location.LocationService;
import com.example.nemergentprueba.utils.PermissionHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements LocationService.LocationListener {
    private static final String TAG = "CameraActivity";
    private static final int MAX_RESTART_ATTEMPTS = 3;

    private String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        
        return permissions.toArray(new String[0]);
    }

    private Camera currentCamera;
    private PreviewView viewFinder;
    private FloatingActionButton captureButton;
    private FloatingActionButton switchCameraButton;
    
    private boolean isFrontCamera = false;
    private ExecutorService cameraExecutor;
    private int restartAttempts = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable cameraRestartRunnable;
    
    private LocationService locationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewFinder = findViewById(R.id.viewFinder);
        captureButton = findViewById(R.id.capture_button);
        switchCameraButton = findViewById(R.id.switch_camera_button);

        captureButton.setEnabled(false);
        switchCameraButton.setEnabled(false);
        
        captureButton.setOnClickListener(view -> takePhoto());
        switchCameraButton.setOnClickListener(view -> toggleCamera());

        cameraExecutor = Executors.newSingleThreadExecutor();
        locationService = new LocationService(this);
        cameraRestartRunnable = this::restartCamera;
        
        requestCameraPermissions();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        Log.d(TAG, "onResume - Verificando estado de la cámara");
        
        restartAttempts = 0;
        mainHandler.removeCallbacks(cameraRestartRunnable);
        
        if (currentCamera != null) {
            if (!currentCamera.isInitialized() || currentCamera.isCapturing()) {
                Log.d(TAG, "Cámara en estado inestable, reiniciando");
                restartCamera();
            }
        } else if (hasRequiredPermissions()) {
            Log.d(TAG, "No hay cámara activa, iniciando una nueva");
            startCamera();
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == 
                PackageManager.PERMISSION_GRANTED) {
            locationService.startLocationUpdates(this);
        }
    }
    
    private boolean hasRequiredPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        mainHandler.removeCallbacks(cameraRestartRunnable);
        locationService.stopLocationUpdates();
    }
    
    private void requestCameraPermissions() {
        if (PermissionHelper.isXiaomiDevice()) {
            Toast.makeText(this, R.string.xiaomi_permission_guide, Toast.LENGTH_LONG).show();
        }

        if (PermissionHelper.requestPermissions(
                this,
                getRequiredPermissions(),
                PermissionHelper.PERMISSION_REQUEST_CODE)) {
            Log.d(TAG, "Permisos concedidos. Iniciando cámara y localización.");
            startCamera();
            startLocationUpdates();
            enableCameraControls();
        } else {
            Log.d(TAG, "Permisos no concedidos. Solicitando permisos al usuario.");
        }
    }
    
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == 
                PackageManager.PERMISSION_GRANTED) {
            locationService.startLocationUpdates(this);
        }
    }
    
    private void enableCameraControls() {
        captureButton.setEnabled(true);
        switchCameraButton.setEnabled(true);
    }

    private void startCamera() {
        currentCamera = new BackCamera(this, viewFinder);
        currentCamera.startCamera(this);
    }

    private void toggleCamera() {
        if (currentCamera != null) {
            currentCamera.shutdown();
        }

        isFrontCamera = !isFrontCamera;
        if (isFrontCamera) {
            currentCamera = new FrontCamera(this, viewFinder);
        } else {
            currentCamera = new BackCamera(this, viewFinder);
        }
        currentCamera.startCamera(this);
        
        if (locationService.getLastLocation() != null) {
            currentCamera.updateLocation(locationService.getLastLocation());
        }
    }

    private void takePhoto() {
        Log.d(TAG, "Intentando tomar foto...");
        
        if (currentCamera == null) {
            Log.e(TAG, "Cámara es null, creando una nueva");
            startCamera();
            
            Toast.makeText(this, R.string.camera_not_initialized, Toast.LENGTH_SHORT).show();
            mainHandler.postDelayed(this::takePhoto, 1000);
            return;
        }
        
        if (currentCamera.isCapturing()) {
            Toast.makeText(this, R.string.wait_for_processing, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!currentCamera.isInitialized()) {
            Log.d(TAG, "Cámara no inicializada, reiniciando...");
            restartCamera();
            
            if (!currentCamera.isInitialized()) {
                if (restartAttempts < MAX_RESTART_ATTEMPTS) {
                    Log.d(TAG, "Programando reintento de captura después del reinicio de cámara");
                    mainHandler.postDelayed(this::takePhoto, 1000);
                    return;
                } else {
                    Toast.makeText(this, R.string.camera_error_try_restart, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        
        File outputDirectory = getOutputDirectory();
        
        try {
            currentCamera.capturePhoto(outputDirectory, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            Log.e(TAG, "Error al llamar a capturePhoto: " + e.getMessage(), e);
            Toast.makeText(this, R.string.error_taking_photo, Toast.LENGTH_SHORT).show();
            
            if (restartAttempts < MAX_RESTART_ATTEMPTS) {
                restartAttempts++;
                Log.d(TAG, "Programando reinicio de cámara después de error (intento " + restartAttempts + ")");
                mainHandler.postDelayed(this::restartCamera, 500);
            }
        }
    }

    private File getOutputDirectory() {
        File mediaDir = new File(getExternalMediaDirs()[0], getResources().getString(R.string.app_name));
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }
        return mediaDir;
    }

    private void restartCamera() {
        Log.d(TAG, "Reiniciando cámara (intento " + restartAttempts + ")");
        
        try {
            if (currentCamera != null) {
                currentCamera.shutdown();
                currentCamera = null;
            }
            
            Thread.sleep(100);
            
            if (isFrontCamera) {
                currentCamera = new FrontCamera(this, viewFinder);
            } else {
                currentCamera = new BackCamera(this, viewFinder);
            }
            
            if (currentCamera != null) {
                currentCamera.startCamera(this);
                
                if (currentCamera.isInitialized()) {
                    Log.d(TAG, "Cámara reiniciada exitosamente");
                    
                    if (locationService != null && locationService.getLastLocation() != null) {
                        currentCamera.updateLocation(locationService.getLastLocation());
                    }
                    
                    restartAttempts = 0;
                } else if (restartAttempts < MAX_RESTART_ATTEMPTS) {
                    restartAttempts++;
                    Log.d(TAG, "Fallo al inicializar cámara, programando otro intento");
                    mainHandler.postDelayed(this::restartCamera, 500);
                } else {
                    Log.e(TAG, "No se pudo reiniciar la cámara después de múltiples intentos");
                    Toast.makeText(this, R.string.camera_error_try_restart, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error durante reinicio de cámara: " + e.getMessage(), e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PermissionHelper.PERMISSION_REQUEST_CODE) {
            if (PermissionHelper.handlePermissionResult(this, requestCode, permissions, grantResults)) {
                startCamera();
                startLocationUpdates();
                enableCameraControls();
                
                if (PermissionHelper.isXiaomiDevice() && !cameraIsWorking()) {
                    Toast.makeText(this, 
                        R.string.xiaomi_check_permissions, 
                        Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    
    private boolean cameraIsWorking() {
        return currentCamera != null && currentCamera.isInitialized();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        mainHandler.removeCallbacksAndMessages(null);
        
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        
        if (currentCamera != null) {
            currentCamera.shutdown();
            currentCamera = null;
        }
        
        if (locationService != null) {
            locationService.stopLocationUpdates();
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
    }
    
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, getString(R.string.location_update_received, 
                location.getLatitude(), location.getLongitude()));
        
        if (currentCamera != null) {
            currentCamera.updateLocation(location);
        }
    }
    
    @Override
    public void onLocationError(String error) {
        Log.e(TAG, "Error de ubicación: " + error);
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }
}