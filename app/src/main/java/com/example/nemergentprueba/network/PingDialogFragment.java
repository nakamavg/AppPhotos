package com.example.nemergentprueba.network;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nemergentprueba.R;
import com.example.nemergentprueba.network.PingService.PingObserver;
import com.example.nemergentprueba.network.PingService.PingResult;
import com.google.android.material.slider.Slider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Diálogo para controlar y mostrar los resultados de los pings
 */
public class PingDialogFragment extends DialogFragment implements PingObserver {
    
    private static final String TAG = "PingDialogFragment";
    private static final int MAX_ATTEMPTS = 100;
    private static final int MIN_ATTEMPTS = 1;
    private static final int DEFAULT_ATTEMPTS = 5;
    
    private PingService pingService;
    private Slider attemptsSlider;
    private TextView attemptsValueText;
    private Button actionButton;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private TextView summaryTextView;
    private RecyclerView resultsRecyclerView;
    private PingResultsAdapter resultsAdapter;
    
    public PingDialogFragment() {
        // Constructor requerido para DialogFragment
    }
    
    /**
     * Crea una nueva instancia del diálogo
     */
    public static PingDialogFragment newInstance() {
        return new PingDialogFragment();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
        
        // Inicializar servicio de ping
        pingService = new PingService();
        pingService.addObserver(this);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_ping_network, container, false);
        
        // Inicializar vistas
        attemptsSlider = view.findViewById(R.id.sliderAttempts);
        attemptsValueText = view.findViewById(R.id.textViewAttemptsValue);
        actionButton = view.findViewById(R.id.buttonAction);
        progressBar = view.findViewById(R.id.progressBarPing);
        statusTextView = view.findViewById(R.id.textViewStatus);
        summaryTextView = view.findViewById(R.id.textViewSummary);
        resultsRecyclerView = view.findViewById(R.id.recyclerViewResults);
        
        // Configurar slider
        attemptsSlider.setValue(DEFAULT_ATTEMPTS);
        attemptsValueText.setText(String.valueOf(DEFAULT_ATTEMPTS));
        attemptsSlider.addOnChangeListener((slider, value, fromUser) -> {
            int attempts = (int) value;
            attemptsValueText.setText(String.valueOf(attempts));
        });
        
        // Configurar RecyclerView
        resultsAdapter = new PingResultsAdapter(new ArrayList<>());
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        resultsRecyclerView.setAdapter(resultsAdapter);
        
        // Configurar botón de acción
        actionButton.setOnClickListener(v -> togglePingService());
        
        // Configurar botón de cierre
        Button closeButton = view.findViewById(R.id.buttonClose);
        closeButton.setOnClickListener(v -> {
            if (pingService.isRunning()) {
                pingService.stopPing();
            }
            dismiss();
        });
        
        // Estado inicial del diálogo
        updateUIState(false);
        
        return view;
    }
    
    /**
     * Inicia o detiene el servicio de ping según el estado actual
     */
    private void togglePingService() {
        if (pingService.isRunning()) {
            // Detener el ping
            pingService.stopPing();
        } else {
            // Iniciar el ping
            int attempts = (int) attemptsSlider.getValue();
            
            if (attempts < MIN_ATTEMPTS || attempts > MAX_ATTEMPTS) {
                Toast.makeText(getContext(), 
                        getString(R.string.attempts_range_error, MIN_ATTEMPTS, MAX_ATTEMPTS), 
                        Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Limpiar resultados anteriores
            resultsAdapter.clearResults();
            summaryTextView.setText("");
            
            // Iniciar ping
            pingService.startPing(attempts);
        }
    }
    
    /**
     * Actualiza el estado de la UI según si está ejecutando o no
     */
    private void updateUIState(boolean isRunning) {
        if (isRunning) {
            actionButton.setText(R.string.stop_ping);
            attemptsSlider.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            actionButton.setText(R.string.start_ping);
            attemptsSlider.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pingService != null) {
            pingService.removeObserver(this);
            if (pingService.isRunning()) {
                pingService.stopPing();
            }
        }
    }
    
    // ===== Implementación de PingObserver =====
    
    @Override
    public void onPingStarted(int totalAttempts) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            updateUIState(true);
            statusTextView.setText(getString(R.string.ping_started, totalAttempts));
            progressBar.setMax(totalAttempts);
            progressBar.setProgress(0);
        });
    }
    
    @Override
    public void onPingProgress(int current, int total, int success, int fail, PingResult result) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            statusTextView.setText(getString(R.string.ping_progress, current, total));
            progressBar.setProgress(current);
            resultsAdapter.addResult(result);
            resultsRecyclerView.scrollToPosition(resultsAdapter.getItemCount() - 1);
        });
    }
    
    @Override
    public void onPingCompleted(int success, int fail, List<PingResult> results) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            updateUIState(false);
            statusTextView.setText(R.string.ping_completed);
            summaryTextView.setText(getString(R.string.ping_summary, success, fail, success + fail));
        });
    }
    
    @Override
    public void onPingError(String message) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            updateUIState(false);
            statusTextView.setText(getString(R.string.ping_error, message));
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        });
    }
    
    /**
     * Adaptador para mostrar resultados de ping en RecyclerView
     */
    private static class PingResultsAdapter extends RecyclerView.Adapter<PingResultsAdapter.ViewHolder> {
        
        private final List<PingResult> results;
        private final SimpleDateFormat timeFormat;
        
        public PingResultsAdapter(List<PingResult> results) {
            this.results = results;
            this.timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ping_result, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PingResult result = results.get(position);
            Context context = holder.itemView.getContext();
            
            // Mostrar el número de intento usando el recurso localizado
            holder.textViewAttempt.setText(context.getString(R.string.ping_attempt_prefix) + result.getAttemptNumber());
            
            // Mostrar status con tiempo cuando está disponible
            if (result.isSuccess()) {
                String statusText = context.getString(R.string.ping_success);
                if (result.getPingTime() > 0) {
                    statusText += " - " + result.getPingTime() + "ms";
                }
                holder.textViewStatus.setText(statusText);
                holder.textViewStatus.setTextColor(context.getResources().getColor(
                        R.color.ping_success, null));
                
                // Mostrar icono verde para ping exitoso
                if (holder.itemView.findViewById(R.id.ping_status_icon) instanceof ImageView) {
                    ImageView iconView = holder.itemView.findViewById(R.id.ping_status_icon);
                    iconView.setImageResource(android.R.drawable.presence_online);
                    iconView.setColorFilter(context.getResources().getColor(
                            R.color.ping_success, null));
                }
            } else {
                holder.textViewStatus.setText(context.getString(R.string.ping_fail));
                holder.textViewStatus.setTextColor(context.getResources().getColor(
                        R.color.ping_fail, null));
                
                // Mostrar icono rojo para ping fallido
                if (holder.itemView.findViewById(R.id.ping_status_icon) instanceof ImageView) {
                    ImageView iconView = holder.itemView.findViewById(R.id.ping_status_icon);
                    iconView.setImageResource(android.R.drawable.presence_busy);
                    iconView.setColorFilter(context.getResources().getColor(
                            R.color.ping_fail, null));
                }
            }
            
            // Mostrar timestamp
            holder.textViewTime.setText(timeFormat.format(new Date(result.getTimestamp())));
        }
        
        @Override
        public int getItemCount() {
            return results.size();
        }
        
        /**
         * Añade un nuevo resultado y notifica cambios
         */
        public void addResult(PingResult result) {
            results.add(result);
            notifyItemInserted(results.size() - 1);
        }
        
        /**
         * Limpia todos los resultados
         */
        public void clearResults() {
            int size = results.size();
            results.clear();
            notifyItemRangeRemoved(0, size);
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewAttempt;
            TextView textViewStatus;
            TextView textViewTime;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewAttempt = itemView.findViewById(R.id.ping_result_sequence);
                textViewStatus = itemView.findViewById(R.id.ping_result_status);
                textViewTime = itemView.findViewById(R.id.ping_result_timestamp);
            }
        }
    }
}