package me.jameshunt.dhiffiechat.compose

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
import androidx.navigation.*
import androidx.navigation.compose.navigate
import kotlinx.coroutines.flow.map
import me.jameshunt.dhiffiechat.*
import me.jameshunt.dhiffiechat.R

class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(DhiffieChatApp.di.relationshipService) as T
    }
}

class HomeViewModel(
    userService: UserService
) : ViewModel() {

    data class FriendMessageData(
        val friendUserId: String,
        val alias: String,
        val count: Int
    )

    val friendMessageData: LiveData<List<FriendMessageData>> = userService
        .getFriends()
        .map { friends ->
            val summaries = userService.getMessageSummaries().associateBy { it.from }
            friends.map { friend ->
                FriendMessageData(
                    friendUserId = friend.userId,
                    alias = friend.alias,
                    count = summaries[friend.userId]?.count ?: 0
                )
            }
        }.asLiveData()
}

@Composable
fun HomeScreen(
    navController: NavController,
    onSendMessage: (gotCameraResult: () -> Unit) -> Unit
) {
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
            summaries.forEach { data ->
                CallToAction(data.alias, R.drawable.ic_baseline_qr_code_scanner_24) {
                    when (data.count == 0) {
                        true -> onSendMessage {
                            navController.navigateToSendMessage(data.friendUserId)
                        }
                        false -> navController.navigateToShowNextMessage(data.friendUserId)
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
