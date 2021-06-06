package me.jameshunt.dhiffiechat.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
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

    fun getAlias(): String? = service.getAlias()?.alias

    fun setAlias(alias: String) {
        service.setAlias(alias)
    }
}


@Composable
fun UserProfile(viewModel: UserProfileViewModel, onAliasSet: () -> Unit) {
    var alias by rememberSaveable { mutableStateOf(viewModel.getAlias() ?: "") }
    Scaffold {
        Column {
            TextField(
                value = alias,
                placeholder = { Text("Alias") },
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                onValueChange = { alias = it }
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
