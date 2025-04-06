package com.example.nemergentprueba.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * Entidad que representa una foto guardada en la base de datos.
 * Contiene información sobre la fecha de captura, ruta relativa del archivo
 * y coordenadas de localización en el momento de la captura.
 */
@Entity(tableName = "photos")
public class PhotoEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private Date captureDate;

    @NonNull
    private String relativePath;

    // Coordenadas de localización
    private double latitude;
    private double longitude;
    private Float accuracy; // Precisión en metros, puede ser null

    // Constructor
    public PhotoEntity(@NonNull Date captureDate, @NonNull String relativePath, 
                       double latitude, double longitude, Float accuracy) {
        this.captureDate = captureDate;
        this.relativePath = relativePath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public Date getCaptureDate() {
        return captureDate;
    }

    public void setCaptureDate(@NonNull Date captureDate) {
        this.captureDate = captureDate;
    }

    @NonNull
    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(@NonNull String relativePath) {
        this.relativePath = relativePath;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }
}