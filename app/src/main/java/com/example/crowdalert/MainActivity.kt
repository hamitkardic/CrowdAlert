package com.example.crowdalert

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.crowdalert.ui.navigation.CrowdAlertNavHost
import com.example.crowdalert.ui.settings.AppSettings
import com.example.crowdalert.ui.settings.AppThemeMode
import com.example.crowdalert.ui.theme.CrowdAlertTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedLocale = prefs.getString("selected_locale", null)
        if (savedLocale != null) {
            val locale = Locale(savedLocale)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            val context = newBase.createConfigurationContext(config)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPostNotificationsPermissionIfNeeded()
        setContent {
            var themeMode by remember { mutableStateOf(AppSettings.getThemeMode(this)) }
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme =
                when (themeMode) {
                    AppThemeMode.Light -> false
                    AppThemeMode.Dark -> true
                    AppThemeMode.Device -> systemDarkTheme
                }

            CrowdAlertTheme(darkTheme = darkTheme) {
                CrowdAlertNavHost(
                    modifier = Modifier.fillMaxSize(),
                    currentThemeMode = themeMode,
                    onThemeModeSelected = { themeMode = it },
                )
            }
        }
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val isGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}