package me.jameshunt.dhiffiechat

import android.app.Application

class DhiffieChatApp: Application() {
    companion object {
        lateinit var di: DI
    }

    override fun onCreate() {
        super.onCreate()
        di = DI(this)
    }
}
