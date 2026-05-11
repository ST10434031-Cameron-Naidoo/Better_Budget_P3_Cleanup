package com.dachkaboiz.betterbudget_bestbudget.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class EditExpenseFragment : Fragment(R.layout.fragment_edit_expense_v2) {

    private lateinit var repository: ExpenseRepository
    private lateinit var categoryRepository: CategoryRepository

    private var currentImageUri: Uri? = null

    private lateinit var ivPhotoPreview: ImageView
    private lateinit var vPhotoPlaceholder: View

    private var editingExpenseId: Int = -1
    private var selectedCategoryId: Int = -1
    private var categoryList: List<Category> = emptyList()


    private lateinit var ivPreview: ImageView
    private lateinit var vPlaceholder: View




    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentImageUri != null) {
                updatePhotoUI()
                // Save to Gallery for backup
                lifecycleScope.launch(Dispatchers.IO) {
                    val storageDir = File(requireContext().getExternalFilesDir("Pictures"), "captured_images")
                    val imageFile = File(storageDir, currentImageUri!!.lastPathSegment ?: "temp_img")
                    if (imageFile.exists()) {
                        ImageUtils.saveImageToGallery(requireContext(), imageFile)
                    }
                }
            }
        }



    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                currentImageUri = uri
                updatePhotoUI()
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

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            // Note: For a production app, save bitmap to file and get URI
            // For now, we update the preview directly
            ivPreview.setImageBitmap(bitmap)
            ivPreview.visibility = View.VISIBLE
            vPlaceholder.visibility = View.GONE
        }


    }
    private val currentUserEmail: String by lazy {
        requireActivity()
            .getSharedPreferences("auth", 0)
            .getString("email", "") ?: ""
    }

    // -----------------------------------------------------------------------
    // onViewCreated
    // -----------------------------------------------------------------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup repositories
        val db = AppDatabase.getDatabase(requireContext())
        repository         = ExpenseRepository(db.expenseDao())
        categoryRepository = CategoryRepository(db.categoryDao())

        // Get the expense ID passed from ExpensesFragment
        editingExpenseId = arguments?.getInt("expenseId", -1) ?: -1

        if (editingExpenseId == -1) {
            Toast.makeText(requireContext(), "No expense to edit.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Initialize Photo UI Views
        ivPhotoPreview = view.findViewById(R.id.ivEditPhotoPreview)
        vPhotoPlaceholder = view.findViewById(R.id.vEditPhotoPlaceholder)
        val btnCamera = view.findViewById<Button>(R.id.btnEditTakePhoto)
        val btnGallery = view.findViewById<Button>(R.id.btnEditAddFromGallery)

        // Bind views using IDs from fragment_edit_expense_v2.xml
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerEditExpenseCategory)
        val etDay           = view.findViewById<EditText>(R.id.etEditExpenseDay)
        val etMonth         = view.findViewById<EditText>(R.id.etEditExpenseMonth)
        val etYear          = view.findViewById<EditText>(R.id.etEditExpenseYear)
        val etDescription   = view.findViewById<EditText>(R.id.etEditExpenseDescription)
        val etAmount        = view.findViewById<EditText>(R.id.etEditExpenseAmount)
        val btnCancel       = view.findViewById<Button>(R.id.btnEditExpenseCancel)
        val btnUpdate       = view.findViewById<Button>(R.id.btnEditExpenseUpdate)

        // -----------------------------------------------------------------------
        // Load categories then pre-fill the form with the existing expense
        // -----------------------------------------------------------------------
        // Load Categories and Prefill Form
        lifecycleScope.launch {
            categoryList = categoryRepository.getCategoriesByUser(currentUserEmail)
            if (categoryList.isEmpty()) {
                Toast.makeText(requireContext(), "No categories found.", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
                return@launch
            }

            // Setup Spinner
            val names = categoryList.map { it.categoryName }
            spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                    selectedCategoryId = categoryList[pos].categoryID
                }
                override fun onNothingSelected(p: AdapterView<*>) {}
            }

            // Load Existing Expense Data
            val expense = repository.getExpenseById(editingExpenseId)
            expense?.let {
                // Set Category
                val index = categoryList.indexOfFirst { c -> c.categoryID == it.categoryID }
                if (index >= 0) spinnerCategory.setSelection(index)

                // Set Text Fields
                etAmount.setText(it.expenseAmount.toString())
                etDescription.setText(it.expenseDescription ?: "")

                // Set Date Fields
                val cal = Calendar.getInstance().apply { timeInMillis = it.expenseDate }
                etDay.setText(cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0'))
                etMonth.setText((cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0'))
                etYear.setText(cal.get(Calendar.YEAR).toString())

                // Set Image Preview
                it.imageUri?.let { uriStr ->
                    currentImageUri = Uri.parse(uriStr)
                    updatePhotoUI()
                }
            }
        }

        // -----------------------------------------------------------------------
        // Button listeners
        // -----------------------------------------------------------------------


        btnCamera.setOnClickListener {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }

        btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnUpdate.setOnClickListener {
            // Validation Logic 
            val day = etDay.text.toString().toIntOrNull() ?: 1
            val month = etMonth.text.toString().toIntOrNull() ?: 1
            val year = etYear.text.toString().toIntOrNull() ?: 2024



            val cal = Calendar.getInstance().apply {
                set(year, month - 1, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }



            val updatedExpense = Expense(
                expenseID = editingExpenseId,
                userEmail = currentUserEmail,
                categoryID = selectedCategoryId,
                subCategoryID = null,
                expenseAmount = etAmount.text.toString().toDoubleOrNull() ?: 0.0,
                expenseDate = cal.timeInMillis,
                expenseDescription = etDescription.text.toString().ifEmpty { null },
                imageUri = currentImageUri?.toString(),
                imageName           = null,
                imageDescription    = null,
                automationFrequency = null // Handled by "Coming Soon" logic
            )

            lifecycleScope.launch {
                repository.updateExpense(updatedExpense)
                Toast.makeText(requireContext(), "Expense Updated", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }
    private fun updateImagePreview(uri: Uri) {
        ivPreview.setImageURI(uri)
        ivPreview.visibility = View.VISIBLE
        vPlaceholder.visibility = View.GONE
    }
}