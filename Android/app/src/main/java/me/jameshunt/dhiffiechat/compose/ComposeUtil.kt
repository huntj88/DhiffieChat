package me.jameshunt.dhiffiechat.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
inline fun <reified T : ViewModel> injectedViewModel(key: String? = null): T {
    return viewModel(key = key, factory = InjectableViewModelFactory())
}

@Composable
fun LoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier
            .size(90.dp, 90.dp)
            .padding(16.dp),
        color = Color.Green,
        strokeWidth = 8.dp
    )
}
