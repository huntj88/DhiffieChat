package me.jameshunt.dhiffiechat.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.jameshunt.dhiffiechat.service.UserService

class UserProfileViewModel(
    private val service: UserService,
    applicationScope: CoroutineScope
) : ViewModel() {
    init {
        applicationScope.launch {
            service.createIdentity()
        }
    }

    val alias: LiveData<String?> = service.getAlias().map { it?.alias }.asLiveData()

    fun setAlias(alias: String) {
        service.setAlias(alias)
    }
}


@Composable
fun UserProfile(viewModel: UserProfileViewModel, onAliasSet: () -> Unit) {
    var aliasSave: String? by rememberSaveable { mutableStateOf(null) }
    val alias = aliasSave ?: viewModel.alias.observeAsState().value ?: ""
    Scaffold(backgroundColor = MaterialTheme.colors.primaryVariant) {
        Column {
            OutlinedTextField(
                value = alias,
                label = { Text("Alias") },
                colors = TextFieldDefaults.outlinedTextFieldColors(),
                placeholder = { Text("Alias") },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onValueChange = { aliasSave = it }
            )
            // TODO: Select icon
            Button(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                onClick = {
                    viewModel.setAlias(alias)
                    onAliasSet()
                },
                content = { Text(text = "Set Alias") }
            )
        }
    }
}
