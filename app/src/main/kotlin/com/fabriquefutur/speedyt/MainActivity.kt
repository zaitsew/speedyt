package com.fabriquefutur.speedyt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabriquefutur.speedyt.ui.HistoryScreen
import com.fabriquefutur.speedyt.ui.HomeScreen
import com.fabriquefutur.speedyt.ui.ResultsScreen
import com.fabriquefutur.speedyt.ui.RunningScreen
import com.fabriquefutur.speedyt.ui.Screen
import com.fabriquefutur.speedyt.ui.SpeedtestViewModel
import com.fabriquefutur.speedyt.ui.SpeedytTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SpeedtestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpeedytTheme { SpeedytApp(viewModel) } }
    }

    /** A test must not keep running, and using bandwidth, once the app leaves the screen. */
    override fun onStop() {
        viewModel.cancel()
        super.onStop()
    }
}

@Composable
private fun SpeedytApp(vm: SpeedtestViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Back leaves the current screen; on a running test it cancels the test first.
    BackHandler(enabled = state.screen != Screen.HOME) { vm.back() }

    // The TV must not dim or start its screensaver mid-test.
    val view = LocalView.current
    val running = state.screen == Screen.RUNNING
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    when (state.screen) {
        Screen.HOME -> HomeScreen(state.connection, state.error, vm::start, vm::showHistory)
        Screen.RUNNING -> RunningScreen(state.progress, vm::cancel)
        Screen.RESULTS -> state.result?.let {
            ResultsScreen(it, state.connection, vm::start, vm::showHistory)
        }
        Screen.HISTORY -> HistoryScreen(state.history)
    }
}
