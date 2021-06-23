package me.jameshunt.dhiffiechat.ui.profile

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
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

    Scaffold {
        Column {
            val textFieldColors = when (isSystemInDarkTheme()) {
                true -> darkThemeTextFieldColors()
                false -> TextFieldDefaults.outlinedTextFieldColors()
            }
            OutlinedTextField(
                value = alias,
                label = { Text("Alias") },
                colors = textFieldColors,
                placeholder = { Text("Alias") },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onValueChange = { aliasSave = it }
            )
            // TODO: Select icon

            val buttonColors = when (isSystemInDarkTheme()) {
                true -> ButtonDefaults.buttonColors()
                    false -> ButtonDefaults.buttonColors()
            }
            Button(
                colors = buttonColors,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                onClick = {
                    viewModel.setAlias(alias)
                    onAliasSet()
                },
                content = {
                    Text(
                        text = "Set Alias",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun darkThemeTextFieldColors(
    textColor: Color = LocalContentColor.current.copy(LocalContentAlpha.current),
    disabledTextColor: Color = textColor.copy(ContentAlpha.disabled),
    backgroundColor: Color = Color.Transparent,
    cursorColor: Color = MaterialTheme.colors.onPrimary,
    errorCursorColor: Color = MaterialTheme.colors.error,
    focusedBorderColor: Color =
        MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.high),
    unfocusedBorderColor: Color =
        MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.disabled),
    disabledBorderColor: Color = unfocusedBorderColor.copy(alpha = ContentAlpha.disabled),
    errorBorderColor: Color = MaterialTheme.colors.error,
    leadingIconColor: Color =
        MaterialTheme.colors.onPrimary.copy(alpha = TextFieldDefaults.IconOpacity),
    disabledLeadingIconColor: Color = leadingIconColor.copy(alpha = ContentAlpha.disabled),
    errorLeadingIconColor: Color = leadingIconColor,
    trailingIconColor: Color =
        MaterialTheme.colors.onPrimary.copy(alpha = TextFieldDefaults.IconOpacity),
    disabledTrailingIconColor: Color = trailingIconColor.copy(alpha = ContentAlpha.disabled),
    errorTrailingIconColor: Color = MaterialTheme.colors.error,
    focusedLabelColor: Color =
        MaterialTheme.colors.onPrimary.copy(alpha = ContentAlpha.high),
    unfocusedLabelColor: Color = MaterialTheme.colors.onPrimary.copy(ContentAlpha.medium),
    disabledLabelColor: Color = unfocusedLabelColor.copy(ContentAlpha.disabled),
    errorLabelColor: Color = MaterialTheme.colors.error,
    placeholderColor: Color = MaterialTheme.colors.onPrimary.copy(ContentAlpha.medium),
    disabledPlaceholderColor: Color = placeholderColor.copy(ContentAlpha.disabled)
): TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(
    textColor = textColor,
    disabledTextColor = disabledTextColor,
    backgroundColor = backgroundColor,
    cursorColor = cursorColor,
    errorCursorColor = errorCursorColor,
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    disabledBorderColor = disabledBorderColor,
    errorBorderColor = errorBorderColor,
    leadingIconColor = leadingIconColor,
    disabledLeadingIconColor = disabledLeadingIconColor,
    errorLeadingIconColor = errorLeadingIconColor,
    trailingIconColor = trailingIconColor,
    disabledTrailingIconColor = disabledTrailingIconColor,
    errorTrailingIconColor = errorTrailingIconColor,
    focusedLabelColor = focusedLabelColor,
    unfocusedLabelColor = unfocusedLabelColor,
    disabledLabelColor = disabledLabelColor,
    errorLabelColor = errorLabelColor,
    placeholderColor = placeholderColor,
    disabledPlaceholderColor = disabledPlaceholderColor
)
