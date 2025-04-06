package com.example.nemergentprueba.database;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Clase para convertir tipos complejos (como Date) que Room no puede manejar directamente.
 */
public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
