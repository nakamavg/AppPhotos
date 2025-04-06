package com.example.nemergentprueba.camera;

import android.content.Context;
import android.widget.Toast;
import androidx.camera.core.CameraSelector;
import androidx.camera.view.PreviewView;

import com.example.nemergentprueba.R;

public class BackCamera extends Camera {
    
    public BackCamera(Context context, PreviewView viewFinder) {
        super(context, viewFinder, CameraSelector.LENS_FACING_BACK);
    }
    
    /**
     * Muestra un mensaje indicando que estamos usando la cámara trasera
     */
    public void showCameraInfo() {
        Toast.makeText(context, context.getString(R.string.using_back_camera), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Método para sugerir cambio a cámara frontal
     */
    public void suggestCameraSwitch() {
        Toast.makeText(context, context.getString(R.string.switch_to_front_camera), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void startCamera(androidx.lifecycle.LifecycleOwner lifecycleOwner) {
        super.startCamera(lifecycleOwner);
        showCameraInfo();
    }
}
