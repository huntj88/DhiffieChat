package me.jameshunt.dhiffiechat.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.navigation.NavController
import kotlinx.coroutines.flow.map
import me.jameshunt.dhiffiechat.*
import me.jameshunt.dhiffiechat.R
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.util.*

class HomeViewModel(userService: UserService) : ViewModel() {

    data class FriendMessageData(
        val friendUserId: String,
        val alias: String,
        val count: Int,
        val mostRecentAt: Instant?
    )

    val friendMessageData: LiveData<List<FriendMessageData>> = userService
        .getFriends()
        .map { friends ->
            val summaries = userService.getMessageSummaries().associateBy { it.from }
            friends.map { friend ->
                val messageFromUserSummary = summaries[friend.userId]
                FriendMessageData(
                    friendUserId = friend.userId,
                    alias = friend.alias,
                    count = messageFromUserSummary?.count ?: 0,
                    mostRecentAt = messageFromUserSummary?.mostRecentCreatedAt
                )
            }.sortedByDescending { it.mostRecentAt }
        }.asLiveData()
}

@Composable
fun HomeScreen(
    toManageFriends: () -> Unit,
    toShowNextMessage: (friendUserId: String) -> Unit,
    toSendMessage: (friendUserId: String) -> Unit
) {
    val viewModel: HomeViewModel = injectedViewModel()
    val fabColor = activeColors().secondary

    Scaffold(
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .clickable { toManageFriends() }
            ) {
                Canvas(
                    modifier = Modifier
                        .wrapContentSize(align = Alignment.Center)
                        .padding(32.dp)
                ) {
                    drawCircle(fabColor, 80f)
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_baseline_qr_code_scanner_24),
                    contentDescription = "Manage Friends",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp),
                    colorFilter = ColorFilter.tint(activeColors().onSecondary)
                )
            }
        },
        content = {
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                val messageSummaries = viewModel.friendMessageData.observeAsState().value

                messageSummaries?.let { summaries ->
                    summaries.filter { it.count > 0 }.ShowList(
                        title = "Messages",
                        onItemClick = { toShowNextMessage(it.friendUserId) }
                    )

                    summaries.filter { it.count == 0 }.ShowList(
                        title = "Friends",
                        onItemClick = { toSendMessage(it.friendUserId) }
                    )
                } ?: run {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(Modifier.align(Alignment.CenterHorizontally)) {
                        LoadingIndicator()
                    }
                }
            }
        })
}

@Composable
fun List<HomeViewModel.FriendMessageData>.ShowList(
    title: String,
    onItemClick: (HomeViewModel.FriendMessageData) -> Unit
) {
    if (this.isNotEmpty()) {
        Text(text = title, modifier = Modifier.padding(start = 12.dp, top = 24.dp))
        this.forEach { data ->
            FriendCard(data) {
                onItemClick(data)
            }
        }
    }
}

@Composable
fun FriendCard(friendData: HomeViewModel.FriendMessageData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(12.dp)
            .clickable { onClick() }
    ) {
        Box(
            Modifier
                .clip(CircleShape)
                .border(1.5.dp, Color.Green, CircleShape)
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_baseline_qr_code_scanner_24),
                contentDescription = friendData.alias,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .requiredSize(50.dp)
                    .clip(CircleShape),
                colorFilter = ColorFilter.tint(userIdToColor(userId = friendData.friendUserId))
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = friendData.alias,
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )

            friendData.mostRecentAt?.let { mostRecentAt ->
                val duration = Duration.between(mostRecentAt, Instant.now())
                val days = duration.toDays()
                val hours = duration.toHours() % 24
                val minutes = duration.toMinutes() % 60
                val seconds = duration.toMillis() / 1000 % 60

                val daysString = if (days > 0) "$days day ago" else null
                val hoursString = if (hours > 0) "$hours hr ago" else null
                val minutesString = if (minutes > 0) "$minutes min ago" else null
                val secondsString = "$seconds sec ago"

                val elapsedTime = daysString ?: hoursString ?: minutesString ?: secondsString

                Text(
                    text = elapsedTime,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            val ctaText = when (friendData.count == 0) {
                true -> "Send a message"
                false -> "${friendData.count} unseen messages"
            }
            Text(
                text = ctaText,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.BottomStart)
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