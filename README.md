# Nemergent Prueba - Aplicación de Cámara con Geolocalización

Esta aplicación Android permite tomar fotos con geolocalización automática, que se actualiza cuando el usuario se mueve. Las fotos se almacenan localmente y pueden visualizarse en una galería integrada.

## Requisitos previos

- Android Studio Arctic Fox (2020.3.1) o superior
- JDK 11 o superior
- Un dispositivo Android (físico o emulador) con API 21 (Android 5.0 Lollipop) o superior
- Conexión a Internet para descargar dependencias

## Configuración del entorno de desarrollo

1. Clona este repositorio:
```bash
git clone https://github.com/tuusuario/Nemergentprueba.git
```

2. Abre Android Studio y selecciona "Open an Existing Project"
3. Navega hasta la carpeta donde clonaste el repositorio y selecciona la carpeta raíz

## Compilación y ejecución en modo desarrollo

### Usando Android Studio

1. Conecta tu dispositivo Android al ordenador mediante USB
2. Habilita la depuración USB en tu dispositivo:
   - Ve a Configuración > Acerca del teléfono
   - Toca "Número de compilación" 7 veces para habilitar las opciones de desarrollador
   - Regresa a Configuración > Opciones de desarrollador
   - Activa "Depuración USB"

3. En Android Studio, selecciona tu dispositivo en el menú desplegable de dispositivos
4. Haz clic en el botón "Run" (triángulo verde) o usa el atajo Shift+F10

### Usando la línea de comandos

1. Navega a la carpeta raíz del proyecto
2. Ejecuta:
```bash
./gradlew installDebug
```

3. La aplicación se instalará automáticamente en el dispositivo conectado

## Solución de problemas comunes

### Permisos de la aplicación

La aplicación requiere los siguientes permisos:
- Cámara
- Ubicación (fina y aproximada)
- Almacenamiento (en dispositivos con Android 10 o inferior)

Si experimentas problemas, asegúrate de que:
- Has concedido todos los permisos solicitados
- Los servicios de ubicación están activados en el dispositivo

### Problemas específicos en dispositivos Xiaomi

En dispositivos Xiaomi, es posible que necesites habilitar permisos adicionales:
1. Ve a Configuración > Permisos
2. Busca la aplicación y concede todos los permisos necesarios
3. Asegúrate de que la aplicación tiene permiso para ejecutarse en segundo plano

## Funciones principales

### Cámara
- Toma fotos con la cámara trasera o frontal
- Incluye automáticamente metadatos de geolocalización

### Sistema de geolocalización
- Actualiza automáticamente la ubicación cuando te mueves (cada 5 metros)
- Mantiene un sistema de caché para reducir el consumo de batería

### Galería
- Visualiza todas las fotos tomadas con la aplicación
- Muestra información de fecha, hora y ubicación
- Permite eliminar fotos

## Arquitectura del proyecto

La aplicación sigue una arquitectura modular con los siguientes componentes:

- **location**: Clases para gestionar la geolocalización, incluyendo `LocationCache` y `LocationService`
- **camera**: Clases para la gestión de la cámara, incluyendo `CameraActivity`, `BackCamera` y `FrontCamera`
- **utils**: Utilidades generales como gestión de permisos, manipulación de imágenes, etc.

## Licencia

[Incluir información de licencia aquí]