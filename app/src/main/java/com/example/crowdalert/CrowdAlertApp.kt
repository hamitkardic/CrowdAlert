package com.example.crowdalert

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Hilt: generates the app-wide component and enables
 * [dagger.hilt.android.AndroidEntryPoint] injection in activities and services.
 */
@HiltAndroidApp
class CrowdAlertApp : Application()
