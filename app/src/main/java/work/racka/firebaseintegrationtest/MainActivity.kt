package work.racka.firebaseintegrationtest

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import work.racka.firebaseintegrationtest.auth.AppPagesState
import work.racka.firebaseintegrationtest.auth.AuthViewModel
import work.racka.firebaseintegrationtest.ui.compose.AuthProvider
import work.racka.firebaseintegrationtest.ui.compose.AuthScreen
import work.racka.firebaseintegrationtest.ui.theme.FirebaseIntegrationTestTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = applicationContext as FirebaseIntegrationApplication


        setContent {
            val authViewModel: AuthViewModel = remember {
                AuthViewModel(
                    auth = application.auth,
                    googleSignUpRequest = application.signUpRequest,
                    googleSignInRequest = application.signInRequest,
                    oneTapClient = application.oneTapClient,
                    client = application.httpClient
                )
            }
            FirebaseIntegrationTestTheme {
                AllScreens(authViewModel = authViewModel)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        println("Config changed: $newConfig")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newConfig.setLocales(newConfig.locales)
        }
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        return super.onRetainCustomNonConfigurationInstance()
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllScreens(
    authViewModel: AuthViewModel
) {

    DisposableEffect(Unit) {
        onDispose {

        }
    }
    println("Recomposed: AllScreens")
    val snackbarHostState = remember { SnackbarHostState() }

    val currentPageState by remember { authViewModel.currentPageState }
    val events by authViewModel.events.collectAsState(initial = AuthViewModel.AuthErrors.Nothing)

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(snackbarData = data)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (currentPageState) {
                is AppPagesState.Auth -> {
                    AuthScreen(
                        registerUser = authViewModel.registerUserState.value,
                        loginUser = authViewModel.loginUserState.value,
                        loginLoadingState = authViewModel.loginLoadingState.value,
                        signUpLoadingState = authViewModel.registerLoadingState.value,
                        onLogin = {
                            if (it == AuthProvider.EMAIL) {
                                authViewModel.loginUser()
                            } else if (it == AuthProvider.GOOGLE) {
                                // Google Stuff
                                authViewModel.onTapSignInWithGoogle()
                            }
                        },
                        onSignUp = {
                            if (it == AuthProvider.EMAIL) {
                                authViewModel.registerUser()
                            } else if (it == AuthProvider.GOOGLE) {
                                // Google Stuff
                                authViewModel.onTapSignUpWithGoogle()
                            }
                        },
                        updateLoginData = { authViewModel.updateLoginUserData(it) },
                        updateRegisterData = { authViewModel.updateRegisterUserData(it) },
                        onSignInWithGoogle = { authViewModel.signInWithGoogle(it) },
                        oneTapSignInResponse = authViewModel.oneTapSignInResultState.value,
                        oneTapSignUpResponse = authViewModel.oneTapSignUpResultState.value
                    )
                }
                is AppPagesState.GoogleSignIn -> {

                }
                is AppPagesState.Home -> {
                    val emailVerified = remember {
                        authViewModel.isEmailUserVerified()
                    }
                    val serverBackendState = authViewModel.serverResponseState
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "You are logged in!!")
                        Text(text = "Is user email verified: $emailVerified")
                        Text(text = "Backend Text: ${serverBackendState.text}")
                        if (serverBackendState.isLoading) {
                            LinearProgressIndicator()
                        }
                        Button(onClick = { authViewModel.authBackend() }) {
                            Text(text = "Auth Backend")
                        }
                        Button(onClick = { authViewModel.logOut() }) {
                            Text(text = "Logout")
                        }
                    }
                }
            }
        }
    }

    // Handle Events here
    LaunchedEffect(key1 = events) {
        handleEvents(
            events = events,
            snackbarHostState = snackbarHostState,
            scope = this
        )
    }
}

fun handleEvents(
    events: AuthViewModel.AuthErrors,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    when (events) {
        is AuthViewModel.AuthErrors.LoggedIn -> {
            scope.launch {
                snackbarHostState.showSnackbar(message = "Logged In Successfully!")
            }
        }
        is AuthViewModel.AuthErrors.NotAuthorized -> {
            scope.launch {
                snackbarHostState.showSnackbar(message = events.message)
            }
        }
        is AuthViewModel.AuthErrors.Error -> {
            scope.launch {
                snackbarHostState.showSnackbar(message = events.message)
            }
        }
        is AuthViewModel.AuthErrors.MissingCreds -> {
            scope.launch {
                snackbarHostState.showSnackbar(message = events.message)
            }
        }
        else -> {}
    }
}
