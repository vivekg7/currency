package com.crylo.currency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.crylo.currency.ui.ConverterViewModel
import com.crylo.currency.ui.DetailScreen
import com.crylo.currency.ui.MainScreen
import com.crylo.currency.ui.PickerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: ConverterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // "Refresh on open": every time the app comes to the foreground, refetch if the
        // cached rates have been superseded. Cheap no-op otherwise.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { vm.refreshIfStale() }
        }
        setContent {
            val context = LocalContext.current
            val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            MaterialTheme(colorScheme = scheme) {
                // Three screens don't justify a navigation library: a saveable route string
                // survives rotation and process death, and back always returns to the list.
                var route by rememberSaveable { mutableStateOf("main") }
                BackHandler(enabled = route != "main") { route = "main" }
                when {
                    route == "picker" -> PickerScreen(vm, onBack = { route = "main" })
                    route.startsWith("detail:") -> {
                        val (_, base, quote) = route.split(':')
                        DetailScreen(
                            vm = vm,
                            base = base,
                            quote = quote,
                            onSwap = { route = "detail:$quote:$base" },
                            onBack = { route = "main" },
                        )
                    }
                    else -> MainScreen(
                        vm = vm,
                        onAdd = { route = "picker" },
                        onChart = { base, quote -> route = "detail:$base:$quote" },
                    )
                }
            }
        }
    }
}
