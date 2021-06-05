package me.jameshunt.dhiffiechat.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.jameshunt.dhiffiechat.DhiffieChatApp
import me.jameshunt.dhiffiechat.activeColors

abstract class ComposeFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = activeColors()) {
                    ScreenComposable()
                }
            }
        }
    }

    @Composable
    abstract fun ScreenComposable()
}

inline fun <reified T : ViewModel> Fragment.injectedViewModel(): Lazy<T> {
    return viewModels { InjectableViewModelFactory() }
}

class InjectableViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DhiffieChatApp.di.createInjected(modelClass)
    }
}
