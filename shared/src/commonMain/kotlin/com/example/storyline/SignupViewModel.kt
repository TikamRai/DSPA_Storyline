package com.example.storyline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SignupViewModel {
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _confirmationPassword = MutableStateFlow("")
    val confirmationPassword: StateFlow<String> = _confirmationPassword

    fun onNameChanged(newName: String) {
        _name.value = newName
    }

    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChanged(newPassword: String) {
        _password.value = newPassword
    }

    fun onConfirmationPasswordChanged(newConfirmationPassword: String) {
        _confirmationPassword.value = newConfirmationPassword
    }

    fun signUp() {
        // Handle sign-up logic here
        // For example, validate the input and call an API
    }
}
