package me.jameshunt.dhiffiechat

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

@SuppressLint("ConflictingOnColor") // TODO: Resolve?
class DhiffieChatApp : Application() {
    companion object {
        lateinit var di: DI

        val LightColors by lazy {
            lightColors(
                primary = Color(di.application.getColor(R.color.colorPrimary)),
                primaryVariant = Color(di.application.getColor(R.color.colorPrimaryVariant)),
                onPrimary = Color(di.application.getColor(R.color.colorOnPrimary)),
                secondary = Color(di.application.getColor(R.color.colorSecondary)),
                onSecondary = Color(di.application.getColor(R.color.colorOnSecondary)),
                surface = Color(di.application.getColor(R.color.colorOnPrimary)),
                onSurface = Color(di.application.getColor(R.color.colorPrimary)),
                background = Color(di.application.getColor(R.color.colorBackground)),
            )
        }
        val DarkColors by lazy {
            darkColors(
                primary = Color(di.application.getColor(R.color.colorPrimary)),
                primaryVariant = Color(di.application.getColor(R.color.colorPrimaryVariant)),
                onPrimary = Color(di.application.getColor(R.color.colorOnPrimary)),
                secondary = Color(di.application.getColor(R.color.colorSecondary)),
                onSecondary = Color(di.application.getColor(R.color.colorOnSecondary)),
                surface = Color(di.application.getColor(R.color.colorPrimary)),
                onSurface = Color(di.application.getColor(R.color.colorOnPrimary)),
                background = Color(di.application.getColor(R.color.colorBackground)),
            )
        }

        val DialogColor by lazy { Color(di.application.getColor(R.color.colorDialog)) }
        val DialogTextColor by lazy { Color(di.application.getColor(R.color.colorDialogText)) }
    }

    override fun onCreate() {
        super.onCreate()
        di = DI(this)
    }
}
