package me.jameshunt.dhiffiechat.compose

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.*
import me.jameshunt.dhiffiechat.DhiffieChatApi.*
import me.jameshunt.dhiffiechat.R
import net.glxn.qrgen.android.MatrixToImageWriter

class ManageFriendsViewModel(
    private val userService: UserService,
    identityManager: IdentityManager
) : ViewModel() {

    private val _relationships: MutableLiveData<Relationships?> = MutableLiveData(null)
    val relationships: LiveData<Relationships?> = _relationships
    val userId = identityManager.getIdentity().toUserId()

    init {
        viewModelScope.launch {
            refresh()
        }
    }

    fun addFriend(userId: String, alias: String) {
        viewModelScope.launch {
            userService.addFriend(userId, alias)
        }
    }

    private suspend fun refresh() {
        _relationships.value = userService.getRelationships()
    }
}

@Composable
fun ManageFriendsScreen() {
    val viewModel: ManageFriendsViewModel = injectedViewModel()
    var isShareOpen by remember { mutableStateOf(false) }
    var isScanOpen by remember { mutableStateOf(false) }
    var isAliasOpen by remember { mutableStateOf(false) }
    var alias by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxHeight()
            .padding(8.dp)
    ) {
        CallToAction(text = "Share QR", drawableId = R.drawable.ic_baseline_qr_code_scanner_24) {
            isShareOpen = true
            Log.d("clicked", "click")
        }
        Spacer(modifier = Modifier.height(8.dp))
        CallToAction(text = "Scan QR", drawableId = R.drawable.ic_baseline_qr_code_scanner_24) {
            isScanOpen = true
            Log.d("clicked", "click")
        }

        val relationships = viewModel.relationships.observeAsState().value

        relationships?.let {
            RequestList("Received Requests", it.receivedRequests) { userId ->
                isScanOpen = true
            }
            RequestList("Sent Requests", it.sentRequests) {}
        } ?: run {
            Spacer(modifier = Modifier.height(8.dp))
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                LoadingIndicator()
            }
        }
    }

    if (isShareOpen) {
        Dialog(onDismissRequest = { isShareOpen = false }) {
            Card {
                QRCodeImage(viewModel.userId)
            }
        }
    }

    if (isScanOpen) {
        Dialog(onDismissRequest = { isScanOpen = false }) {
            Card {
                QRScanner {
                    userId = it
                    isScanOpen = false
                    isAliasOpen = true
                }
            }
        }
    }

    if (isAliasOpen) {
        Dialog(onDismissRequest = { isAliasOpen = false }) {
            Card {
                Column {
                    TextField(value = alias, onValueChange = {
                        alias = it
                    })
                    Button(onClick = {
                        viewModel.addFriend(userId!!, alias)
                    }) {
                        Text(text = "Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun RequestList(title: String, requestList: List<String>, ifItemClicked: (userId: String) -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(text = title, fontSize = 30.sp)
    requestList.forEach { userId ->
        CallToAction(text = userId, drawableId = R.drawable.ic_baseline_qr_code_scanner_24) {
            ifItemClicked(userId)
        }
    }

    if (requestList.isEmpty()) {
        Card(
            elevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(width = 1.5.dp, Color.LightGray)
        ) {
            Text(
                text = "No Results",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun QRCodeImage(data: String) {
    val result1 = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 400, 400)
    Image(
        bitmap = MatrixToImageWriter.toBitmap(result1).asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.requiredSize(350.dp)
    )
}