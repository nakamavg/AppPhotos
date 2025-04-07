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
    
    @Override
    public void startCamera(androidx.lifecycle.LifecycleOwner lifecycleOwner) {
        super.startCamera(lifecycleOwner);
    }
}
