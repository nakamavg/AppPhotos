package com.example.nemergentprueba.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.nemergentprueba.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Servicio para gestionar la obtención de coordenadas de localización.
 */
public class LocationService {
    private static final String TAG = "LocationService";
    
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private LocationListener locationListener;

    public LocationService(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Inicia la actualización de localización
     * @param listener Listener para recibir actualizaciones de localización
     */
    public void startLocationUpdates(LocationListener listener) {
        this.locationListener = listener;
        
        if (ActivityCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (locationListener != null) {
                locationListener.onLocationError(context.getString(R.string.location_permission_required));
            }
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(10000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateLocation(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback, Looper.getMainLooper());
            
            // También intentamos obtener la última ubicación conocida inmediatamente
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    updateLocation(location);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Error al solicitar actualizaciones de ubicación", e);
            if (locationListener != null) {
                locationListener.onLocationError(context.getString(R.string.location_error, e.getMessage()));
            }
        }
    }

    /**
     * Detiene las actualizaciones de localización
     */
    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    /**
     * Obtiene la última localización conocida
     * @return La última ubicación o null si no está disponible
     */
    public Location getLastLocation() {
        return lastLocation;
    }

    private void updateLocation(Location location) {
        lastLocation = location;
        if (locationListener != null) {
            locationListener.onLocationChanged(location);
        }
    }

    /**
     * Interfaz para recibir actualizaciones de localización
     */
    public interface LocationListener {
        void onLocationChanged(Location location);
        void onLocationError(String error);
    }
}