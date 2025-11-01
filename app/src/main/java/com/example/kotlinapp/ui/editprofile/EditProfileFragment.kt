package com.example.kotlinapp.ui.editprofile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kotlinapp.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class EditProfileFragment : Fragment() {

    private lateinit var viewModel: EditProfileViewModel


    private lateinit var usernameEdit: EditText
    private lateinit var descriptionEdit: TextInputEditText
    private lateinit var sportsSelector: TextView
    private lateinit var sportsChipGroup: ChipGroup
    private lateinit var updateButton: Button
    private lateinit var progressBar: ProgressBar


    private lateinit var profileImageView: ImageView
    private lateinit var textChangePhoto: TextView


    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>


    private val sportsOptions = arrayOf("Football", "Basketball", "Volleyball")
    private val selectedSports = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    try {

                        val bitmap = MediaStore.Images.Media.getBitmap(
                            requireActivity().contentResolver,
                            imageUri
                        )

                        viewModel.saveProfileImage(bitmap)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel = ViewModelProvider(this).get(EditProfileViewModel::class.java)


        usernameEdit = view.findViewById(R.id.editUsername)
        descriptionEdit = view.findViewById(R.id.editDescription)
        sportsSelector = view.findViewById(R.id.sportsSelector)
        sportsChipGroup = view.findViewById(R.id.sportsChipGroup)
        updateButton = view.findViewById(R.id.btnUpdateProfile)
        progressBar = view.findViewById(R.id.progressBar)


        profileImageView = view.findViewById(R.id.profileImageView)
        textChangePhoto = view.findViewById(R.id.textChangePhoto)


        sportsSelector.setOnClickListener {
            showSportsSelectionDialog()
        }
        updateButton.setOnClickListener {
            onSaveClicked()
        }


        profileImageView.setOnClickListener { openGallery() }
        textChangePhoto.setOnClickListener { openGallery() }


        setupObservers()


        viewModel.loadUserProfile()
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun setupObservers() {

        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                usernameEdit.setText(user.username)
                descriptionEdit.setText(user.description)

                selectedSports.clear()
                selectedSports.addAll(user.sportList)
                updateSportsChips()


                viewModel.loadInitialProfileImage()
            }
        }


        viewModel.profileImage.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap)
            } else {

                profileImageView.setImageResource(R.drawable.profle_default)
            }
        }


        viewModel.updateState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EditProfileViewModel.UpdateState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    updateButton.isEnabled = false
                }
                is EditProfileViewModel.UpdateState.Success -> {
                    progressBar.visibility = View.GONE
                    updateButton.isEnabled = true
                    Toast.makeText(requireContext(), "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editProfileFragment_to_profileFragment)
                }
                is EditProfileViewModel.UpdateState.Error -> {
                    progressBar.visibility = View.GONE
                    updateButton.isEnabled = true
                    Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is EditProfileViewModel.UpdateState.Idle -> {
                    progressBar.visibility = View.GONE
                    updateButton.isEnabled = true
                }
            }
        }


        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun onSaveClicked() {
        val username = usernameEdit.text.toString()
        val description = descriptionEdit.text.toString()
        val sports = selectedSports.toList()

        viewModel.updateUserProfile(username, description, sports)
    }


    private fun showSportsSelectionDialog() {
        val checkedItems = sportsOptions.map { selectedSports.contains(it) }.toBooleanArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Sports")
            .setMultiChoiceItems(sportsOptions, checkedItems) { _, which, isChecked ->
                val sport = sportsOptions[which]
                if (isChecked) {
                    selectedSports.add(sport)
                } else {
                    selectedSports.remove(sport)
                }
            }
            .setPositiveButton("OK") { dialog, _ ->
                updateSportsChips()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun updateSportsChips() {
        sportsChipGroup.removeAllViews()
        selectedSports.forEach { sport ->
            val chip = Chip(sportsChipGroup.context).apply {
                text = sport
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedSports.remove(sport)
                    updateSportsChips()
                }
            }
            sportsChipGroup.addView(chip)
        }
    }
}