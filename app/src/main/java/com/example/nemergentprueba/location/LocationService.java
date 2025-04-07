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
 * Integra el sistema de caché para actualizaciones continuas basadas en movimiento.
 */
public class LocationService {
    private static final String TAG = "LocationService";
    
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private LocationListener locationListener;
    private LocationCache locationCache;

    public LocationService(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationCache = LocationCache.getInstance(context);
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

        // Aseguramos que la caché esté utilizando actualizaciones continuas
        locationCache.startContinuousUpdates();

        // Configuramos las actualizaciones de ubicación con FusedLocationClient
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
                } else {
                    // Si no hay ubicación disponible, usar la de la caché
                    Location cachedLocation = locationCache.getCurrentLocation();
                    if (cachedLocation != null && 
                        (cachedLocation.getLatitude() != 0 || cachedLocation.getLongitude() != 0)) {
                        updateLocation(cachedLocation);
                    }
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
        
        // No detenemos las actualizaciones continuas de la caché
        // para mantener los datos actualizados en segundo plano
    }

    /**
     * Obtiene la última localización conocida
     * @return La última ubicación o null si no está disponible
     */
    public Location getLastLocation() {
        // Primero verificamos nuestra caché interna
        if (lastLocation != null && 
            (lastLocation.getLatitude() != 0 || lastLocation.getLongitude() != 0)) {
            return lastLocation;
        }
        
        // Si no tenemos ubicación en caché interna, usamos la del sistema de caché
        return locationCache.getCurrentLocation();
    }

    private void updateLocation(Location location) {
        if (location == null || (location.getLatitude() == 0 && location.getLongitude() == 0)) {
            return; // Evitamos actualizar con ubicaciones inválidas
        }
        
        lastLocation = location;
        
        // Actualizamos también el sistema de caché
        locationCache.updateCachedLocation(location);
        
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