package work.racka.firebaseintegrationtest.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import work.racka.firebaseintegrationtest.auth.LoginUser

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginFields(
    modifier: Modifier = Modifier,
    loginUser: LoginUser,
    updateData: (LoginUser) -> Unit,
    loading: Boolean,
    onLogin: () -> Unit,
    focusManager: FocusManager = LocalFocusManager.current,
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
) {
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            value = loginUser.email,
            onValueChange = { updateData(loginUser.copy(email = it)) },
            label = { Text(text = "Email") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Email, contentDescription = "Email")
            },
            isError = !loginUser.emailCorrect,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        TextField(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            value = loginUser.password,
            onValueChange = { updateData(loginUser.copy(password = it)) },
            label = { Text(text = "Password") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Password, contentDescription = "Password")
            },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Show Password"
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    keyboardController?.hide()
                    onLogin()
                }
            )
        )

        Button(onClick = { onLogin(); keyboardController?.hide(); }, enabled = !loading) {
            if (loading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(text = "Login")
        }
    }
}