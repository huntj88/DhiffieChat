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
                secondary = Color(di.application.getColor(R.color.colorSecondary)),
//                secondaryVariant = Color(di.application.getColor(R.color.colorSecondaryVariant)),
                onSecondary = Color(di.application.getColor(R.color.colorOnSecondary))
            )
        }
        val DarkColors by lazy {
            darkColors(
                primary = Color(di.application.getColor(R.color.colorPrimary)),
                primaryVariant = Color(di.application.getColor(R.color.colorPrimaryVariant)),
                onPrimary = Color(di.application.getColor(R.color.colorOnPrimary)),
                secondary = Color(di.application.getColor(R.color.colorSecondary)),
//                secondaryVariant = Color(di.application.getColor(R.color.colorSecondaryVariant)),
                onSecondary = Color(di.application.getColor(R.color.colorOnSecondary)),
            )
        }

        val accent by lazy { Color(di.application.getColor(R.color.accent)) }
    }

    override fun onCreate() {
        super.onCreate()
        di = DI(this)
    }
}