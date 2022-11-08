package work.racka.firebaseintegrationtest.auth

data class LoginUser(
    val email: String = "",
    val emailCorrect: Boolean = true,
    val password: String = ""
)
