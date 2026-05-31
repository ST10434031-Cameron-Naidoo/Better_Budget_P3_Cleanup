package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class AddExpenseFragment : Fragment(R.layout.fragment_add_expense) {

    private var editingExpenseId: Int? = null
    private var pendingExpense: Expense? = null
    private lateinit var repository: ExpenseRepository
    private lateinit var categoryGoalRepository: CategoryGoalRepository
    private lateinit var subCategoryRepository: SubCategoryRepository
    private val firebaseCategoryRepository = FirebaseCategoryRepository()

    private var selectedSubCategoryId: Int? = null
    private var selectedCategoryFirebaseId: String = "" // replaces selectedCategoryId: Int
    private var categoryList: List<Category> = emptyList()
    private var subCategoryList: List<SubCategory> = emptyList()
    private lateinit var spinnerSubCategory: Spinner

    private val currentUserEmail: String by lazy {
        requireActivity().getSharedPreferences("auth", 0).getString("email", "") ?: ""
    }

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
                lifecycleScope.launch(Dispatchers.IO) {
                    val storageDir = File(requireContext().getExternalFilesDir("Pictures"), "captured_images")
                    val imageFile  = File(storageDir, currentImageUri!!.lastPathSegment!!)
                    if (imageFile.exists()) ImageUtils.saveImageToGallery(requireContext(), imageFile)
                }
            }
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
                    Toast.makeText(requireContext(), "Failed to persist permission", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // ── Automation ────────────────────────────────────────────────────────────
    private val automatedRepo = FirebaseAutomatedExpenseRepository()
    private var isAutomated = false
    private var selectedFrequencyUnit = "MONTH"


    private fun openCamera() {
        currentImageUri = ImageUtils.createImageFile(requireContext())
        takePictureLauncher.launch(currentImageUri)
    }

    private fun updatePhotoUI() {
        if (currentImageUri != null) {
            ivPhotoPreview.setImageURI(currentImageUri)
            ivPhotoPreview.visibility    = View.VISIBLE
            vPhotoPlaceholder.visibility = View.GONE
        } else {
            ivPhotoPreview.visibility    = View.GONE
            vPhotoPlaceholder.visibility = View.VISIBLE
        }
    }

    private suspend fun loadSubCategories(categoryId: Int) {
//        subCategoryList = subCategoryRepository.getSubCategoriesByCategory(categoryId)
        val names = mutableListOf("None") + subCategoryList.map { it.subCategoryName }
        spinnerSubCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
        spinnerSubCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
//                selectedSubCategoryId = if (position == 0) null else subCategoryList[position - 1].subCategoryID
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db  = AppDatabase.getDatabase(requireContext())
        repository             = ExpenseRepository(db.expenseDao())
        categoryGoalRepository = CategoryGoalRepository(db.categoryGoalDao())
//        subCategoryRepository  = SubCategoryRepository(db.subCategoryDao())
        editingExpenseId       = arguments?.getInt("expenseId", -1)?.takeIf { it != -1 }

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
        btnGallery.setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }

        parentFragmentManager.setFragmentResultListener("camera_request", viewLifecycleOwner) { _, bundle ->
            bundle.getString("image_uri")?.let { currentImageUri = Uri.parse(it); updatePhotoUI() }
        }

        parentFragmentManager.setFragmentResultListener("snooze_result", viewLifecycleOwner) { _, bundle ->
            when (bundle.getString("action")) {
                "USE_SNOOZE" -> pendingExpense?.let { e -> lifecycleScope.launch { saveExpense(e, "Expense saved! Snooze used.") } }
                "SAVE"       -> pendingExpense?.let { e -> lifecycleScope.launch { saveExpense(e, "Expense saved (over budget).") } }
                "CANCEL"     -> { pendingExpense = null; Toast.makeText(requireContext(), "Expense not saved.", Toast.LENGTH_SHORT).show() }
            }
        }

        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }

        if (uid != null) {
            firebaseCategoryRepository.getCategories(uid) { list ->
                categoryList = list
                if (categoryList.isEmpty()) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Please create a category first before adding an expense.", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                    return@getCategories
                }

                val categoryNames = categoryList.map { it.categoryName }
                requireActivity().runOnUiThread {
                    spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categoryNames)
                }

                selectedCategoryFirebaseId = categoryList[0].firebaseId
                // TODO: subcategory owner still uses Int parentCategoryID — pass 0 until they migrate
                lifecycleScope.launch { loadSubCategories(0) }

                spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                        selectedCategoryFirebaseId = categoryList[position].firebaseId
                        // TODO: subcategory owner still uses Int parentCategoryID — pass 0 until they migrate
                        lifecycleScope.launch { loadSubCategories(0) }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

                lifecycleScope.launch {
                    if (editingExpenseId != null) {
                        val expense = repository.getExpenseById(editingExpenseId!!)
                        if (expense != null) {
                            etAmount.setText(expense.expenseAmount.toString())
                            etDescription.setText(expense.expenseDescription ?: "")
                            val cal = Calendar.getInstance().apply { timeInMillis = expense.expenseDate }
                            etDay.setText(cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0'))
                            etMonth.setText((cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0'))
                            etYear.setText(cal.get(Calendar.YEAR).toString())
                            expense.imageUri?.let { currentImageUri = Uri.parse(it); updatePhotoUI() }
                        }
                    } else {
                        val cal = Calendar.getInstance()
                        etDay.setText(cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0'))
                        etMonth.setText((cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0'))
                        etYear.setText(cal.get(Calendar.YEAR).toString())
                    }
                }

                btnAdd.setOnClickListener {
                    val dayText     = etDay.text.toString().trim()
                    val monthText   = etMonth.text.toString().trim()
                    val yearText    = etYear.text.toString().trim()
                    val amountText  = etAmount.text.toString().trim()
                    val description = etDescription.text.toString().trim()
                    var hasError    = false

                    if (selectedCategoryFirebaseId.isEmpty()) {
                        Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val amountDouble = amountText.toDoubleOrNull()
                    if (amountText.isEmpty() || amountDouble == null || amountDouble <= 0) { etAmount.error = "Enter a valid amount greater than 0"; hasError = true }
                    val day = dayText.toIntOrNull()
                    if (day == null || day < 1 || day > 31) { etDay.error = "1–31"; hasError = true }
                    val month = monthText.toIntOrNull()
                    if (month == null || month < 1 || month > 12) { etMonth.error = "1–12"; hasError = true }
                    val year = yearText.toIntOrNull()
                    if (year == null || year < 2000 || year > 2100) { etYear.error = "e.g. 2025"; hasError = true }
                    if (hasError) return@setOnClickListener

                    val cal = Calendar.getInstance().apply { set(year!!, month!! - 1, day!!, 0, 0, 0); set(Calendar.MILLISECOND, 0) }

                    val expense = Expense(
                        expenseID          = editingExpenseId ?: 0,
                        userEmail          = currentUserEmail,
                        categoryID         = 0, // TODO: expense owner migrates categoryID to String firebaseId
                        subCategoryID      = selectedSubCategoryId,
                        expenseAmount      = amountText.toDouble(),
                        expenseDate        = cal.timeInMillis,
                        expenseDescription = description.ifEmpty { null },
                        imageUri           = currentImageUri?.toString(),
                        imageName          = null,
                        imageDescription   = null
                    )

                    lifecycleScope.launch {
                        val startOfMonth = Calendar.getInstance().apply { set(year!!, month!! - 1, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                        val endOfMonth   = Calendar.getInstance().apply { set(year!!, month!! - 1, 1, 23, 59, 59); set(Calendar.MILLISECOND, 999); set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }.timeInMillis
                        val currentTotal = repository.getTotalSpentByCategory(0, startOfMonth, endOfMonth) ?: 0.0
                        val goal         = categoryGoalRepository.getGoalByCategoryAndMonth(0, month!!, year!!)
                        val maxGoal      = goal?.maxGoal

                        if (maxGoal != null && (currentTotal + expense.expenseAmount) > maxGoal) {
                            pendingExpense = expense
                            val catName = categoryList.find { it.firebaseId == selectedCategoryFirebaseId }?.categoryName ?: "Category"
                            SnoozeDialogFragment.newInstance(
                                categoryName = catName,
                                currentTotal = currentTotal,
                                maxGoal      = maxGoal,
                                newAmount    = expense.expenseAmount,
                                snoozesLeft  = 2
                            ).show(parentFragmentManager, "snooze_dialog")
                        } else {
                            saveExpense(expense, null)
                        }
                    }
                }
            }
        }
    }

    private suspend fun saveExpense(expense: Expense, customMessage: String?) {
        if (editingExpenseId != null) repository.updateExpense(expense)
        else repository.insertExpense(expense)
        Toast.makeText(requireContext(), customMessage ?: if (editingExpenseId != null) "Expense updated!" else "Expense added!", Toast.LENGTH_SHORT).show()
        pendingExpense = null
        parentFragmentManager.popBackStack()
    }
}