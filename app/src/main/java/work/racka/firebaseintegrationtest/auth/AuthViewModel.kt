package work.racka.firebaseintegrationtest.auth

import android.util.Patterns
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInResult
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import work.racka.firebaseintegrationtest.FirebaseIntegrationApplication

class AuthViewModel(
    private val auth: FirebaseAuth,
    private val googleSignUpRequest: BeginSignInRequest,
    private val googleSignInRequest: BeginSignInRequest,
    private val oneTapClient: SignInClient,
    private val client: HttpClient
) : ViewModel() {

    private val currentPage = mutableStateOf(
        if (auth.currentUser == null) AppPagesState.Auth
        else AppPagesState.Home
    )
    val currentPageState: State<AppPagesState> = currentPage

    private val registerUser = mutableStateOf(RegisterUser())
    val registerUserState: State<RegisterUser> = registerUser

    private val oneTapSignInResult: MutableState<Result<BeginSignInResult?>?> = mutableStateOf(null)
    val oneTapSignInResultState: State<Result<BeginSignInResult?>?> = oneTapSignInResult

    private val oneTapSignUpResult: MutableState<Result<BeginSignInResult?>?> = mutableStateOf(null)
    val oneTapSignUpResultState: State<Result<BeginSignInResult?>?> = oneTapSignInResult

    private val loginUser = mutableStateOf(LoginUser())
    val loginUserState: State<LoginUser> = loginUser

    private val registerLoading = mutableStateOf(false)
    val registerLoadingState: State<Boolean> = registerLoading
    private val loginLoading = mutableStateOf(false)
    val loginLoadingState: State<Boolean> = loginLoading

    var serverResponseState by mutableStateOf(ServerResponseState())
        private set

    private val eventsChannel = Channel<AuthErrors>(Channel.UNLIMITED)
    val events: Flow<AuthErrors> = eventsChannel.receiveAsFlow()

    init {
        reloadData()
    }

    /**
     * BACKEND SERVER AUTH
     */
    fun authBackend() {
        auth.currentUser?.let { user ->
            user
            serverResponseState = serverResponseState.copy(isLoading = true)
            viewModelScope.launch {
                val url =
                    "https://firebase-integration-server-test-production.up.railway.app/authenticated"
                val result: String? = withContext(Dispatchers.IO) {
                    val tokenResult = user.getIdToken(true).await()
                    try {
                        tokenResult.token?.let { token ->
                            val result = client.get(url) { bearerAuth(token) }
                            when (result.status) {
                                HttpStatusCode.OK -> result.bodyAsText()
                                HttpStatusCode.Unauthorized -> "Unauthorized..."
                                else -> "Unknown Error"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        e.message
                    }
                }
                serverResponseState =
                    ServerResponseState(isLoading = false, text = result ?: "Some Error!")
            }
        }
    }

    /**
     * EMAIL AUTHENTICATION
     */

    fun updateRegisterUserData(user: RegisterUser) {
        val isEmailCorrect =
            user.email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(user.email).matches()
        val passwordMatches = user.password == user.repeatPassword
        val checkedUser = user.copy(correctEmail = isEmailCorrect, passwordMatch = passwordMatches)
        registerUser.value = checkedUser
    }

    fun updateLoginUserData(user: LoginUser) {
        val isEmailCorrect =
            user.email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(user.email).matches()
        val checkedUser = user.copy(emailCorrect = isEmailCorrect)
        loginUser.value = checkedUser
    }

    fun registerUser() {
        val user = registerUserState.value
        val notBlankDetails = user.email.isNotBlank() && user.password.isNotBlank()
        if (user.correctEmail && user.passwordMatch && notBlankDetails) {
            registerLoading.value = true
            viewModelScope.launch {
                try {
                    val authResult = withContext(Dispatchers.IO) {
                        auth.createUserWithEmailAndPassword(user.email, user.password)
                    }
                    authResult.apply {
                        addOnSuccessListener {
                            checkUserLoggedIn()
                            sendVerificationEmail()
                        }
                        addOnFailureListener { e ->
                            eventsChannel.trySend(AuthErrors.Error("Failed: ${e.message}"))
                        }
                        addOnCompleteListener { registerLoading.value = false }
                    }
                } catch (e: Exception) {
                    registerLoading.value = false
                    eventsChannel.send(AuthErrors.Error(message = "Error: ${e.message}"))
                }
            }
        }
    }

    fun loginUser() {
        val user = loginUser.value
        val notBlankDetails = user.email.isNotBlank() && user.password.isNotBlank()
        if (user.emailCorrect && notBlankDetails) {
            loginLoading.value = true
            viewModelScope.launch {
                try {
                    val authResult = withContext(Dispatchers.IO) {
                        auth.signInWithEmailAndPassword(user.email, user.password)
                    }
                    authResult.addOnSuccessListener { checkUserLoggedIn() }
                        .addOnFailureListener { e ->
                            eventsChannel.trySend(AuthErrors.Error("Failed: ${e.message}"))
                        }
                        .addOnCompleteListener { loginLoading.value = false }
                } catch (e: Exception) {
                    loginLoading.value = false
                    eventsChannel.send(AuthErrors.Error(message = "Error: ${e.message}"))
                }
            }
        }
    }

    fun isEmailUserVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false

    private fun sendVerificationEmail() {
        viewModelScope.launch {
            auth.currentUser?.sendEmailVerification()
                ?.addOnSuccessListener {
                    eventsChannel.trySend(AuthErrors.Success("Verification Email Sent!"))
                }?.addOnFailureListener { e ->
                    eventsChannel.trySend(AuthErrors.Error("Failed: ${e.message}"))
                }
        }
    }

    /**
     * GOOGLE AUTHENTICATION
     */

    fun onTapSignInWithGoogle() {
        viewModelScope.launch {
            loginLoading.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    oneTapClient.beginSignIn(googleSignInRequest).await()
                }
                oneTapSignInResult.value = Result.success(result)
                loginLoading.value = false
            } catch (e: Exception) {
                loginLoading.value = false
                eventsChannel.send(AuthErrors.Error(message = "Error: ${e.message}"))
                oneTapSignInResult.value = Result.failure(e)
            }
        }
    }

    fun onTapSignUpWithGoogle() {
        viewModelScope.launch {
            registerLoading.value = true
            try {
                println("VM Signing Up..")
                val result = oneTapClient.beginSignIn(googleSignUpRequest).await()
                println("VM Sign Up Success")
                oneTapSignUpResult.value = Result.success(result)
                registerLoading.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                registerLoading.value = false
                eventsChannel.send(AuthErrors.Error(message = "Error: ${e.message}"))
                oneTapSignUpResult.value = Result.failure(e)
            }
        }
    }

    fun signInWithGoogle(googleCredential: AuthCredential) {
        viewModelScope.launch {
            loginLoading.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    auth.signInWithCredential(googleCredential).await()
                }
                val isNewUser = result.additionalUserInfo?.isNewUser ?: false
                if (isNewUser) {
                    // TODO: Add to firestore
                }
                checkUserLoggedIn()
                loginLoading.value = false
            } catch (e: Exception) {
                loginLoading.value = false
                eventsChannel.send(AuthErrors.Error(message = "Error: ${e.message}"))
            }
        }
    }

    /**
     * COMMON STUFF
     */

    fun logOut() {
        viewModelScope.launch {
            val logoutListener = LogoutListener(
                userRemoved = { currentPage.value = AppPagesState.Auth }
            )
            auth.signOut()
            auth.addIdTokenListener(logoutListener)
        }
    }

    private fun reloadData() {
        auth.currentUser?.reload()
            ?.addOnSuccessListener { checkUserLoggedIn() }
    }

    private fun checkUserLoggedIn() {
        if (auth.currentUser == null) {
            eventsChannel.trySend(AuthErrors.NotAuthorized("Please Log in!"))
            currentPage.value = AppPagesState.Auth
        } else {
            eventsChannel.trySend(AuthErrors.LoggedIn)
            currentPage.value = AppPagesState.Home
            clearFields()
        }
    }

    private fun clearFields() {
        registerUser.value = RegisterUser()
        loginUser.value = LoginUser()
    }

    private fun userData() {
        auth.currentUser?.let { user ->
            val userProfChange = userProfileChangeRequest {
                displayName = "Someone"
            }
        }
    }

    private class LogoutListener(private val userRemoved: () -> Unit) :
        FirebaseAuth.IdTokenListener {
        override fun onIdTokenChanged(p0: FirebaseAuth) {
            if (p0.currentUser == null) {
                userRemoved()
            }
            p0.removeIdTokenListener(this)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as FirebaseIntegrationApplication
                AuthViewModel(
                    auth = application.auth,
                    googleSignUpRequest = application.signUpRequest,
                    googleSignInRequest = application.signInRequest,
                    oneTapClient = application.oneTapClient,
                    client = application.httpClient
                )
            }
        }
    }

    sealed class AuthErrors {
        object Nothing : AuthErrors()
        object LoggedIn : AuthErrors()
        class NotAuthorized(val message: String) : AuthErrors()
        class MissingCreds(val message: String) : AuthErrors()
        class Error(val message: String) : AuthErrors()
        class Success(val message: String) : AuthErrors()
    }
}