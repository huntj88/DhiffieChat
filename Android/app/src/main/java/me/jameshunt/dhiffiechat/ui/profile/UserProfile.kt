package me.jameshunt.dhiffiechat.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
import kotlinx.coroutines.flow.map
import me.jameshunt.dhiffiechat.service.UserService
import me.jameshunt.dhiffiechat.ui.compose.StyledTextField

class UserProfileViewModel(private val service: UserService) : ViewModel() {
    val alias: LiveData<String?> = service.getAlias().map { it?.alias }.asLiveData()

    fun setAlias(alias: String) {
        service.setAlias(alias)
    }
}


@Composable
fun UserProfile(viewModel: UserProfileViewModel, onAliasSet: () -> Unit) {
    var aliasSave: String? by rememberSaveable { mutableStateOf(null) }
    val alias = aliasSave ?: viewModel.alias.observeAsState().value ?: ""

    Scaffold {
        Column {
            StyledTextField(
                value = alias,
                labelString = "Alias",
                onValueChange = { aliasSave = it}
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
                content = {
                    Text(
                        text = "Confirm",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            )
        }
    }
}


