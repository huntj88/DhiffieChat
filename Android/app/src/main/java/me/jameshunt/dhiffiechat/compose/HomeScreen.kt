package me.jameshunt.dhiffiechat.compose

import LoadingIndicator
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.navigate
import kotlinx.coroutines.flow.map
import me.jameshunt.dhiffiechat.*
import me.jameshunt.dhiffiechat.R
import java.math.BigInteger
import java.util.*

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
                FriendCard(data) {
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

@Composable
fun FriendCard(friendData: HomeViewModel.FriendMessageData, onClick: () -> Unit) {
    Card(
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = BorderStroke(width = 1.5.dp, Color.LightGray)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Image(
                painter = painterResource(id = R.drawable.ic_baseline_qr_code_scanner_24),
                contentDescription = friendData.alias,
                contentScale = ContentScale.Fit,
                modifier = Modifier.requiredSize(50.dp),
                colorFilter = ColorFilter.tint(userIdToColor(userId = friendData.friendUserId))
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = friendData.alias,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            val ctaText = when (friendData.count == 0) {
                true -> "Send a message"
                false -> "${friendData.count} unseen messages"
            }
            Text(
                text = ctaText,
                fontSize = 16.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun userIdToColor(userId: String): Color {
    val bytes = Base64.getDecoder().decode(userId)
    val hex = String.format("%040x", BigInteger(1, bytes))
    val red = Integer.parseInt(hex.substring(0, 2), 16)
    val green = Integer.parseInt(hex.substring(2, 4), 16)
    val blue = Integer.parseInt(hex.substring(4, 6), 16)
    return Color(red, green, blue)
}