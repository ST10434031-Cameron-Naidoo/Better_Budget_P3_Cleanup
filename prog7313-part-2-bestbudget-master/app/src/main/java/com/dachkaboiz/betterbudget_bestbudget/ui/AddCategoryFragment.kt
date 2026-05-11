package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import android.view.ViewGroup
import android.widget.FrameLayout

class AddCategoryFragment : Fragment(R.layout.fragment_add_category) {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var categoryGoalRepository: CategoryGoalRepository

    private val currentUserEmail: String by lazy {
        requireActivity()
            .getSharedPreferences("auth", 0)
            .getString("email", "") ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup repositories
        val db = AppDatabase.getDatabase(requireContext())


        categoryRepository     = CategoryRepository(db.categoryDao())
        categoryGoalRepository = CategoryGoalRepository(db.categoryGoalDao())

        // Bind views — IDs from fragment_add_category.xml
        val etName        = view.findViewById<EditText>(R.id.etCategoryName)
        val etIcon        = view.findViewById<EditText>(R.id.etCategoryIcon)
        val etDescription = view.findViewById<EditText>(R.id.etCategoryDescription)
        val etMinGoal     = view.findViewById<EditText>(R.id.etCategoryMinGoal)
        val etMaxGoal     = view.findViewById<EditText>(R.id.etCategoryMaxGoal)
        val btnCancel     = view.findViewById<Button>(R.id.btnCategoryCancel)
        val btnAdd        = view.findViewById<Button>(R.id.btnCategoryAdd)


        // Cancel — go back
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Add — validate then save to Room
        btnAdd.setOnClickListener {

            val name        = etName.text.toString().trim()
            val icon        = etIcon.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val minGoalText = etMinGoal.text.toString().trim()
            val maxGoalText = etMaxGoal.text.toString().trim()

            // ---- Validation ----
            var hasError = false

            if (name.isEmpty()) {
                etName.error = "Category name is required"
                hasError = true
            }

            if (icon.isEmpty()) {
                etIcon.error = "Icon is required"
                hasError = true
            } else if (!isEmojiOnly(icon)) {
                etIcon.error = "Please enter only emojis"
                hasError = true
            }

            if (description.isEmpty()) {
                etDescription.error = "Description is required"
                hasError = true
            }

            val minGoal = minGoalText.toDoubleOrNull()
            if (minGoalText.isNotEmpty() && minGoal == null) {
                etMinGoal.error = "Enter a valid number"
                hasError = true
            }

            val maxGoal = maxGoalText.toDoubleOrNull()
            if (maxGoalText.isNotEmpty() && maxGoal == null) {
                etMaxGoal.error = "Enter a valid number"
                hasError = true
            }

            if (minGoal != null && maxGoal != null && minGoal > maxGoal) {
                etMinGoal.error = "Min goal cannot exceed max goal"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            // ---- Save to Room ----
            lifecycleScope.launch {
                val newCategoryId = db.categoryDao().insertCategory(
                    Category(
                        userEmail           = currentUserEmail,
                        categoryName        = name,
                        categoryIcon        = icon,
                        categoryDescription = description                    )
                ).toInt()

                // If the user provided goals, save a CategoryGoal for this month
                if (minGoal != null || maxGoal != null) {
                    val cal = Calendar.getInstance()
                    categoryGoalRepository.insertCategoryGoal(
                        CategoryGoal(
                            categoryID = newCategoryId,
                            minGoal    = minGoal ?: 0.0,
                            maxGoal    = maxGoal ?: 0.0,
                            month      = cal.get(Calendar.MONTH) + 1,
                            year       = cal.get(Calendar.YEAR)
                        )
                    )
                }

                Toast.makeText(requireContext(), "Category added!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }
    private fun isEmojiOnly(text: String): Boolean {
        if (text.isEmpty()) return false
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val type = Character.getType(codePoint)

            // Check against standard emoji Unicode categories
            if (type != Character.SURROGATE.toInt() &&
                type != Character.OTHER_SYMBOL.toInt() &&
                type != Character.NON_SPACING_MARK.toInt() &&
                type != Character.COMBINING_SPACING_MARK.toInt()) {
                return false
            }
            i += Character.charCount(codePoint)
        }
        return true
    }
}