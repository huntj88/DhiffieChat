package me.jameshunt.dhiffiechat

import android.app.Application
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

class DhiffieChatApp: Application() {
    companion object {
        lateinit var di: DI
    }

    override fun onCreate() {
        super.onCreate()
        di = DI(this)
    }
}

object DhiffieTheme {
    val DarkColors = darkColors()
    val LightColors = lightColors()
}

@Composable
fun activeColors(): Colors = when (isSystemInDarkTheme()) {
    true -> DhiffieTheme.DarkColors
    false -> DhiffieTheme.LightColors
}

class PrefManager(application: DhiffieChatApp) {
    private val prefs = application.getSharedPreferences("dhiffieChat", Context.MODE_PRIVATE)

    fun isFirstLaunch(): Boolean = prefs.getBoolean("isFirstLaunch", true).also {
        if (it) {
            prefs.edit().putBoolean("isFirstLaunch", false).apply()
        }
    }
}