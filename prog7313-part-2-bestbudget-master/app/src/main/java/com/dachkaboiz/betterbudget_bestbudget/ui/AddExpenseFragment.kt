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
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class AddExpenseFragment : Fragment(R.layout.fragment_add_expense) {

    private lateinit var repository: ExpenseRepository
    private val firebaseCategoryRepository = FirebaseCategoryRepository()

    private var editingExpenseId: String? = null
    private var selectedCategoryFirebaseId: String = ""
    private var categoryList: List<Category> = emptyList()

    private var currentImageUri: Uri? = null
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var vPhotoPlaceholder: View

    private val currentUserEmail: String by lazy {
        requireActivity().getSharedPreferences("auth", 0).getString("email", "") ?: ""
    }

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
                    val imageFile = File(storageDir, currentImageUri!!.lastPathSegment!!)
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

    private fun openCamera() {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        repository = ExpenseRepository(uid)

        editingExpenseId = arguments?.getString("expenseId")

        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerExpenseCategory)
        val etDay = view.findViewById<EditText>(R.id.etExpenseDay)
        val etMonth = view.findViewById<EditText>(R.id.etExpenseMonth)
        val etYear = view.findViewById<EditText>(R.id.etExpenseYear)
        val etDescription = view.findViewById<EditText>(R.id.etExpenseDescription)
        val etAmount = view.findViewById<EditText>(R.id.etExpenseAmount)
        val btnCancel = view.findViewById<Button>(R.id.btnExpenseCancel)
        val btnAdd = view.findViewById<Button>(R.id.btnExpenseAdd)
        ivPhotoPreview = view.findViewById(R.id.ivPhotoPreview)
        vPhotoPlaceholder = view.findViewById(R.id.vPhotoPlaceholder)
        val btnTakePhoto = view.findViewById<Button>(R.id.btnTakePhoto)
        val btnGallery = view.findViewById<Button>(R.id.btnAddFromGallery)

        btnTakePhoto.setOnClickListener {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }

        btnGallery.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        firebaseCategoryRepository.getCategories(uid) { list ->
            categoryList = list
            if (categoryList.isEmpty()) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Please create a category first.", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
                return@getCategories
            }

            val categoryNames = categoryList.map { it.categoryName }
            requireActivity().runOnUiThread {
                spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categoryNames)
            }

            selectedCategoryFirebaseId = categoryList[0].firebaseId

            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                    selectedCategoryFirebaseId = categoryList[position].firebaseId
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
                val day = etDay.text.toString().trim().toIntOrNull()
                val month = etMonth.text.toString().trim().toIntOrNull()
                val year = etYear.text.toString().trim().toIntOrNull()
                val amount = etAmount.text.toString().trim().toDoubleOrNull()
                val description = etDescription.text.toString().trim()

                if (day == null || month == null || year == null || amount == null || amount <= 0) {
                    Toast.makeText(requireContext(), "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val cal = Calendar.getInstance().apply {
                    set(year, month - 1, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val id = editingExpenseId ?: repository.generateExpenseId()

                val expense = Expense(
                    expenseID = id,
                    userEmail = currentUserEmail,
                    categoryId = selectedCategoryFirebaseId,
                    subCategoryId = null,
                    expenseAmount = amount,
                    expenseDate = cal.timeInMillis,
                    expenseDescription = description.ifEmpty { null },
                    imageUri = currentImageUri?.toString(),
                    imageName = null,
                    imageDescription = null,
                    automationFrequency = null,
                    automationMultiplier = null
                )

                lifecycleScope.launch {
                    if (editingExpenseId != null)
                        repository.updateExpense(expense)
                    else
                        repository.insertExpense(expense)

                    Toast.makeText(requireContext(), "Expense saved!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }
}
