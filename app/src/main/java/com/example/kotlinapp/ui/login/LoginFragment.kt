package com.example.kotlinapp.ui.login

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kotlinapp.R

class LoginFragment : Fragment() {

    private lateinit var viewModel: LoginViewModel

    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var passwordToggle: ImageButton
    private lateinit var loginButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var signUpText: TextView
    private lateinit var logoImage: ImageView
    private lateinit var titleText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)


        logoImage = view.findViewById(R.id.logoImage)
        emailEdit = view.findViewById(R.id.editEmail)
        passwordEdit = view.findViewById(R.id.editPassword)
        passwordToggle = view.findViewById(R.id.btnPasswordToggle)
        loginButton = view.findViewById(R.id.btnLogin)
        forgotPasswordText = view.findViewById(R.id.txtForgot)
        signUpText = view.findViewById(R.id.txtSignUp)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginUiState.Idle -> Unit

                is LoginUiState.Loading -> {
                    loginButton.isEnabled = false
                    loginButton.text = "Ingresando..."
                }

                is LoginUiState.Success -> {
                    loginButton.isEnabled = true
                    loginButton.text = "Login"
                    findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                }

                is LoginUiState.Error -> {
                    loginButton.isEnabled = true
                    loginButton.text = "Login"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.email.observe(viewLifecycleOwner) { current ->
            if (emailEdit.text.toString() != current) emailEdit.setText(current)
        }
        viewModel.password.observe(viewLifecycleOwner) { current ->
            if (passwordEdit.text.toString() != current) passwordEdit.setText(current)
        }
        viewModel.isPasswordVisible.observe(viewLifecycleOwner) { visible ->
            if (visible) {
                passwordEdit.transformationMethod = SingleLineTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.ic_visibility)
            } else {
                passwordEdit.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.ic_visibility_off)
            }
            passwordEdit.setSelection(passwordEdit.text?.length ?: 0)
        }


        emailEdit.addTextChangedListener { text -> viewModel.onEmailChanged(text?.toString() ?: "") }
        passwordEdit.addTextChangedListener { text -> viewModel.onPasswordChanged(text?.toString() ?: "") }
        passwordToggle.setOnClickListener { viewModel.togglePasswordVisibility() }


        loginButton.setOnClickListener {
            viewModel.login()
        }

        forgotPasswordText.setOnClickListener { viewModel.recoverPass()}


        signUpText.setOnClickListener {
            findNavController().navigate(R.id.signupFragment)
        }
    }
}