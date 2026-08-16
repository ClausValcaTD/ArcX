package com.m5dev.arcx.domain.model

data class UserPreferences(
    // General
    val defaultExtractLocation: String = "",
    val defaultCompressionFormat: String = "ZIP",
    val defaultCompressionLevel: String = "Normal",
    val askBeforeOverwrite: Boolean = true,

    // Appearance
    val theme: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val dynamicColors: Boolean = true,
    val showHiddenFiles: Boolean = false,

    // Notifications
    val showExtractionNotifications: Boolean = true,
    val showCompletionSound: Boolean = true,
    val vibrateOnCompletion: Boolean = true
)
