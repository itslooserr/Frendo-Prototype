package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.AppNavigator
import com.example.ui.SecureTextViewModel
import com.example.ui.SecureTextViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeProvider
import com.example.ui.theme.LocalAppThemePreferences

class MainActivity : ComponentActivity() {

  private val viewModel: SecureTextViewModel by viewModels {
    SecureTextViewModelFactory((application as SecureTextApplication).repository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val session by viewModel.userSession.collectAsState(initial = null)
      val appDarkOver by viewModel.onboardingIsDarkMode.collectAsState()
      val appFontSizeOver by viewModel.onboardingFontSizeMultiplier.collectAsState()
      val systemDark = isSystemInDarkTheme()

      ThemeProvider(
        userSession = session,
        onboardingIsDarkMode = appDarkOver,
        onboardingFontSize = appFontSizeOver,
        systemDarkTheme = systemDark
      ) {
        val prefs = LocalAppThemePreferences.current
        MyApplicationTheme(darkTheme = prefs.isDarkMode) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            AppNavigator(viewModel)
          }
        }
      }
    }
  }
}

