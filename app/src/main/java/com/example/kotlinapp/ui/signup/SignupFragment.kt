package com.example.kotlinapp.ui.signup

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kotlinapp.R
import com.example.kotlinapp.ui.signup.SignupViewModel.RegistrationState

class SignupFragment : Fragment() {

    private lateinit var viewModel: SignupViewModel

    // --- Vistas de la UI ---
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var passwordToggle: ImageButton
    private lateinit var signupButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_signup, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(SignupViewModel::class.java)

        // --- Inicialización de Vistas ---
        emailEdit = view.findViewById(R.id.editEmail)
        passwordEdit = view.findViewById(R.id.editPassword)
        passwordToggle = view.findViewById(R.id.btnPasswordToggle)
        signupButton = view.findViewById(R.id.btnContinue)
        progressBar = view.findViewById(R.id.progressBar)


        // --- Observadores y Listeners (sin cambios) ---
        val navOptions = androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.loginFragment, inclusive = true)
            .build()

        findNavController().navigate(R.id.loginFragment, null, navOptions)

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
        signupButton.setOnClickListener {
            viewModel.onSignupClicked()
        }

        // --- Observador del estado de registro (MODIFICADO) ---
        observeRegistrationState()
    }

    private fun observeRegistrationState() {
        viewModel.registrationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegistrationState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    signupButton.isEnabled = false
                }
                // MODIFICADO: Ahora manejamos el nuevo estado de éxito `AuthSuccess`
                is RegistrationState.AuthSuccess -> {
                    progressBar.visibility = View.GONE
                    signupButton.isEnabled = true
                    Toast.makeText(context, "Cuenta creada. Completa tu perfil.", Toast.LENGTH_SHORT).show()

                    // AÑADIDO: Preparamos el argumento para pasar el UID
                    val bundle = bundleOf("user_uid" to state.uid)

                    // AÑADIDO: Navegamos a la nueva pantalla de crear cuenta
                    // Asegúrate de tener esta acción definida en tu `nav_graph.xml`
                    findNavController().navigate(R.id.action_signupFragment_to_createAccountFragment, bundle)

                    // AÑADIDO: Reseteamos el estado en el ViewModel
                    viewModel.onNavigationComplete()
                }
                is RegistrationState.Error -> {
                    progressBar.visibility = View.GONE
                    signupButton.isEnabled = true
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is RegistrationState.Idle -> {
                    progressBar.visibility = View.GONE
                    signupButton.isEnabled = true
                }
            }
        }
    }
}