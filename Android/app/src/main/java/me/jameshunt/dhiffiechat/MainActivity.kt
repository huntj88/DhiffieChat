package me.jameshunt.dhiffiechat

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.compose.InjectableViewModelFactory

class MainActivity : FragmentActivity() {

    private val viewModel: LauncherScreenViewModel by viewModels { InjectableViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.state.observe(this) { state ->
            if (state == LauncherScreenViewModel.LauncherState.Done) {
                theme.applyStyle(R.style.Theme_DhiffieChat, true)
                setContentView(R.layout.activity_main)
            }
        }
        viewModel.load()
    }
}

class LauncherScreenViewModel(private val service: LauncherService): ViewModel() {
    enum class LauncherState {
        Loading,
        Done
    }

    val state = MutableLiveData(LauncherState.Loading)

    fun load() {
        viewModelScope.launch {
            service.init()
            state.value = LauncherState.Done
        }
    }
}