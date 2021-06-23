package me.jameshunt.dhiffiechat.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.fragment.NavHostFragment
import me.jameshunt.dhiffiechat.DhiffieChatApp
import me.jameshunt.dhiffiechat.R

abstract class ComposeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val colors = when (isSystemInDarkTheme()) {
                    true -> DhiffieChatApp.DarkColors
                    false -> DhiffieChatApp.LightColors
                }
                MaterialTheme(colors = colors) {
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

class InjectableViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DhiffieChatApp.di.createInjected(modelClass)
    }
}

@Composable
fun Fragment.BackAppBar(title: String, content: @Composable ColumnScope.() -> Unit) {
    val frag = this
    Column {
        TopAppBar(
            title = { Text(text = title) },
            backgroundColor = MaterialTheme.colors.primaryVariant,
            navigationIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = "Back",
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable {
                            NavHostFragment.findNavController(frag).popBackStack()
                        }
                )
            }
        )
        content()
    }
}