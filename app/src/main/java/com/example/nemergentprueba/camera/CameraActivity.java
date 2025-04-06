package com.example.nemergentprueba.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.nemergentprueba.R;
import com.example.nemergentprueba.utils.PermissionHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";

    // Permisos requeridos para la cámara
    private String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        
        // Permiso de cámara siempre es necesario
        permissions.add(Manifest.permission.CAMERA);
        
        // Para Android 12 (API 31) y superior, no solicitar WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) { // R = Android 11
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Inicializar vistas
        viewFinder = findViewById(R.id.viewFinder);
        captureButton = findViewById(R.id.capture_button);
        switchCameraButton = findViewById(R.id.switch_camera_button);

        // Configurar listeners de botones (estarán deshabilitados hasta tener permisos)
        captureButton.setEnabled(false);
        switchCameraButton.setEnabled(false);
        
        captureButton.setOnClickListener(view -> takePhoto());
        switchCameraButton.setOnClickListener(view -> toggleCamera());

        // Inicializar executor para operaciones de cámara
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // Solicitar permisos usando el nuevo PermissionHelper
        requestCameraPermissions();
    }
    
    private void requestCameraPermissions() {
        // Si estamos en un dispositivo Xiaomi, mostramos un Toast con instrucciones adicionales
        if (PermissionHelper.isXiaomiDevice()) {
            Toast.makeText(this, R.string.xiaomi_permission_guide, Toast.LENGTH_LONG).show();
        }
        
        // Solicitar permisos usando el helper
        if (PermissionHelper.requestPermissions(
                this, 
                getRequiredPermissions(), 
                PermissionHelper.PERMISSION_REQUEST_CODE)) {
            // Si ya tenemos los permisos, iniciamos la cámara
            startCamera();
            enableCameraControls();
        }
    }
    
    private void enableCameraControls() {
        captureButton.setEnabled(true);
        switchCameraButton.setEnabled(true);
    }

    private void startCamera() {
        // Iniciar con cámara trasera por defecto
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
    }

    private void takePhoto() {
        if (currentCamera != null) {
            // Crear directorio para guardar fotos si no existe
            File outputDirectory = getOutputDirectory();
            currentCamera.capturePhoto(outputDirectory, ContextCompat.getMainExecutor(this));
        } else {
            Toast.makeText(this, getString(R.string.camera_not_initialized), Toast.LENGTH_SHORT).show();
        }
    }

    private File getOutputDirectory() {
        File mediaDir = new File(getExternalMediaDirs()[0], getResources().getString(R.string.app_name));
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }
        return mediaDir;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // Usar el helper para manejar el resultado de la solicitud de permisos
        PermissionHelper.handlePermissionResult(
            this,
            requestCode,
            permissions,
            grantResults,
            () -> {
                // Este código se ejecutará si todos los permisos fueron concedidos
                startCamera();
                enableCameraControls();
                
                // Verificación especial para dispositivos Xiaomi
                if (PermissionHelper.isXiaomiDevice() && !cameraIsWorking()) {
                    // Si es un Xiaomi pero la cámara sigue sin funcionar, podría necesitar
                    // configuración adicional en MIUI
                    Toast.makeText(this, 
                        "Por favor, verifique los permisos en Ajustes > Seguridad de MIUI", 
                        Toast.LENGTH_LONG).show();
                    
                    // Opcionalmente, puedes abrir directamente la pantalla de permisos de MIUI
                    // PermissionHelper.openMIUIPermissionSettings(this);
                }
            }
        );
    }
    
    // Método para verificar si la cámara está funcionando correctamente
    private boolean cameraIsWorking() {
        return currentCamera != null && currentCamera.isInitialized();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (currentCamera != null) {
            currentCamera.shutdown();
        }
    }
}