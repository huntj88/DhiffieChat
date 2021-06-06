package me.jameshunt.dhiffiechat

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.ui.compose.InjectableViewModelFactory
import me.jameshunt.dhiffiechat.service.LauncherService

class LauncherActivity: FragmentActivity() {
    private val viewModel: LauncherScreenViewModel by viewModels { InjectableViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.state.observe(this) { state ->
            if (state == LauncherScreenViewModel.LauncherState.Done) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
        viewModel.load()
    }
}

class LauncherScreenViewModel(
    private val service: LauncherService,
    private val applicationScope: CoroutineScope
): ViewModel() {
    enum class LauncherState {
        Loading,
        Done
    }

    val state = MutableLiveData(LauncherState.Loading)

    fun load() {
        applicationScope.launch {
            service.init()
            state.value = LauncherState.Done
        }
    }
}
