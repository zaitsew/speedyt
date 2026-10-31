package com.fabriquefutur.speedyt.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fabriquefutur.speedyt.analysis.ConnectionInfo
import com.fabriquefutur.speedyt.analysis.ConnectionKind
import com.fabriquefutur.speedyt.data.HistoryEntry
import com.fabriquefutur.speedyt.data.HistoryRepository
import com.fabriquefutur.speedyt.engine.CloudflareTransport
import com.fabriquefutur.speedyt.engine.ErrorKind
import com.fabriquefutur.speedyt.engine.Phase
import com.fabriquefutur.speedyt.engine.Progress
import com.fabriquefutur.speedyt.engine.SpeedTestEngine
import com.fabriquefutur.speedyt.engine.SpeedTestException
import com.fabriquefutur.speedyt.engine.SpeedTestResult
import com.fabriquefutur.speedyt.net.ConnectionReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { HOME, RUNNING, RESULTS, HISTORY }

data class UiState(
    val screen: Screen = Screen.HOME,
    val progress: Progress = Progress(Phase.LATENCY),
    val result: SpeedTestResult? = null,
    val resultAtMillis: Long = 0,
    val connection: ConnectionInfo = ConnectionInfo(ConnectionKind.NONE),
    val error: ErrorKind? = null,
    val history: List<HistoryEntry> = emptyList(),
)

class SpeedtestViewModel(app: Application) : AndroidViewModel(app) {
    private val connections = ConnectionReader(app)
    private val history = HistoryRepository(app)
    private val _state = MutableStateFlow(UiState(connection = connections.read()))
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch {
            history.entries.collect { list -> _state.update { it.copy(history = list) } }
        }
    }

    fun start() {
        if (job?.isActive == true) return
        if (!connections.hasInternet()) {
            _state.update { it.copy(error = ErrorKind.NO_NETWORK, connection = connections.read()) }
            return
        }
        _state.update {
            it.copy(
                screen = Screen.RUNNING,
                progress = Progress(Phase.LATENCY),
                result = null,
                error = null,
                connection = connections.read(),
            )
        }
        job = viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = SpeedTestEngine(CloudflareTransport()).run { p ->
                    _state.update { it.copy(progress = p) }
                }
                // Read the link again: signal strength can change during a test.
                val connection = connections.read()
                val now = System.currentTimeMillis()
                history.add(
                    HistoryEntry(
                        timestampMillis = now,
                        downloadMbps = result.downloadMbps,
                        uploadMbps = result.uploadMbps,
                        pingMs = result.idleLatencyMs,
                        jitterMs = result.jitterMs,
                        connection = connection.kind,
                        wifiBand = connection.band,
                    ),
                )
                _state.update {
                    it.copy(screen = Screen.RESULTS, result = result, resultAtMillis = now, connection = connection)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SpeedTestException) {
                Log.w(TAG, "Test failed: ${e.kind}", e.cause)
                _state.update { it.copy(screen = Screen.HOME, error = e.kind) }
            } catch (e: Exception) {
                Log.w(TAG, "Test failed", e)
                _state.update { it.copy(screen = Screen.HOME, error = ErrorKind.FAILED) }
            }
        }
    }

    /** Stops a running test and returns to the start screen. Safe to call at any time. */
    fun cancel() {
        job?.cancel()
        job = null
        _state.update { if (it.screen == Screen.RUNNING) it.copy(screen = Screen.HOME) else it }
    }

    fun showHistory() = _state.update { it.copy(screen = Screen.HISTORY) }

    /** The Back key: leaves the current screen, cancelling a running test on the way. */
    fun back() {
        if (_state.value.screen == Screen.RUNNING) cancel() else _state.update { it.copy(screen = Screen.HOME) }
    }

    override fun onCleared() {
        job?.cancel()
    }

    private companion object {
        const val TAG = "Speedyt"
    }
}
