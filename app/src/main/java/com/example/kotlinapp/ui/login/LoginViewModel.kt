package com.example.kotlinapp.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {


    private val _email = MutableLiveData<String>("")
    val email: LiveData<String> = _email

    private val _password = MutableLiveData<String>("")
    val password: LiveData<String> = _password

    private val _isPasswordVisible = MutableLiveData<Boolean>(false)
    val isPasswordVisible: LiveData<Boolean> = _isPasswordVisible


    private val _loginClicked = MutableLiveData<Unit?>()
    val loginClicked: LiveData<Unit?> = _loginClicked

    fun onEmailChanged(value: String) {
        _email.value = value
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = _isPasswordVisible.value?.not() ?: true
    }

    fun onLoginPressed() {
        _loginClicked.value = Unit

        _loginClicked.value = null
    }
}
