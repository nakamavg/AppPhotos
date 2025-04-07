package com.example.nemergentprueba.ping;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class PingService {
    private static final String TAG = "PingService";
    private static final String GOOGLE_HOST = "google.com";
    private static final int TIMEOUT_MS = 5000;

    private Context context;
    private List<PingListener> listeners = new ArrayList<>();
    private boolean isRunning = false;
    private int totalAttempts = 0;
    private int successCount = 0;
    private int currentAttempt = 0;

    public PingService(Context context) {
        this.context = context;
    }

    public void addListener(PingListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PingListener listener) {
        listeners.remove(listener);
    }

    public void startPinging(int attempts) {
        if (isRunning) return;

        totalAttempts = attempts;
        successCount = 0;
        currentAttempt = 0;
        isRunning = true;

        new PingTask().execute();
    }

    public void stopPinging() {
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    private class PingTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                InetAddress inetAddress = InetAddress.getByName(GOOGLE_HOST);
                return inetAddress.isReachable(TIMEOUT_MS);
            } catch (IOException e) {
                Log.e(TAG, "Error al hacer ping: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            currentAttempt++;
            if (success) successCount++;

            for (PingListener listener : listeners) {
                listener.onPingResult(currentAttempt, success);
            }

            if (currentAttempt < totalAttempts && isRunning) {
                new PingTask().execute();
            } else {
                isRunning = false;
                for (PingListener listener : listeners) {
                    listener.onPingCompleted(successCount, totalAttempts - successCount);
                }
            }
        }
    }

    public interface PingListener {
        void onPingResult(int attemptNumber, boolean success);
        void onPingCompleted(int successCount, int failureCount);
    }
}