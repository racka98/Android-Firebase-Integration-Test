package work.racka.firebaseintegrationtest.auth

data class RegisterUser(
    val email: String = "",
    val correctEmail: Boolean = true,
    val password: String = "",
    val repeatPassword: String = "",
    val passwordMatch: Boolean = true
)
