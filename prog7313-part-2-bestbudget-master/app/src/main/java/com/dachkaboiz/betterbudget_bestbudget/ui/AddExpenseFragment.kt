package com.dachkaboiz.betterbudget_bestbudget.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository
import androidx.activity.result.contract.ActivityResultContracts
import com.dachkaboiz.betterbudget_bestbudget.data.utils.ImageUtils
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import java.io.File
import android.content.Intent



class AddExpenseFragment : Fragment(R.layout.fragment_add_expense) {


    private var editingExpenseId: Int? = null
    private var pendingExpense: Expense? = null

    private lateinit var repository: ExpenseRepository
    private lateinit var categoryGoalRepository: CategoryGoalRepository
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var subCategoryRepository: SubCategoryRepository

    private var selectedSubCategoryId: Int? = null
    private var selectedCategoryId: Int = -1
    private var categoryList: List<Category> = emptyList()

    private var subCategoryList: List<SubCategory> = emptyList()

    private lateinit var spinnerSubCategory: Spinner

    private val currentUserEmail: String by lazy {
        requireActivity()
            .getSharedPreferences("auth", 0)
            .getString("email", "") ?: ""
    }

    // -----------------------------------------------------------------------
    private var currentImageUri: Uri? = null

    private lateinit var ivPhotoPreview: ImageView
    private lateinit var vPhotoPlaceholder: View

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentImageUri != null) {
                updatePhotoUI()
                // SAVE TO GALLERY Logic
                lifecycleScope.launch(Dispatchers.IO) {
                    // Get the actual file from the URI
                    val storageDir = File(requireContext().getExternalFilesDir("Pictures"), "captured_images")
                    // You'll need to track the actual File object or reconstruct it
                    val imageFile = File(storageDir, currentImageUri!!.lastPathSegment!!)

                    if (imageFile.exists()) {
                        ImageUtils.saveImageToGallery(requireContext(), imageFile)
                    }
                }

            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    // PERSIST THE PERMISSION HERE
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)

                    currentImageUri = uri
                    updatePhotoUI()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to persist permission", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun openCamera() {
        // This MUST use your ImageUtils and return a content:// URI from FileProvider
        currentImageUri = ImageUtils.createImageFile(requireContext())
        takePictureLauncher.launch(currentImageUri)
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

    // Loads subcategories for given categoryId and updates spinner
    private suspend fun loadSubCategories(categoryId: Int) {
        subCategoryList = subCategoryRepository.getSubCategoriesByCategory(categoryId)
        val names = mutableListOf("None") + subCategoryList.map { it.subCategoryName }
        spinnerSubCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            names
        )
        spinnerSubCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                selectedSubCategoryId = if (position == 0) null else subCategoryList[position - 1].subCategoryID
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // onViewCreated

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup repositories
        val db = AppDatabase.getDatabase(requireContext())
        repository             = ExpenseRepository(db.expenseDao())
        categoryGoalRepository = CategoryGoalRepository(db.categoryGoalDao())
        categoryRepository     = CategoryRepository(db.categoryDao())
        subCategoryRepository = SubCategoryRepository(db.subCategoryDao())

        editingExpenseId = arguments?.getInt("expenseId", -1)?.takeIf { it != -1 }


        // Bind every view using the confirmed IDs from fragment_add_expense.xml

        val spinnerCategory    = view.findViewById<Spinner>(R.id.spinnerExpenseCategory)
        spinnerSubCategory = view.findViewById(R.id.spinnerExpenseSubCategory)
        val etDay              = view.findViewById<EditText>(R.id.etExpenseDay)
        val etMonth            = view.findViewById<EditText>(R.id.etExpenseMonth)
        val etYear             = view.findViewById<EditText>(R.id.etExpenseYear)
        val etDescription      = view.findViewById<EditText>(R.id.etExpenseDescription)
        val etAmount           = view.findViewById<EditText>(R.id.etExpenseAmount)
        //val rgFrequency        = view.findViewById<RadioGroup>(R.id.rgAutomateFrequency)
        val btnCancel          = view.findViewById<Button>(R.id.btnExpenseCancel)
        val btnAdd             = view.findViewById<Button>(R.id.btnExpenseAdd)

        // 1. Setup UI References
        ivPhotoPreview = view.findViewById(R.id.ivPhotoPreview)
        vPhotoPlaceholder = view.findViewById(R.id.vPhotoPlaceholder)
        val btnTakePhoto = view.findViewById<Button>(R.id.btnTakePhoto)
        val btnGallery = view.findViewById<Button>(R.id.btnAddFromGallery)

        // ... Bind other views (etAmount, etDay, etc.) ...

        //  Attach Photo Listeners
        btnTakePhoto.setOnClickListener {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }

        btnGallery.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        // Fragment Result Listener (If coming from another fragment)
        parentFragmentManager.setFragmentResultListener("camera_request", viewLifecycleOwner) { _, bundle ->
            bundle.getString("image_uri")?.let {
                currentImageUri = Uri.parse(it)
                updatePhotoUI()
            }
        }

        // ... Database Setup & Category Loading ...

        //  Edit Mode Pre-fill
        lifecycleScope.launch {
            if (editingExpenseId != null) {
                val expense = repository.getExpenseById(editingExpenseId!!)
                expense?.let {
                    // ... pre-fill text fields ...
                    it.imageUri?.let { uriStr ->
                        currentImageUri = Uri.parse(uriStr)
                        updatePhotoUI()
                    }
                }
            }
        }


        // Fragment result listeners registered before any coroutine


        // Receive snooze dialog result from SnoozeDialogFragment
        parentFragmentManager.setFragmentResultListener(
            "snooze_result", viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString("action")) {
                "USE_SNOOZE" -> pendingExpense?.let { e ->
                    lifecycleScope.launch { saveExpense(e, "Expense saved! Snooze used.") }
                }
                "SAVE" -> pendingExpense?.let { e ->
                    lifecycleScope.launch { saveExpense(e, "Expense saved (over budget).") }
                }
                "CANCEL" -> {
                    pendingExpense = null
                    Toast.makeText(requireContext(), "Expense not saved.", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // Cancel

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        // Load categories from Room, populate spinner, pre-fill if editing.
        // The Add button is wired INSIDE this coroutine so selectedCategoryId
        // is always valid by the time the user can tap ADD.

        lifecycleScope.launch {

            categoryList = categoryRepository.getCategoriesByUser(currentUserEmail)

            if (categoryList.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please create a category first before adding an expense.",
                    Toast.LENGTH_LONG
                ).show()
                parentFragmentManager.popBackStack()
                return@launch
            }

            // Populate category spinner
            val categoryNames = categoryList.map { it.categoryName }
            spinnerCategory.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                categoryNames
            )

            // Load subcategories for default category
            selectedCategoryId = categoryList[0].categoryID
            loadSubCategories(selectedCategoryId)

            // Reload subcategories when category changes
            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                    selectedCategoryId = categoryList[position].categoryID
                    lifecycleScope.launch { loadSubCategories(selectedCategoryId) }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }


            // Pre-fill fields

            if (editingExpenseId != null) {
                // EDIT MODE — load the existing expense and fill every field
                val expense = repository.getExpenseById(editingExpenseId!!)
                if (expense != null) {
                    etAmount.setText(expense.expenseAmount.toString())
                    etDescription.setText(expense.expenseDescription ?: "")

                    val cal = Calendar.getInstance()
                    cal.timeInMillis = expense.expenseDate
                    etDay.setText(cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0'))
                    etMonth.setText((cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0'))
                    etYear.setText(cal.get(Calendar.YEAR).toString())

                    // Pre-select frequency radio button
                    //when (expense.automationFrequency) {
                        //"Day"   -> rgFrequency.check(R.id.rbDay)
                        //"Week"  -> rgFrequency.check(R.id.rbWeek)
                        //"Month" -> rgFrequency.check(R.id.rbMonth)
                        //"Year"  -> rgFrequency.check(R.id.rbYear)
                    //}

                    // Pre-select the category in the spinner
                    val index = categoryList.indexOfFirst { c ->
                        c.categoryID == expense.categoryID
                    }
                    if (index >= 0) {
                        spinnerCategory.setSelection(index)
                        selectedCategoryId = expense.categoryID
                    }


                }
            } else {
                // ADD MODE — pre-fill today's date
                val cal = Calendar.getInstance()
                etDay.setText(cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0'))
                etMonth.setText((cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0'))
                etYear.setText(cal.get(Calendar.YEAR).toString())
            }


            // Add / Save button wired here so category list is loaded first

            btnAdd.setOnClickListener {

                val dayText     = etDay.text.toString().trim()
                val monthText   = etMonth.text.toString().trim()
                val yearText    = etYear.text.toString().trim()
                val amountText  = etAmount.text.toString().trim()
                val description = etDescription.text.toString().trim()

                //val frequencyLabel = when (rgFrequency.checkedRadioButtonId) {
                //    R.id.rbDay   -> "Day"
                //   R.id.rbWeek  -> "Week"
                // R.id.rbMonth -> "Month"
                //   R.id.rbYear  -> "Year"
                //    else         -> null
                //}

                // ---- Validation ----
                var hasError = false

                if (selectedCategoryId == -1) {
                    Toast.makeText(
                        requireContext(), "Please select a category", Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }



                val amountDouble = amountText.toDoubleOrNull()
                if (amountText.isEmpty() || amountDouble == null || amountDouble <= 0) {
                    etAmount.error = "Enter a valid amount greater than 0"
                    hasError = true
                }

                val day = dayText.toIntOrNull()
                if (day == null || day < 1 || day > 31) {
                    etDay.error = "1–31"
                    hasError = true
                }

                val month = monthText.toIntOrNull()
                if (month == null || month < 1 || month > 12) {
                    etMonth.error = "1–12"
                    hasError = true
                }

                val year = yearText.toIntOrNull()
                if (year == null || year < 2000 || year > 2100) {
                    etYear.error = "e.g. 2025"
                    hasError = true
                }

                if (hasError) return@setOnClickListener

                // Convert day/month/year to Long timestamp
                val cal = Calendar.getInstance()
                cal.set(year!!, month!! - 1, day!!, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val expense = Expense(
                    expenseID           = editingExpenseId ?: 0,
                    userEmail           = currentUserEmail,
                    categoryID          = selectedCategoryId,
                    subCategoryID       = selectedSubCategoryId,
                    expenseAmount       = amountText.toDouble(),
                    expenseDate         = cal.timeInMillis,
                    expenseDescription  = description.ifEmpty { null },
                    imageUri            = currentImageUri?.toString(),
                    imageName           = null,
                    imageDescription    = null,

                    //automationFrequency = frequencyLabel
                )

                // Snooze pre-commit check
                lifecycleScope.launch {
                    val startOfMonth = Calendar.getInstance().apply {
                        set(year, month - 1, 1, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val endOfMonth = Calendar.getInstance().apply {
                        set(year, month - 1, 1, 23, 59, 59)
                        set(Calendar.MILLISECOND, 999)
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }.timeInMillis

                    val currentTotal = repository.getTotalSpentByCategory(
                        selectedCategoryId, startOfMonth, endOfMonth
                    ) ?: 0.0

                    val goal    = categoryGoalRepository.getGoalByCategoryAndMonth(
                        selectedCategoryId, month, year
                    )
                    val maxGoal = goal?.maxGoal

                    if (maxGoal != null && (currentTotal + expense.expenseAmount) > maxGoal) {
                        // Over budget — show snooze dialog
                        pendingExpense = expense
                        val catName = categoryRepository
                            .getCategoryById(selectedCategoryId)
                            ?.categoryName ?: "Category $selectedCategoryId"

                        SnoozeDialogFragment.newInstance(
                            categoryName = catName,
                            currentTotal = currentTotal,
                            maxGoal      = maxGoal,
                            newAmount    = expense.expenseAmount,
                            snoozesLeft  = 2
                        ).show(parentFragmentManager, "snooze_dialog")
                    } else {
                        // Under budget — save straight away
                        saveExpense(expense, null)
                    }
                }
            }
        }
    }


    // Save helper handles both insert and update

    private suspend fun saveExpense(expense: Expense, customMessage: String?) {
        if (editingExpenseId != null) {
            repository.updateExpense(expense)
        } else {
            repository.insertExpense(expense)
        }
        Toast.makeText(
            requireContext(),
            customMessage
                ?: if (editingExpenseId != null) "Expense updated!" else "Expense added!",
            Toast.LENGTH_SHORT
        ).show()
        pendingExpense = null
        parentFragmentManager.popBackStack()
    }
}