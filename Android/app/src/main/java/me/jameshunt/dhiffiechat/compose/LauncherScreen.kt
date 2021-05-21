package me.jameshunt.dhiffiechat.compose

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.LauncherService

class LauncherScreenViewModel(private val service: LauncherService): ViewModel() {
    enum class LauncherState {
        Start,
        Loading,
        UserProfile,
        Home
    }

    val state = MutableLiveData(LauncherState.Start)

    fun load() {
        if (state.value == LauncherState.Loading) return
        state.value = LauncherState.Loading

        viewModelScope.launch {
            service.init()
            state.value = when (service.isFirstLaunch()) {
                true -> LauncherState.UserProfile
                false -> LauncherState.Home
            }
        }
    }
}

@Composable
fun LauncherScreen(toUserProfile: () -> Unit, toHome: () -> Unit) {
    val viewModel = injectedViewModel<LauncherScreenViewModel>()
    viewModel.load()

    Scaffold {
        when (viewModel.state.observeAsState().value!!) {
            LauncherScreenViewModel.LauncherState.Start,
            LauncherScreenViewModel.LauncherState.Loading -> LoadingIndicator()
            LauncherScreenViewModel.LauncherState.UserProfile -> toUserProfile()
            LauncherScreenViewModel.LauncherState.Home -> toHome()
        }
    }
}
