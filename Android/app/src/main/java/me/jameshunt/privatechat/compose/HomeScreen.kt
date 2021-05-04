package me.jameshunt.privatechat.compose

import LoadingIndicator
import android.util.Log
import androidx.compose.foundation.layout.*
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
import me.jameshunt.privatechat.PrivateChatApi.*
import me.jameshunt.privatechat.PrivateChatService
import me.jameshunt.privatechat.R

class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(DI.privateChatService) as T
    }
}

class HomeViewModel(private val apiService: PrivateChatService) : ViewModel() {

    private val _relationships: MutableLiveData<Relationships?> = MutableLiveData(null)
    private val _messageSummaries: MutableLiveData<List<MessageFromUserSummary>?> =
        MutableLiveData(null)

    data class FriendMessageData(
        val from: String,
        val count: Int
    )

    val friendMessageData: LiveData<List<FriendMessageData>?> = _relationships.combineWith(
        _messageSummaries
    ) { relationships: Relationships?, messageSummaries: List<MessageFromUserSummary>? ->
        relationships ?: return@combineWith null
        messageSummaries ?: return@combineWith null

        relationships.friends.map { friendUserId ->
            messageSummaries
                .firstOrNull { friendUserId == it.from }
                ?.let { FriendMessageData(it.from, it.count) }
                ?: FriendMessageData(from = friendUserId, count = 0)
        }
    }

    init {
        viewModelScope.launch {
            refresh()
        }
    }

    private suspend fun refresh() {
        _relationships.value = apiService.getUserRelationships()
        _messageSummaries.value = apiService.getMessageSummaries()
    }

}

@Composable
fun HomeScreen(navController: NavController, onSendMessage: (recipientUserId: String, gotCameraResult: () -> Unit) -> Unit) {
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


        val messageSummaries = viewModel.friendMessageData.observeAsState().value

        messageSummaries?.let { summaries ->
            Spacer(modifier = Modifier.height(8.dp))
            summaries.forEach {
                CallToAction(it.from, R.drawable.ic_baseline_qr_code_scanner_24) {
                    onSendMessage(it.from) {
                        navController.navigate("sendMessage")
                    }
                }
            }

        } ?: run {
            Spacer(modifier = Modifier.height(8.dp))
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                LoadingIndicator()
            }
        }
    }
}


fun <T, K, R> LiveData<T>.combineWith(
    liveData: LiveData<K>,
    block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block(this.value, liveData.value)
    }
    return result
}

