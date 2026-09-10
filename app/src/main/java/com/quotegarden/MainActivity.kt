package com.quotegarden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotegarden.core.datastore.ThemeMode
import com.quotegarden.core.datastore.ThemeStore
import com.quotegarden.core.designsystem.theme.QuoteGardenTheme
import com.quotegarden.core.navigation.AppNavDisplay
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themeStore: ThemeStore

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val mode by themeStore.mode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            QuoteGardenTheme(darkTheme = mode.resolveDark(isSystemInDarkTheme())) {
                AppNavDisplay()
            }
        }
    }
}
