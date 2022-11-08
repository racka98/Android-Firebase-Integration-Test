package work.racka.firebaseintegrationtest

import android.app.Application
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.logging.*

class FirebaseIntegrationApplication : Application() {
    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()
    val signInRequest: BeginSignInRequest
        get() = provideSignInRequest()
    val signUpRequest: BeginSignInRequest
        get() = provideSignUpRequest()
    val oneTapClient: SignInClient
        get() = Identity.getSignInClient(this)
    val httpClient: HttpClient
        get() = createHttpClient()

    private fun Application.provideSignInRequest() = BeginSignInRequest.builder()
        .setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId(getString(R.string.web_client_id))
                .setFilterByAuthorizedAccounts(true)
                .build()
        ).setAutoSelectEnabled(true)
        .build()

    private fun Application.provideSignUpRequest() = BeginSignInRequest.builder()
        .setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId(getString(R.string.web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .build()
        ).build()

    private fun createHttpClient() = HttpClient(OkHttp) {
        followRedirects = true
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }
    }
}