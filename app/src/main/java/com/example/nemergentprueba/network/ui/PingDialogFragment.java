package com.example.nemergentprueba.network.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nemergentprueba.R;
import com.example.nemergentprueba.network.PingService;

import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para realizar y mostrar resultados de pings a Google.com
 */
public class PingDialogFragment extends DialogFragment implements PingService.PingListener {
    
    private PingService pingService;
    private PingResultAdapter adapter;
    
    // UI Components
    private EditText attemptsInput;
    private Button toggleButton;
    private View statusContainer;
    private TextView statusText;
    private RecyclerView resultsList;
    private View emptyView;
    private View summaryContainer;
    private TextView successfulCount;
    private TextView failedCount;
    
    private int totalAttempts = 0;
    private int currentAttempt = 0;
    
    public static PingDialogFragment newInstance() {
        return new PingDialogFragment();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
        
        // Inicializar el servicio de ping
        pingService = new PingService();
        pingService.setPingListener(this);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_ping, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar componentes de UI
        attemptsInput = view.findViewById(R.id.ping_attempts_input);
        toggleButton = view.findViewById(R.id.ping_toggle_button);
        statusContainer = view.findViewById(R.id.ping_status_container);
        statusText = view.findViewById(R.id.ping_status_text);
        resultsList = view.findViewById(R.id.ping_results_list);
        emptyView = view.findViewById(R.id.ping_empty_view);
        summaryContainer = view.findViewById(R.id.ping_summary_container);
        successfulCount = view.findViewById(R.id.ping_successful_count);
        failedCount = view.findViewById(R.id.ping_failed_count);
        
        // Configurar RecyclerView
        adapter = new PingResultAdapter();
        resultsList.setAdapter(adapter);
        
        // Configurar botón de inicio/detención
        toggleButton.setOnClickListener(v -> {
            if (pingService.isRunning()) {
                stopPing();
            } else {
                startPing();
            }
        });
        
        // Actualizar UI inicial
        updateUI();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
    
    @Override
    public void onDestroy() {
        // Asegurarse de detener el ping si el fragmento se destruye
        if (pingService != null && pingService.isRunning()) {
            pingService.stopPing();
        }
        super.onDestroy();
    }
    
    // Método para iniciar el ping
    private void startPing() {
        // Validar número de intentos
        String attemptsStr = attemptsInput.getText().toString().trim();
        if (TextUtils.isEmpty(attemptsStr)) {
            Toast.makeText(getContext(), R.string.ping_invalid_attempts, Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int attempts = Integer.parseInt(attemptsStr);
            if (attempts <= 0 || attempts > 100) {
                Toast.makeText(getContext(), R.string.ping_invalid_attempts, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Iniciar ping
            totalAttempts = attempts;
            currentAttempt = 0;
            adapter.clearResults();
            updateEmptyView();
            
            toggleButton.setText(R.string.ping_stop_button);
            statusContainer.setVisibility(View.VISIBLE);
            summaryContainer.setVisibility(View.GONE);
            
            pingService.startPing(attempts);
            
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.ping_invalid_attempts, Toast.LENGTH_SHORT).show();
        }
    }
    
    // Método para detener el ping
    private void stopPing() {
        pingService.stopPing();
    }
    
    // Actualizar UI según el estado actual
    private void updateUI() {
        if (pingService.isRunning()) {
            toggleButton.setText(R.string.ping_stop_button);
            statusContainer.setVisibility(View.VISIBLE);
            statusText.setText(getString(R.string.ping_status_in_progress, currentAttempt, totalAttempts));
        } else {
            toggleButton.setText(R.string.ping_start_button);
            
            if (adapter.getItemCount() > 0) {
                statusContainer.setVisibility(View.VISIBLE);
                statusText.setText(R.string.ping_status_completed);
                showSummary();
            } else {
                statusContainer.setVisibility(View.GONE);
                summaryContainer.setVisibility(View.GONE);
            }
        }
        
        updateEmptyView();
    }
    
    // Mostrar u ocultar vista vacía
    private void updateEmptyView() {
        if (adapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            resultsList.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            resultsList.setVisibility(View.VISIBLE);
        }
    }
    
    // Mostrar resumen de resultados
    private void showSummary() {
        List<PingService.PingResult> results = pingService.getResults();
        int successful = 0;
        int failed = 0;
        
        for (PingService.PingResult result : results) {
            if (result.isSuccessful()) {
                successful++;
            } else {
                failed++;
            }
        }
        
        successfulCount.setText(String.valueOf(successful));
        failedCount.setText(String.valueOf(failed));
        summaryContainer.setVisibility(View.VISIBLE);
    }
    
    // Implementación de PingListener
    
    @Override
    public void onPingStarted() {
        currentAttempt = 0;
        updateUI();
    }
    
    @Override
    public void onPingResult(PingService.PingResult result) {
        currentAttempt++;
        adapter.addResult(result);
        statusText.setText(getString(R.string.ping_status_in_progress, currentAttempt, totalAttempts));
        updateEmptyView();
    }
    
    @Override
    public void onPingCompleted(List<PingService.PingResult> results) {
        statusText.setText(R.string.ping_status_completed);
        toggleButton.setText(R.string.ping_start_button);
        showSummary();
    }
    
    @Override
    public void onPingStopped(List<PingService.PingResult> results) {
        statusText.setText(R.string.ping_status_stopped);
        toggleButton.setText(R.string.ping_start_button);
        showSummary();
    }
    
    /**
     * Adaptador para el RecyclerView de resultados de ping
     */
    private class PingResultAdapter extends RecyclerView.Adapter<PingResultAdapter.PingResultViewHolder> {
        
        private final List<PingService.PingResult> results = new ArrayList<>();
        
        @NonNull
        @Override
        public PingResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ping_simple_result, parent, false);
            return new PingResultViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull PingResultViewHolder holder, int position) {
            holder.bind(results.get(position));
        }
        
        @Override
        public int getItemCount() {
            return results.size();
        }
        
        public void addResult(PingService.PingResult result) {
            results.add(result);
            notifyItemInserted(results.size() - 1);
        }
        
        public void clearResults() {
            results.clear();
            notifyDataSetChanged();
        }
        
        class PingResultViewHolder extends RecyclerView.ViewHolder {
            private final View iconView;
            private final TextView textView;
            
            public PingResultViewHolder(@NonNull View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.ping_result_icon);
                textView = itemView.findViewById(R.id.ping_result_text);
            }
            
            public void bind(PingService.PingResult result) {
                if (result.isSuccessful()) {
                    iconView.setBackgroundResource(R.color.ping_success);
                    if (result.getPingTime() >= 0) {
                        textView.setText(getString(R.string.ping_result_success, result.getPingTime()));
                    } else {
                        textView.setText(R.string.ping_result_success);
                    }
                } else {
                    iconView.setBackgroundResource(R.color.ping_failure);
                    textView.setText(R.string.ping_result_failure);
                }
            }
        }
    }
}