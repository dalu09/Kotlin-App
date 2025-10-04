package com.example.kotlinapp.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.AuthRepository
import kotlinx.coroutines.launch

// Estados para manejar Loading/Éxito/Error en la UI
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {

    private val repo = AuthRepository()
    private val _email = MutableLiveData<String>("")
    val email: LiveData<String> = _email

    private val _password = MutableLiveData<String>("")
    val password: LiveData<String> = _password

    private val _isPasswordVisible = MutableLiveData<Boolean>(false)
    val isPasswordVisible: LiveData<Boolean> = _isPasswordVisible


    private val _loginClicked = MutableLiveData<Unit?>()
    val loginClicked: LiveData<Unit?> = _loginClicked

    private val _uiState = MutableLiveData<LoginUiState>(LoginUiState.Idle)
    val uiState: LiveData<LoginUiState> = _uiState

    fun onEmailChanged(value: String) {
        _email.value = value
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = _isPasswordVisible.value?.not() ?: true
    }

    fun login() {
        val e = _email.value?.trim().orEmpty()
        val p = _password.value.orEmpty()

        if (e.isEmpty() || p.isEmpty()) {
            _uiState.value = LoginUiState.Error("Ingrese Email y contraseña")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                repo.signIn(e, p)
                _uiState.value = LoginUiState.Success
            } catch (ex: Exception) {
                _uiState.value = LoginUiState.Error(mapAuthError(ex))
            }
        }
    }

    //Recuperar contraseña
    fun recoverPass() {
        val e = _email.value?.trim().orEmpty()
        if (e.isEmpty()) {
            _uiState.value = LoginUiState.Error("Escribe tu email para recuperar contraseña")
            return
        }
        viewModelScope.launch {
            try {
                repo.sendPasswordReset(e)
                _uiState.value = LoginUiState.Error("Te enviamos un correo para restablecer la contraseña")
            } catch (ex: Exception) {
                _uiState.value = LoginUiState.Error(mapAuthError(ex))
            }
        }
    }

    private fun mapAuthError(ex: Exception): String {
        val msg = ex.message?.lowercase().orEmpty()
        return when {
            "there is no user" in msg || "no user record" in msg -> "No existe una cuenta con ese email"
            "badly formatted" in msg || "formatted" in msg       -> "Email inválido"
            "password" in msg && "invalid" in msg                -> "Contraseña incorrecta"
            "network" in msg                                     -> "Sin conexión. Intenta de nuevo"
            else                                                 -> "Error al iniciar sesión"
        }
    }
}
