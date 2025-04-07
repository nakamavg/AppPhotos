package com.nemergent.test.ui.network

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nemergent.test.R
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class PingDialogFragment : DialogFragment() {

    private lateinit var statusTextView: TextView
    private lateinit var hostEditText: EditText
    private lateinit var attemptsEditText: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var summaryTextView: TextView
    
    private var pingJob: Job? = null
    private val pingResults = mutableListOf<PingResult>()
    private val pingAdapter = PingResultAdapter(pingResults)
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialogStyle)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_ping, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        statusTextView = view.findViewById(R.id.statusTextView)
        hostEditText = view.findViewById(R.id.hostEditText)
        attemptsEditText = view.findViewById(R.id.attemptsEditText)
        startButton = view.findViewById(R.id.startButton)
        stopButton = view.findViewById(R.id.stopButton)
        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView)
        summaryTextView = view.findViewById(R.id.summaryTextView)
        
        // Default host to ping
        hostEditText.setText("google.com")
        attemptsEditText.setText("5")
        
        // Set up RecyclerView
        resultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        resultsRecyclerView.adapter = pingAdapter
        
        // Initial state
        updateUI(isRunning = false)
        statusTextView.text = getString(R.string.ping_status_ready)
        
        // Setup button listeners
        startButton.setOnClickListener { startPingTest() }
        stopButton.setOnClickListener { stopPingTest() }
        
        view.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dismiss()
        }
    }
    
    private fun startPingTest() {
        val host = hostEditText.text.toString().trim()
        val attemptsStr = attemptsEditText.text.toString().trim()
        
        if (host.isEmpty() || attemptsStr.isEmpty()) {
            statusTextView.text = getString(R.string.enter_valid_attempts)
            return
        }
        
        val attempts = attemptsStr.toIntOrNull() ?: 0
        val MIN_ATTEMPTS = 1
        val MAX_ATTEMPTS = 20
        
        if (attempts < MIN_ATTEMPTS || attempts > MAX_ATTEMPTS) {
            statusTextView.text = getString(R.string.attempts_range_error, MIN_ATTEMPTS, MAX_ATTEMPTS)
            return
        }
        
        // Clear previous results
        pingResults.clear()
        pingAdapter.notifyDataSetChanged()
        summaryTextView.text = ""
        
        // Update UI state
        updateUI(isRunning = true)
        statusTextView.text = getString(R.string.ping_started, attempts)
        
        pingJob = coroutineScope.launch {
            var successCount = 0
            var failureCount = 0
            
            for (i in 1..attempts) {
                if (!isActive) break // Check if the job was cancelled
                
                statusTextView.text = getString(R.string.ping_progress, i, attempts)
                
                val result = withContext(Dispatchers.IO) {
                    try {
                        val startTime = System.nanoTime()
                        val reachable = InetAddress.getByName(host).isReachable(5000) // 5 seconds timeout
                        val endTime = System.nanoTime()
                        val pingTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)
                        
                        if (reachable) {
                            successCount++
                            PingResult(i, true, pingTime)
                        } else {
                            failureCount++
                            PingResult(i, false, 0)
                        }
                    } catch (e: UnknownHostException) {
                        failureCount++
                        PingResult(i, false, 0, "Unknown host: ${e.message}")
                    } catch (e: IOException) {
                        failureCount++
                        PingResult(i, false, 0, "IO Error: ${e.message}")
                    } catch (e: Exception) {
                        failureCount++
                        PingResult(i, false, 0, "Error: ${e.message}")
                    }
                }
                
                // Add result to list and update UI
                pingResults.add(result)
                pingAdapter.notifyItemInserted(pingResults.size - 1)
                resultsRecyclerView.scrollToPosition(pingResults.size - 1)
                
                delay(500) // Short delay between pings
            }
            
            // Complete ping test
            statusTextView.text = getString(R.string.ping_completed)
            summaryTextView.text = getString(R.string.ping_summary, successCount, failureCount, attempts)
            updateUI(isRunning = false)
        }
    }
    
    private fun stopPingTest() {
        pingJob?.cancel()
        pingJob = null
        updateUI(isRunning = false)
        statusTextView.text = getString(R.string.ping_status_ready)
    }
    
    private fun updateUI(isRunning: Boolean) {
        hostEditText.isEnabled = !isRunning
        attemptsEditText.isEnabled = !isRunning
        startButton.visibility = if (isRunning) View.GONE else View.VISIBLE
        stopButton.visibility = if (isRunning) View.VISIBLE else View.GONE
    }
    
    override fun onDestroy() {
        pingJob?.cancel()
        coroutineScope.cancel()
        super.onDestroy()
    }
    
    data class PingResult(
        val attemptNumber: Int,
        val isSuccess: Boolean,
        val timeMs: Long,
        val errorMessage: String? = null
    )
    
    inner class PingResultAdapter(private val results: List<PingResult>) :
        RecyclerView.Adapter<PingResultAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val attemptNumberTextView: TextView = view.findViewById(R.id.attemptNumberTextView)
            val statusTextView: TextView = view.findViewById(R.id.resultStatusTextView)
            val timeTextView: TextView = view.findViewById(R.id.timeTextView)
            val errorTextView: TextView = view.findViewById(R.id.errorTextView)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ping_result, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val result = results[position]
            
            holder.attemptNumberTextView.text = "#${result.attemptNumber}"
            
            if (result.isSuccess) {
                holder.statusTextView.text = getString(R.string.ping_success)
                holder.statusTextView.setTextColor(resources.getColor(R.color.ping_success, null))
                holder.timeTextView.text = "${getString(R.string.ping_time)} ${result.timeMs}ms"
                holder.timeTextView.visibility = View.VISIBLE
                holder.errorTextView.visibility = View.GONE
            } else {
                holder.statusTextView.text = getString(R.string.ping_fail)
                holder.statusTextView.setTextColor(resources.getColor(R.color.ping_failure, null))
                holder.timeTextView.visibility = View.GONE
                
                if (result.errorMessage != null) {
                    holder.errorTextView.text = result.errorMessage
                    holder.errorTextView.visibility = View.VISIBLE
                } else {
                    holder.errorTextView.visibility = View.GONE
                }
            }
        }
        
        override fun getItemCount() = results.size
    }
    
    companion object {
        fun newInstance(): PingDialogFragment {
            return PingDialogFragment()
        }
    }
}