package com.example.nemergentprueba.ping;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.nemergentprueba.R;

public class PingDialog extends DialogFragment implements PingService.PingListener {
    private PingService pingService;
    private EditText attemptsInput;
    private Button startStopButton;
    private TextView resultsText;
    private TextView progressText;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_ping_simple, null);

        pingService = new PingService(requireContext());
        pingService.addListener(this);

        attemptsInput = view.findViewById(R.id.attempts_input);
        startStopButton = view.findViewById(R.id.start_stop_button);
        resultsText = view.findViewById(R.id.results_text);
        progressText = view.findViewById(R.id.progress_text);

        startStopButton.setOnClickListener(v -> togglePing());

        builder.setView(view)
                .setTitle(R.string.ping_button)
                .setNegativeButton(R.string.close_button, (dialog, id) -> {
                    pingService.stopPinging();
                    dismiss();
                });

        return builder.create();
    }

    private void togglePing() {
        if (pingService.isRunning()) {
            pingService.stopPinging();
            startStopButton.setText(R.string.start_ping);
        } else {
            try {
                int attempts = Integer.parseInt(attemptsInput.getText().toString());
                if (attempts > 0) {
                    pingService.startPinging(attempts);
                    startStopButton.setText(R.string.stop_ping);
                    resultsText.setText("");
                    progressText.setText(getString(R.string.ping_status_in_progress, 0, attempts));
                }
            } catch (NumberFormatException e) {
                attemptsInput.setError(getString(R.string.ping_invalid_attempts));
            }
        }
    }

    @Override
    public void onPingResult(int attemptNumber, boolean success) {
        requireActivity().runOnUiThread(() -> {
            String result = success ? "✓" : "✗";
            resultsText.append(result + " ");
            progressText.setText(getString(R.string.ping_status_in_progress, attemptNumber, 
                    Integer.parseInt(attemptsInput.getText().toString())));
        });
    }

    @Override
    public void onPingCompleted(int successCount, int failureCount) {
        requireActivity().runOnUiThread(() -> {
            startStopButton.setText(R.string.start_ping);
            progressText.setText(getString(R.string.ping_summary, successCount, failureCount, 
                    successCount + failureCount));
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pingService.removeListener(this);
    }
}