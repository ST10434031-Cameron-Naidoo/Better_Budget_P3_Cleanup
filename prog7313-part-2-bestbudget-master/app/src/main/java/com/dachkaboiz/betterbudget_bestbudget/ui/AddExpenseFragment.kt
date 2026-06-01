package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.SnoozeCount
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseSnoozeRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class AddExpenseFragment : Fragment(R.layout.fragment_add_expense_v2) {

    private lateinit var repository: ExpenseRepository
    private lateinit var snoozeRepository: FirebaseSnoozeRepository
    private lateinit var goalRepository: FirebaseCategoryGoalRepository
    private val firebaseCategoryRepository = FirebaseCategoryRepository()
    private lateinit var subCategoryRepository: SubCategoryRepository

    private lateinit var spinnerSubCategory: Spinner

    private var editingExpenseId: String? = null
    private var selectedCategoryFirebaseId: String = ""
    private var firebaseSubCategoryID: String = ""
    private var categoryList: List<Category> = emptyList()

    // Holds the expense waiting for the snooze dialog result
    private var pendingExpense: Expense? = null

    // Prevents stale dialog results from firing on subsequent dialogs
    private var isWaitingForSnoozeResult = false

    private var currentImageUri: Uri? = null
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var vPhotoPlaceholder: View

    private val currentUserEmail: String by lazy {
        requireActivity().getSharedPreferences("auth", 0).getString("email", "") ?: ""
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(
                requireContext(),
                "Camera permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }

    private fun createImageUri(): Uri {
        val photoFile = File(
            requireContext().externalCacheDir,
            "expense_${System.currentTimeMillis()}.jpg"
        )
        photoFile.createNewFile()
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    currentImageUri = uri
                    updatePhotoUI()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to persist permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private fun openCamera() {
        currentImageUri = createImageUri()
        currentImageUri?.let { takePictureLauncher.launch(it) }
    }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentImageUri != null) {
                ivPhotoPreview.setImageURI(currentImageUri)
                ivPhotoPreview.visibility = View.VISIBLE
                vPhotoPlaceholder.visibility = View.GONE
            } else {
                Toast.makeText(
                    requireContext(),
                    "Could not take picture",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun updatePhotoUI() {
        if (currentImageUri != null) {
            ivPhotoPreview.setImageURI(currentImageUri)
            ivPhotoPreview.visibility = View.VISIBLE
            vPhotoPlaceholder.visibility = View.GONE
        } else {
            ivPhotoPreview.visibility = View.GONE
            vPhotoPlaceholder.visibility = View.VISIBLE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        repository           = ExpenseRepository(uid)
        snoozeRepository     = FirebaseSnoozeRepository(uid)
        goalRepository       = FirebaseCategoryGoalRepository(uid)
        subCategoryRepository = SubCategoryRepository(uid)

        editingExpenseId = arguments?.getString("expenseId")

        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerExpenseCategory)
        spinnerSubCategory  = view.findViewById(R.id.spinnerExpenseSubCategory)
        val etDay           = view.findViewById<EditText>(R.id.etExpenseDay)
        val etMonth         = view.findViewById<EditText>(R.id.etExpenseMonth)
        val etYear          = view.findViewById<EditText>(R.id.etExpenseYear)
        val etDescription   = view.findViewById<EditText>(R.id.etExpenseDescription)
        val etAmount        = view.findViewById<EditText>(R.id.etExpenseAmount)
        val btnCancel       = view.findViewById<Button>(R.id.btnExpenseCancel)
        val btnAdd          = view.findViewById<Button>(R.id.btnExpenseAdd)
        ivPhotoPreview      = view.findViewById(R.id.ivPhotoPreview)
        vPhotoPlaceholder   = view.findViewById(R.id.vPhotoPlaceholder)
        val btnTakePhoto    = view.findViewById<Button>(R.id.btnTakePhoto)
        val btnGallery      = view.findViewById<Button>(R.id.btnAddFromGallery)

        btnTakePhoto.setOnClickListener {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
        btnGallery.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Listen for result from SnoozeDialogFragment
        // isWaitingForSnoozeResult guards against stale results
        // firing from a previous dialog session
        parentFragmentManager.setFragmentResultListener(
            "snooze_result",
            viewLifecycleOwner
        ) { _, bundle ->

            if (!isWaitingForSnoozeResult) return@setFragmentResultListener
            isWaitingForSnoozeResult = false

            val action  = bundle.getString("action")
            val expense = pendingExpense ?: return@setFragmentResultListener

            when (action) {

                "USE_SNOOZE" -> {
                    // Save expense and save a SnoozeCount record
                    // The SnoozeCount record tells GoalHomeFragment
                    // to exclude this expense from the progress bar
                    lifecycleScope.launch {
                        repository.insertExpense(expense)

                        val snoozeId = snoozeRepository.generateSnoozeId()
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = expense.expenseDate
                        }
                        val record = SnoozeCount(
                            snoozeId   = snoozeId,
                            expenseId  = expense.expenseID,
                            categoryId = expense.categoryId,
                            month      = cal.get(Calendar.MONTH) + 1,
                            year       = cal.get(Calendar.YEAR)
                        )
                        snoozeRepository.saveSnooze(record)

                        Toast.makeText(
                            requireContext(),
                            "Expense saved. Snooze used.",
                            Toast.LENGTH_SHORT
                        ).show()
                        pendingExpense = null
                        parentFragmentManager.popBackStack()
                    }
                }

                "SAVE" -> {
                    // Save expense without a snooze record
                    // It WILL count toward the maximum on the progress bar
                    lifecycleScope.launch {
                        repository.insertExpense(expense)
                        Toast.makeText(
                            requireContext(),
                            "Expense saved (over budget).",
                            Toast.LENGTH_SHORT
                        ).show()
                        pendingExpense = null
                        parentFragmentManager.popBackStack()
                    }
                }

                "CANCEL" -> {
                    // User cancelled — do not save anything
                    pendingExpense = null
                    Toast.makeText(
                        requireContext(),
                        "Expense not saved.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Load categories from Firebase
        firebaseCategoryRepository.getCategories(uid) { list ->
            categoryList = list

            if (categoryList.isEmpty()) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Please create a category first.",
                        Toast.LENGTH_LONG
                    ).show()
                    parentFragmentManager.popBackStack()
                }
                return@getCategories
            }

            val categoryNames = categoryList.map { it.categoryName }
            requireActivity().runOnUiThread {
                spinnerCategory.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    categoryNames
                )
            }

            selectedCategoryFirebaseId = categoryList[0].firebaseId
            loadSubCategoriesFor(selectedCategoryFirebaseId)

            spinnerCategory.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>, v: View?,
                        position: Int, id: Long
                    ) {
                        selectedCategoryFirebaseId = categoryList[position].firebaseId
                        loadSubCategoriesFor(selectedCategoryFirebaseId)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
        }

        // Pre-fill fields if editing an existing expense
        lifecycleScope.launch {
            if (editingExpenseId != null) {
                val expense = repository.getExpenseById(editingExpenseId!!)
                if (expense != null) {
                    etAmount.setText(expense.expenseAmount.toString())
                    etDescription.setText(expense.expenseDescription ?: "")
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = expense.expenseDate
                    }
                    etDay.setText(
                        cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                    )
                    etMonth.setText(
                        (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                    )
                    etYear.setText(cal.get(Calendar.YEAR).toString())
                    expense.imageUri?.let {
                        currentImageUri = Uri.parse(it)
                        updatePhotoUI()
                    }
                }
            } else {
                val cal = Calendar.getInstance()
                etDay.setText(
                    cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                )
                etMonth.setText(
                    (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                )
                etYear.setText(cal.get(Calendar.YEAR).toString())
            }
        }

        btnAdd.setOnClickListener {
            val day    = etDay.text.toString().trim().toIntOrNull()
            val month  = etMonth.text.toString().trim().toIntOrNull()
            val year   = etYear.text.toString().trim().toIntOrNull()
            val amount = etAmount.text.toString().trim().toDoubleOrNull()
            val description = etDescription.text.toString().trim()

            if (day == null || month == null || year == null ||
                amount == null || amount <= 0
            ) {
                Toast.makeText(
                    requireContext(),
                    "Please fill all fields correctly.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val cal = Calendar.getInstance().apply {
                set(year, month - 1, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val id = editingExpenseId ?: repository.generateExpenseId()

            val expense = Expense(
                expenseID            = id,
                userEmail            = currentUserEmail,
                categoryId           = selectedCategoryFirebaseId,
                subCategoryId        = if (firebaseSubCategoryID.isBlank()) null
                else firebaseSubCategoryID,
                expenseAmount        = amount,
                expenseDate          = cal.timeInMillis,
                expenseDescription   = description.ifEmpty { null },
                imageUri             = currentImageUri?.toString(),
                imageName            = null,
                imageDescription     = null,
                automationFrequency  = null,
                automationMultiplier = null
            )

            lifecycleScope.launch {

                // Editing an existing expense — just update, no snooze check
                if (editingExpenseId != null) {
                    repository.updateExpense(expense)
                    Toast.makeText(
                        requireContext(),
                        "Expense updated!",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                    return@launch
                }

                // Check if a goal exists for this category this month
                val allGoals = goalRepository.getAllGoals()
                val goal = allGoals.firstOrNull {
                    it.categoryId == selectedCategoryFirebaseId &&
                            it.month == month &&
                            it.year == year
                }

                val maxGoal = goal?.maxGoal

                if (maxGoal != null) {

                    // Get all expenses for this category this month
                    val existingExpenses = repository
                        .getExpensesByCategory(selectedCategoryFirebaseId)
                        .filter {
                            val expCal = Calendar.getInstance().apply {
                                timeInMillis = it.expenseDate
                            }
                            expCal.get(Calendar.MONTH) + 1 == month &&
                                    expCal.get(Calendar.YEAR) == year
                        }

                    // Get snoozed IDs to exclude from current total
                    val snoozedIds = snoozeRepository.getSnoozedExpenseIds(
                        selectedCategoryFirebaseId, month, year
                    )

                    // Only count non-snoozed expenses toward the total
                    val currentTotal = existingExpenses
                        .filter { it.expenseID !in snoozedIds }
                        .sumOf { it.expenseAmount }

                    if (currentTotal + amount > maxGoal) {

                        // Check how many snoozes the user has left this month
                        val snoozesUsed = snoozeRepository.getSnoozeCountForMonth(
                            month, year
                        )
                        val snoozesLeft = maxOf(0, 2 - snoozesUsed)

                        val catName = categoryList
                            .find { it.firebaseId == selectedCategoryFirebaseId }
                            ?.categoryName ?: "this category"

                        // Store the expense and set the flag before
                        // showing the dialog so the listener knows
                        // this result is valid
                        pendingExpense = expense
                        isWaitingForSnoozeResult = true

                        SnoozeDialogFragment.newInstance(
                            categoryName = catName,
                            currentTotal = currentTotal,
                            maxGoal      = maxGoal,
                            newAmount    = amount,
                            snoozesLeft  = snoozesLeft
                        ).show(parentFragmentManager, "snooze_dialog")

                        return@launch
                    }
                }

                // No goal or expense is within the maximum — save directly
                repository.insertExpense(expense)
                Toast.makeText(
                    requireContext(),
                    "Expense saved!",
                    Toast.LENGTH_SHORT
                ).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun loadSubCategoriesFor(categoryId: String) {
        lifecycleScope.launch {
            val subList = subCategoryRepository.getSubCategories(categoryId)

            if (subList.isEmpty()) {
                requireActivity().runOnUiThread {
                    spinnerSubCategory.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("None")
                    )
                    firebaseSubCategoryID = ""
                }
                return@launch
            }

            val names = subList.map { it.subCategoryName }
            requireActivity().runOnUiThread {
                spinnerSubCategory.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
            }

            firebaseSubCategoryID = subList[0].firebaseId

            spinnerSubCategory.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>, v: View?,
                        position: Int, id: Long
                    ) {
                        firebaseSubCategoryID = subList[position].firebaseId
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
        }
    }
}