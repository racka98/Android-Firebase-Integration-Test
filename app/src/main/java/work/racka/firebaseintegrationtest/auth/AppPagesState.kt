package work.racka.firebaseintegrationtest.auth

sealed class AppPagesState {
    object Auth: AppPagesState()
    object Home: AppPagesState()
    object GoogleSignIn: AppPagesState()
}
