package me.jameshunt.dhiffiechat

import android.app.Application
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class DhiffieChatApp : Application() {
    companion object {
        lateinit var di: DI

        val LightColors by lazy {
            lightColors(
                primary = Color(di.application.getColor(R.color.colorPrimary)),
                primaryVariant = Color(di.application.getColor(R.color.colorPrimaryVariant)),
                onPrimary = Color(di.application.getColor(R.color.colorOnPrimary)),
                secondary = Color(di.application.getColor(R.color.colorSecondary))
            )
        }
        val DarkColors by lazy { darkColors() }
    }

    override fun onCreate() {
        super.onCreate()
        di = DI(this)
    }
}

@Composable
fun activeColors(): Colors = when (isSystemInDarkTheme()) {
    true -> DhiffieChatApp.DarkColors
    false -> DhiffieChatApp.LightColors
}
