package me.jameshunt.privatechat.compose

import LoadingIndicator
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import kotlinx.coroutines.launch
import me.jameshunt.privatechat.DI
import me.jameshunt.privatechat.PrivateChatApi.MessageFromUserSummary
import me.jameshunt.privatechat.PrivateChatService
import me.jameshunt.privatechat.R

class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(DI.privateChatService) as T
    }
}

class HomeViewModel(private val apiService: PrivateChatService) : ViewModel() {

    private val _messageSummaries: MutableLiveData<List<MessageFromUserSummary>?> = MutableLiveData(null)
    val messageSummaries: LiveData<List<MessageFromUserSummary>?> = _messageSummaries

    init {
        viewModelScope.launch {
            refresh()
        }
    }

    private suspend fun refresh() {
        _messageSummaries.value = apiService.getMessageSummaries()
    }

}

@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())

    Column(
        Modifier
            .fillMaxHeight()
            .padding(8.dp)
    ) {

        Spacer(modifier = Modifier.height(8.dp))
        CallToAction(text = "Manage Friends", drawableId = R.drawable.ic_baseline_person_add_24) {
            Log.d("clicked", "click")
            navController.navigate("manageFriends")
        }


        val messageSummaries = viewModel.messageSummaries.observeAsState().value

        messageSummaries?.let { summaries ->
            summaries.forEach {
                Text(text = it.from)
            }

        } ?: run {
            Spacer(modifier = Modifier.height(8.dp))
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                LoadingIndicator()
            }
        }
    }
}

