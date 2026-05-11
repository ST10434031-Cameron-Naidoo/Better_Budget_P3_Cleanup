package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase.Companion.getDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.repository.UserRepository
import com.dachkaboiz.betterbudget_bestbudget.data.utils.ImageUtils
import com.dachkaboiz.betterbudget_bestbudget.databinding.FragmentProfileBinding
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.UserViewModel
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.UserViewModelFactory
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.util.*

class ProfileFragment : Fragment(R.layout.fragment_profile_v2) {

    private lateinit var userViewModel: UserViewModel

    private lateinit var etEmail: EditText
    private lateinit var etProfileName: EditText
    private lateinit var etProfileLastName: EditText
    private lateinit var tvAge: TextView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvUploadProfilePicture: TextView
    private lateinit var tvTakeProfilePicture: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button

    private lateinit var spProfileDay: Spinner
    private lateinit var spProfileMonth: Spinner
    private lateinit var spProfileYear: Spinner
    private var isInitializing = false



    private val today = Calendar.getInstance()
    private val thisYear = today.get(Calendar.YEAR)

    private var currentImageUri: Uri? = null
    private val requestPermissionLauncher =
// Activity Result API launcher used for requesting camera permission.
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    private fun openCamera() {
        currentImageUri = ImageUtils.createImageFile(requireContext())
        takePictureLauncher.launch(currentImageUri)
    }
    // Camera launcher to take picture into provided URI
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentImageUri != null) {

                // Show image in UI
                ivProfilePicture.setImageURI(currentImageUri)

                // Do NOT save here — saving happens when UPDATE is clicked

            } else {
                Toast.makeText(requireContext(), "Could not take picture", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                currentImageUri = uri
                ivProfilePicture.setImageURI(uri)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupSpinners()

        setupViewModel()
        loadUserData()
        setupUpdateButton()
        setupLogoutButton()
        setupDeleteButton()
    }

    // ---------------------------------------------------------
    // View Binding
    // ---------------------------------------------------------
    private fun bindViews(view: View) {
        spProfileDay = view.findViewById(R.id.spProfileDay)
        spProfileMonth = view.findViewById(R.id.spProfileMonth)
        spProfileYear = view.findViewById(R.id.spProfileYear)

        etProfileName = view.findViewById(R.id.etProfileFirstName)
        etProfileLastName = view.findViewById(R.id.etProfileLastName)
        etEmail = view.findViewById(R.id.etProfileEmail)

        tvAge = view.findViewById(R.id.tvProfileAge)
        tvUploadProfilePicture = view.findViewById(R.id.tvUploadProfilePicture)
        tvTakeProfilePicture = view.findViewById(R.id.tvTakeProfilePicture)
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture)
        tvUploadProfilePicture.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        tvTakeProfilePicture.setOnClickListener {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }

        btnLogout = view.findViewById(R.id.btnProfileLogOut)
        btnUpdate = view.findViewById(R.id.btnProfileUpdate)
        btnDelete = view.findViewById(R.id.btnProfileDelete)
    }

    // ---------------------------------------------------------
    // ViewModel Setup
    // ---------------------------------------------------------
    private fun setupViewModel() {
        val dao = getDatabase(requireContext()).userDao()
        val repository = UserRepository(dao)
        val factory = UserViewModelFactory(repository)
        userViewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]
    }

    // ---------------------------------------------------------
    // Load User Data
    // ---------------------------------------------------------
    private fun loadUserData() {
        val prefs = requireActivity().getSharedPreferences("auth", 0)
        val email = prefs.getString("email", null)

        if (email != null) userViewModel.loadUser(email)

        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                etEmail.setText(user.email)
                etProfileName.setText(user.firstName ?: "")
                etProfileLastName.setText(user.surname ?: "")
                if (user.profilePicUri != null) {
                    ivProfilePicture.setImageURI(Uri.parse(user.profilePicUri))
                }
                spProfileYear.onItemSelectedListener = null
                spProfileMonth.onItemSelectedListener = null

                if (user.birthDate != null) {

                    val localDate = Instant.ofEpochMilli(user.birthDate)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()

                    isInitializing = true

                    val yearIndex = getYearIndex(localDate.year)
                    spProfileYear.setSelection(yearIndex)

                    val monthIndex = localDate.monthValue
                    spProfileMonth.setSelection(monthIndex)

                    updateDaysSpinner(spProfileDay, monthIndex, localDate.year)

                    val dayIndex = localDate.dayOfMonth
                    spProfileDay.setSelection(dayIndex)

                    isInitializing = false


                    tvAge.text = user.age?.toString() ?: ""
                }


            }

        }
    }

    // ---------------------------------------------------------
    // Spinner Setup
    // ---------------------------------------------------------

    private fun setupSpinners() {

        // Build adapters ONLY — no selections, no null/null
        setupYearSpinner(spProfileYear)
        updateMonthSpinner(spProfileMonth)
        updateDaysSpinner(spProfileDay, 1, 2000) // temporary valid month/year

        // Attach listeners
        spProfileYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (isInitializing) return
                // Year change does nothing
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spProfileMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (isInitializing) return
                val year = spProfileYear.selectedItem.toString().toIntOrNull()
                updateDaysSpinner(spProfileDay, pos, year)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    // ---------------------------------------------------------
    // Update Button Logic
    // ---------------------------------------------------------
    private fun setupUpdateButton() {
        btnUpdate.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val firstName = etProfileName.text.toString().trim().ifEmpty { null }
            val surname = etProfileLastName.text.toString().trim().ifEmpty { null }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val year = spProfileYear.selectedItem.toString().toIntOrNull()

            val month = spProfileMonth.selectedItemPosition.let {
                if (it > 0) it else null
            }

            val day = spProfileDay.selectedItemPosition.let {
                if (it > 0) it else null
            }

            var age: Int? = null
            var birthDateMillis: Long? = null

            if (year != null && month != null && day != null) {
                age = calculateAge(day, month, year)
                birthDateMillis = LocalDate.of(year, month, day)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            }

            userViewModel.updateUserProfile(
                firstName = firstName,
                lastName = surname,
                email = email,
                birthDate = birthDateMillis,
                age = age,
                profilePicUri = currentImageUri?.toString()
            )

            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .replace(R.id.mainFragment, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

    }
    private fun setupLogoutButton() {
        btnLogout.setOnClickListener {

            AlertDialog.Builder(requireContext())
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("LOG OUT") { _, _ ->

                    // 1. Clear saved login email
                    val prefs = requireActivity().getSharedPreferences("auth", 0)
                    prefs.edit().clear().apply()

                    // 2. Navigate to LoginActivity
                    val intent = Intent(requireContext(), LoginActivity::class.java)

                    // 3. Clear back stack so user cannot return with Back button
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                }
                .setNegativeButton("CANCEL", null)
                .show()
        }
    }

    private fun setupDeleteButton() {
        btnDelete.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, DeleteProfileFragment())
                .addToBackStack(null)
                .commit()
        }
    }


    // ---------------------------------------------------------
    // Helper Functions
    // ---------------------------------------------------------
    private fun getYearIndex(year: Int): Int =
        (year - 1930) + 1



    private fun setupYearSpinner(spinner: Spinner) {
        val years = mutableListOf("YYYY") + (1930..thisYear).map { it.toString() }

        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            years
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun updateMonthSpinner(spinner: Spinner) {
        val months = mutableListOf("MM") + listOf(
            "January","February","March","April","May","June",
            "July","August","September","October","November","December"
        )

        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            months
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }


    private fun updateDaysSpinner(spinner: Spinner, monthIndex: Int?, year: Int?) {
        val days = mutableListOf("DD")
        var daysInMonth:Int = 31

        if (monthIndex != null && year != null && monthIndex > 0) {
            val month = monthIndex
            daysInMonth = when (month) {
                2 -> if (year % 4 == 0) 29 else 28
                4, 6, 9, 11 -> 30
                else -> 31
            }
        }
        days += (1..daysInMonth).map { it.toString() }
        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            days
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }


    private fun calculateAge(day: Int, month: Int, year: Int): Int {
        val birthDate = LocalDate.of(year, month, day)
        return Period.between(birthDate, LocalDate.now()).years
    }
}
