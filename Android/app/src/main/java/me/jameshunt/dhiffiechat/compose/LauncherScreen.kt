package me.jameshunt.dhiffiechat.compose

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.UserService

class LauncherScreenViewModel(private val service: UserService): ViewModel() {
    enum class LauncherState {
        Loading,
        UserProfile,
        Home
    }

    val state = MutableLiveData(LauncherState.Loading)

    init {
        viewModelScope.launch {
            service.createIdentity()
            state.value = when (service.getAlias() == null) {
                true -> LauncherState.UserProfile
                false -> LauncherState.Home
            }
        }
    }
}

@Composable
fun LauncherScreen(toUserProfile: () -> Unit, toHome: () -> Unit) {
    val viewModel = injectedViewModel<LauncherScreenViewModel>()

    Scaffold {
        when (viewModel.state.observeAsState().value!!) {
            LauncherScreenViewModel.LauncherState.Loading -> LoadingIndicator()
            LauncherScreenViewModel.LauncherState.UserProfile -> toUserProfile()
            LauncherScreenViewModel.LauncherState.Home -> toHome()
        }
    }
}
