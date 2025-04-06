package com.example.nemergentprueba.camera;

import android.content.Context;
import android.widget.Toast;
import androidx.camera.core.CameraSelector;
import androidx.camera.view.PreviewView;

import com.example.nemergentprueba.R;

public class FrontCamera extends Camera {
    
    public FrontCamera(Context context, PreviewView viewFinder) {
        super(context, viewFinder, CameraSelector.LENS_FACING_FRONT);
    }
    
    /**
     * Muestra un mensaje indicando que estamos usando la cámara delantera
     */
    public void showCameraInfo() {
        Toast.makeText(context, context.getString(R.string.using_front_camera), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Método para sugerir cambio a cámara trasera
     */
    public void suggestCameraSwitch() {
        Toast.makeText(context, context.getString(R.string.switch_to_back_camera), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void startCamera(androidx.lifecycle.LifecycleOwner lifecycleOwner) {
        super.startCamera(lifecycleOwner);
        showCameraInfo();
    }
}