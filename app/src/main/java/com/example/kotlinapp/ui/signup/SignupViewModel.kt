package com.example.kotlinapp.ui.signup

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.repository.AuthRepository
import kotlinx.coroutines.launch


class SignupViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()


    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


    private val _email = MutableLiveData<String>("")
    val email: LiveData<String> = _email

    private val _password = MutableLiveData<String>("")
    val password: LiveData<String> = _password

    private val _isPasswordVisible = MutableLiveData<Boolean>(false)
    val isPasswordVisible: LiveData<Boolean> = _isPasswordVisible

    private val _registrationState = MutableLiveData<RegistrationState>(RegistrationState.Idle)
    val registrationState: LiveData<RegistrationState> = _registrationState


    fun onEmailChanged(value: String) {
        _email.value = value
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = _isPasswordVisible.value?.not() ?: true
    }


    fun onSignupClicked() {

        if (!isNetworkAvailable()) {
            _registrationState.value = RegistrationState.Error("No hay conexión a internet. Revisa tu red e inténtalo de nuevo.")
            return
        }


        val email = _email.value ?: ""
        val password = _password.value ?: ""


        if (email.isBlank() || password.isBlank()) {
            _registrationState.value = RegistrationState.Error("Email y contraseña no pueden estar vacíos.")
            return
        }


        viewModelScope.launch {
            try {
                _registrationState.value = RegistrationState.Loading
                val firebaseUser = authRepository.createUserInAuth(email, password)
                _registrationState.value = RegistrationState.AuthSuccess(firebaseUser.uid)

            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Ocurrió un error desconocido")
            }
        }
    }

    fun onNavigationComplete() {
        _registrationState.value = RegistrationState.Idle
    }


    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }


    sealed class RegistrationState {
        object Idle : RegistrationState()
        object Loading : RegistrationState()
        data class AuthSuccess(val uid: String) : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }
}