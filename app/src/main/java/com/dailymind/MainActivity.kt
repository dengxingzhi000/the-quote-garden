package com.dailymind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dailymind.core.designsystem.theme.DailyMindTheme
import com.dailymind.core.navigation.AppNavDisplay
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DailyMindTheme { AppNavDisplay() } }
    }
}
