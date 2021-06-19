package me.jameshunt.dhiffiechat.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
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

inline fun <reified T : ViewModel> Fragment.injectedViewModel(
    noinline ownerProducer: () -> ViewModelStoreOwner = { this }
): Lazy<T> {
    return viewModels(
        ownerProducer = ownerProducer,
        factoryProducer = { InjectableViewModelFactory() })
}

inline fun <reified T : ViewModel> FragmentActivity.injectedViewModel(): Lazy<T> {
    return viewModels(factoryProducer = { InjectableViewModelFactory() })
}

class InjectableViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DhiffieChatApp.di.createInjected(modelClass)
    }
}
