package me.jameshunt.dhiffiechat

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import me.jameshunt.dhiffiechat.service.EphemeralKeySyncService
import me.jameshunt.dhiffiechat.service.InitService
import me.jameshunt.dhiffiechat.service.LambdaApi
import me.jameshunt.dhiffiechat.service.UserService
import me.jameshunt.dhiffiechat.ui.compose.InjectableViewModelFactory

class LauncherActivity : FragmentActivity() {
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

class LauncherScreenViewModel(private val initService: InitService) : ViewModel() {
    enum class LauncherState {
        Loading,
        Done
    }

    private val disposables = CompositeDisposable()
    val state = MutableLiveData(LauncherState.Loading)

    fun load() {
        val disposable = initService.init()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { state.value = LauncherState.Done },
                onError = {
                    // ignore error, let home screen show error
                    state.value = LauncherState.Done
                }
            )
        disposables.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
