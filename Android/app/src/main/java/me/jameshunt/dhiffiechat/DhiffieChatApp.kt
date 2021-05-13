package me.jameshunt.dhiffiechat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DhiffieChatApp: Application() {
    companion object {
        lateinit var di: DI
    }

    override fun onCreate() {
        super.onCreate()
        di = DI(this)
    }
}

class InjectableViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DhiffieChatApp.di.createInjected(modelClass)
    }
}
