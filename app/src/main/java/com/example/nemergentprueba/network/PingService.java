package com.example.nemergentprueba.network;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para realizar pings a Google.com
 */
public class PingService {
    private static final String TAG = "PingService";
    private static final String TARGET_HOST = "8.8.8.8"; // Usando Google DNS en lugar de google.com
    private static final int PING_TIMEOUT = 1000; // 1 segundo de timeout
    
    private PingTask currentTask;
    private boolean isRunning = false;
    private PingListener listener;
    private Handler uiHandler;
    
    // Lista para almacenar resultados de los pings
    private List<PingResult> results = new ArrayList<>();
    
    // Implementación del patrón Observer
    private List<PingObserver> observers = new ArrayList<>();
    
    public PingService() {
        uiHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Añade un observador para recibir actualizaciones
     * @param observer El observador a añadir
     */
    public void addObserver(PingObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }
    
    /**
     * Elimina un observador
     * @param observer El observador a eliminar
     */
    public void removeObserver(PingObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notifica a los observadores que se ha iniciado el ping
     */
    private void notifyPingStarted(int totalAttempts) {
        for (PingObserver observer : new ArrayList<>(observers)) {
            uiHandler.post(() -> observer.onPingStarted(totalAttempts));
        }
    }
    
    /**
     * Notifica a los observadores sobre el progreso del ping
     */
    private void notifyPingProgress(int current, int total, int success, int fail, PingResult result) {
        for (PingObserver observer : new ArrayList<>(observers)) {
            uiHandler.post(() -> observer.onPingProgress(current, total, success, fail, result));
        }
    }
    
    /**
     * Notifica a los observadores que se ha completado el ping
     */
    private void notifyPingCompleted(int success, int fail, List<PingResult> results) {
        for (PingObserver observer : new ArrayList<>(observers)) {
            uiHandler.post(() -> observer.onPingCompleted(success, fail, results));
        }
    }
    
    /**
     * Notifica a los observadores sobre un error
     */
    private void notifyPingError(String message) {
        for (PingObserver observer : new ArrayList<>(observers)) {
            uiHandler.post(() -> observer.onPingError(message));
        }
    }
    
    /**
     * Inicia la tarea de ping
     * @param attempts Número de intentos a realizar
     */
    public void startPing(int attempts) {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        results.clear();
        
        currentTask = new PingTask(attempts);
        currentTask.execute();
        
        // Notificar a los listeners y observers
        if (listener != null) {
            uiHandler.post(() -> listener.onPingStarted());
        }
        notifyPingStarted(attempts);
    }
    
    /**
     * Detiene la tarea de ping en curso
     */
    public void stopPing() {
        if (currentTask != null && isRunning) {
            currentTask.cancel(true);
            isRunning = false;
            
            if (listener != null) {
                uiHandler.post(() -> listener.onPingStopped(results));
            }
        }
    }
    
    /**
     * Establece el listener para recibir actualizaciones de los pings
     * @param listener El listener a establecer
     */
    public void setPingListener(PingListener listener) {
        this.listener = listener;
    }
    
    /**
     * Comprueba si el servicio está realizando pings
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Obtiene la lista actual de resultados
     */
    public List<PingResult> getResults() {
        return new ArrayList<>(results);
    }
    
    /**
     * AsyncTask para realizar pings en segundo plano
     */
    private class PingTask extends AsyncTask<Void, PingResult, List<PingResult>> {
        private final int attempts;
        
        public PingTask(int attempts) {
            this.attempts = attempts;
        }
        
        @Override
        protected List<PingResult> doInBackground(Void... params) {
            int successCount = 0;
            int failCount = 0;
            
            Log.d(TAG, "Iniciando tarea de ping con " + attempts + " intentos");
            
            for (int i = 0; i < attempts && !isCancelled(); i++) {
                Log.d(TAG, "Ejecutando ping #" + (i+1) + " de " + attempts);
                PingResult result = executePing();
                result.setAttemptNumber(i + 1);
                
                // Actualizar contadores
                if (result.isSuccessful()) {
                    successCount++;
                    Log.d(TAG, "Ping #" + (i+1) + " EXITOSO - Tiempo: " + result.getPingTime() + "ms");
                } else {
                    failCount++;
                    Log.d(TAG, "Ping #" + (i+1) + " FALLIDO - Razón: " + result.getOutput());
                }
                
                publishProgress(result);
                results.add(result);
                
                // Notificar progreso a observers
                final int currentSuccess = successCount;
                final int currentFail = failCount;
                final int currentAttempt = i + 1;
                notifyPingProgress(currentAttempt, attempts, currentSuccess, currentFail, result);
                
                // Pequeña pausa entre pings
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Tarea de ping interrumpida durante la pausa");
                    break;
                }
            }
            
            // Notificar observers cuando completa
            final int finalSuccess = successCount;
            final int finalFail = failCount;
            Log.d(TAG, "Tarea de ping completada: " + finalSuccess + " éxitos, " + finalFail + " fallos");
            notifyPingCompleted(finalSuccess, finalFail, new ArrayList<>(results));
            
            return results;
        }
        
        @Override
        protected void onProgressUpdate(PingResult... values) {
            if (values.length > 0 && listener != null && !isCancelled()) {
                listener.onPingResult(values[0]);
            }
        }
        
        @Override
        protected void onPostExecute(List<PingResult> results) {
            isRunning = false;
            if (listener != null) {
                listener.onPingCompleted(results);
            }
        }
        
        @Override
        protected void onCancelled() {
            isRunning = false;
        }
        
        /**
         * Ejecuta un comando ping único usando InetAddress en lugar de Runtime.exec para mayor compatibilidad
         */
        private PingResult executePing() {
            long startTime = System.currentTimeMillis();
            boolean isSuccessful = false;
            long pingTime = -1;
            StringBuilder output = new StringBuilder();
            
            Log.d(TAG, "Iniciando ping a " + TARGET_HOST + " usando InetAddress");
            try {
                // Intentar primero con Java para mayor compatibilidad
                InetAddress address = InetAddress.getByName(TARGET_HOST);
                Log.d(TAG, "Resolvió " + TARGET_HOST + " a la dirección IP: " + address.getHostAddress());
                output.append("Ping a ").append(TARGET_HOST).append("...\n");
                
                Log.d(TAG, "Intentando alcanzar " + TARGET_HOST + " con timeout de " + PING_TIMEOUT + "ms");
                if (address.isReachable(PING_TIMEOUT)) {
                    isSuccessful = true;
                    pingTime = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "Ping EXITOSO usando InetAddress.isReachable(). Tiempo: " + pingTime + "ms");
                    output.append("Respuesta desde ").append(address.getHostAddress())
                          .append(": tiempo=").append(pingTime).append("ms");
                } else {
                    Log.d(TAG, "Ping FALLIDO usando InetAddress.isReachable() - Tiempo de espera agotado");
                    output.append("Tiempo de espera agotado.");
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "Error de host desconocido: " + e.getMessage(), e);
                output.append("Error: No se pudo resolver el nombre de host ").append(TARGET_HOST);
                // Intentar con Runtime como fallback si falla la resolución de nombre
                Log.d(TAG, "Intentando método alternativo con Runtime.exec() debido a error de resolución de nombre");
                return executeRuntimePing();
            } catch (IOException e) {
                Log.e(TAG, "Error de E/S durante el ping con InetAddress: " + e.getMessage(), e);
                output.append("Error de E/S: ").append(e.getMessage());
                // Intentar con Runtime como fallback si hay error de E/S
                Log.d(TAG, "Intentando método alternativo con Runtime.exec() debido a error de E/S");
                return executeRuntimePing();
            }
            
            return new PingResult(isSuccessful, output.toString(), pingTime);
        }
        
        /**
         * Método alternativo usando Runtime.exec para ejecutar ping
         */
        private PingResult executeRuntimePing() {
            Process process = null;
            BufferedReader reader = null;
            StringBuilder output = new StringBuilder();
            boolean isSuccessful = false;
            long pingTime = -1;
            
            try {
                // Ejecutar comando ping (un solo intento: -c 1)
                String pingCmd = "ping -c 1 -W 1 " + TARGET_HOST;
                Log.d(TAG, "Ejecutando comando shell: " + pingCmd);
                output.append("Ejecutando: ").append(pingCmd).append("\n");
                
                process = Runtime.getRuntime().exec(pingCmd);
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, "Salida del ping: " + line);
                    output.append(line).append("\n");
                    
                    // Extraer tiempo de ping si está disponible
                    if (line.contains("time=")) {
                        isSuccessful = true;
                        int timeIndex = line.indexOf("time=");
                        if (timeIndex != -1) {
                            String timeStr = line.substring(timeIndex + 5);
                            int endIndex = timeStr.indexOf(" ms");
                            if (endIndex != -1) {
                                try {
                                    pingTime = (long) Float.parseFloat(timeStr.substring(0, endIndex));
                                    Log.d(TAG, "Tiempo de ping extraído: " + pingTime + "ms");
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Error al parsear el tiempo de ping: " + e.getMessage());
                                    // No se pudo parsear el tiempo
                                }
                            }
                        }
                    }
                }
                
                // Leer también error output
                reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    Log.e(TAG, "Error del comando ping: " + line);
                    output.append("ERROR: ").append(line).append("\n");
                }
                
                // Esperar a que termine el proceso con timeout
                Log.d(TAG, "Esperando a que termine el proceso ping con timeout de 2 segundos");
                boolean finished = process.waitFor(2, TimeUnit.SECONDS);
                if (finished) {
                    int exitValue = process.exitValue();
                    isSuccessful = isSuccessful || exitValue == 0;
                    Log.d(TAG, "Proceso ping terminado con código de salida: " + exitValue + " (exitoso: " + isSuccessful + ")");
                    output.append("Código de salida: ").append(exitValue);
                } else {
                    process.destroy();
                    Log.w(TAG, "El proceso ping no terminó a tiempo y fue cancelado");
                    output.append("El proceso ping no terminó a tiempo y fue cancelado.");
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Error de E/S durante el ping con Runtime.exec(): " + e.getMessage(), e);
                output.append("Error de E/S: ").append(e.getMessage());
            } catch (InterruptedException e) {
                Log.e(TAG, "Proceso interrumpido: " + e.getMessage(), e);
                output.append("Proceso interrumpido: ").append(e.getMessage());
                Thread.currentThread().interrupt();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error al cerrar el lector: " + e.getMessage());
                        // Ignorar
                    }
                }
                if (process != null) {
                    process.destroy();
                }
            }
            
            return new PingResult(isSuccessful, output.toString(), pingTime);
        }
    }
    
    /**
     * Clase que representa el resultado de un ping
     */
    public static class PingResult {
        private final boolean successful;
        private final String output;
        private final long pingTime; // en ms
        private int attemptNumber;
        private long timestamp;
        
        public PingResult(boolean successful, String output, long pingTime) {
            this.successful = successful;
            this.output = output;
            this.pingTime = pingTime;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isSuccessful() {
            return successful;
        }
        
        public String getOutput() {
            return output;
        }
        
        public long getPingTime() {
            return pingTime;
        }
        
        public void setAttemptNumber(int attemptNumber) {
            this.attemptNumber = attemptNumber;
        }
        
        public int getAttemptNumber() {
            return attemptNumber;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public boolean isSuccess() {
            return successful;
        }
    }
    
    /**
     * Interfaz para escuchar eventos del servicio de ping (patrón Observer)
     */
    public interface PingListener {
        void onPingStarted();
        void onPingResult(PingResult result);
        void onPingCompleted(List<PingResult> results);
        void onPingStopped(List<PingResult> results);
    }
    
    /**
     * Interfaz para observar eventos del servicio de ping (patrón Observer)
     */
    public interface PingObserver {
        void onPingStarted(int totalAttempts);
        void onPingProgress(int current, int total, int success, int fail, PingResult result);
        void onPingCompleted(int success, int fail, List<PingResult> results);
        void onPingError(String message);
    }
}