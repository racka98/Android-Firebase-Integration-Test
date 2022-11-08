package work.racka.firebaseintegrationtest.ui.compose

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.BeginSignInResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import work.racka.firebaseintegrationtest.auth.LoginUser
import work.racka.firebaseintegrationtest.auth.RegisterUser

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AuthScreen(
    registerUser: RegisterUser,
    loginUser: LoginUser,
    loginLoadingState: Boolean,
    signUpLoadingState: Boolean,
    onLogin: (AuthProvider) -> Unit,
    onSignUp: (AuthProvider) -> Unit,
    updateLoginData: (LoginUser) -> Unit,
    updateRegisterData: (RegisterUser) -> Unit,
    onSignInWithGoogle: (AuthCredential) -> Unit,
    oneTapSignInResponse: Result<BeginSignInResult?>?,
    oneTapSignUpResponse: Result<BeginSignInResult?>?
) {

    val context = LocalContext.current
    val oneTapSignIn = remember {
        Identity.getSignInClient(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val credentials = oneTapSignIn.getSignInCredentialFromIntent(result.data)
                    val googleIdToken = credentials.googleIdToken
                    val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
                    onSignInWithGoogle(googleCredentials)
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        stickyHeader {
            Text(text = "Sign Up", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            SignUpFields(
                registerUser = registerUser,
                updateData = updateRegisterData,
                loading = signUpLoadingState,
                onSignUp = { onSignUp(AuthProvider.EMAIL) }
            )
        }

        stickyHeader {
            Text(text = "Login", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            LoginFields(
                loginUser = loginUser,
                updateData = updateLoginData,
                loading = loginLoadingState,
                onLogin = { onLogin(AuthProvider.EMAIL) }
            )
        }

        stickyHeader {
            Text(text = "With Google", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            Button(
                onClick = {
                    onLogin(AuthProvider.GOOGLE)
                },
                enabled = !loginLoadingState || !signUpLoadingState
            ) {
                if (loginLoadingState || signUpLoadingState) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(text = "Sign In With Google")
            }
        }
    }

    fun launch(signInResult: BeginSignInResult) {
        val intent = IntentSenderRequest.Builder(signInResult.pendingIntent.intentSender)
            .build()
        launcher.launch(intent)
    }

    if (oneTapSignInResponse != null) {
        if (oneTapSignInResponse.isSuccess) {
            oneTapSignInResponse.getOrNull()?.let {
                LaunchedEffect(key1 = Unit) { launch(it) }
            }
        } else if (oneTapSignInResponse.isFailure) {
            LaunchedEffect(key1 = Unit) {
                println("Sign In Failure")
                println(oneTapSignInResponse.exceptionOrNull()?.message)
                onSignUp(AuthProvider.GOOGLE)
            }
        }
    }

    if (oneTapSignUpResponse != null) {
        if (oneTapSignUpResponse.isSuccess) {
            oneTapSignUpResponse.getOrNull()?.let {
                LaunchedEffect(key1 = Unit) { launch(it) }
            }
        } else if (oneTapSignUpResponse.isFailure) {
            LaunchedEffect(key1 = Unit) {
                Log.d("TestTag", "Sign Up Failure")
                println(oneTapSignUpResponse.exceptionOrNull()?.message)
            }
        }
    }
}

enum class AuthProvider {
    EMAIL, GOOGLE;
}