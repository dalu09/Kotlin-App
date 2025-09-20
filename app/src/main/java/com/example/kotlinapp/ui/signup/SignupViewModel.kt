package com.example.kotlinapp.ui.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SignupViewModel : ViewModel() {

    private val _email = MutableLiveData<String>("")
    val email: LiveData<String> = _email

    private val _password = MutableLiveData<String>("")
    val password: LiveData<String> = _password

    private val _isPasswordVisible = MutableLiveData<Boolean>(false)
    val isPasswordVisible: LiveData<Boolean> = _isPasswordVisible


    private val _signupClicked = MutableLiveData<Unit?>()
    val signupClicked: LiveData<Unit?> = _signupClicked

    fun onEmailChanged(value: String) {
        _email.value = value
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = _isPasswordVisible.value?.not() ?: true
    }


    fun onSignupPressed() {
        _signupClicked.value = Unit
        _signupClicked.value = null
    }
}