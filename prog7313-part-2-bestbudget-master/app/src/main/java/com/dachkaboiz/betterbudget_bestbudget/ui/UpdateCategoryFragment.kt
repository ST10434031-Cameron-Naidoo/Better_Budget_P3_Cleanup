package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal
import kotlinx.coroutines.launch
import java.util.Calendar


class UpdateCategoryFragment (private val categoryID: Int):
    Fragment(R.layout.fragment_edit_category) {
    private var currentCategory: Category? = null
    private var currentGoal: CategoryGoal? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())

        val etName = view.findViewById<EditText>(R.id.etEditCategoryName)
        val etIcon = view.findViewById<EditText>(R.id.etEditCategoryIcon)
        val etDescription = view.findViewById<EditText>(R.id.etEditCategoryDescription)
        val etMinGoal = view.findViewById<EditText>(R.id.etEditCategoryMinGoal)
        val etMaxGoal = view.findViewById<EditText>(R.id.etEditCategoryMaxGoal)
        val btnUpdate = view.findViewById<Button>(R.id.btnEditCategoryUpdate)
        val btnCancel = view.findViewById<Button>(R.id.btnEditCategoryCancel)

        // 1. Load existing data
        lifecycleScope.launch {
            currentCategory = db.categoryDao().getCategoryById(categoryID)

            // Get goals (getting the first one for the current subcategory)
            val goals = db.categoryGoalDao().getGoalsByCategory(categoryID)
            currentGoal = goals

            // Populate UI
            currentCategory?.let { cat ->
                etName.setText(cat.categoryName)
                etIcon.setText(cat.categoryIcon)
                etDescription.setText(cat.categoryDescription ?: "")
            }

            currentGoal?.let { goal ->
                etMinGoal.setText(goal.minGoal?.toString() ?: "")
                etMaxGoal.setText(goal.maxGoal?.toString() ?: "")
            }
        }
        // 2. Handle Update
        btnUpdate.setOnClickListener {
            val name = etName.text.toString().trim()
            val icon = etIcon.text.toString().trim().ifBlank { "default_category_icon" }
            val description = etDescription.text.toString().trim()
            val minGoal = etMinGoal.text.toString().toDoubleOrNull()
            val maxGoal = etMaxGoal.text.toString().toDoubleOrNull()

            if (name.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Update Category
                currentCategory?.let { cat->
                    val updatedCategory = cat.copy(
                        categoryName = name,
                        categoryIcon = icon,
                        categoryDescription = description
                    )
                    db.categoryDao().updateCategory(updatedCategory)
                }

                // Handle Goal (Update existing or Insert new if they just added one)
                val cal = Calendar.getInstance()
                if (currentGoal != null) {
                    val updatedGoal = currentGoal!!.copy(
                        minGoal = minGoal,
                        maxGoal = maxGoal
                    )
                    db.categoryGoalDao().updateCategoryGoal(updatedGoal)
                } else if (minGoal != null || maxGoal != null) {
                    val newGoal = CategoryGoal(
                        categoryID = categoryID,
                        minGoal = minGoal,
                        maxGoal = maxGoal,
                        month = cal.get(Calendar.MONTH) + 1,
                        year = cal.get(Calendar.YEAR)
                    )
                    db.categoryGoalDao().insertCategoryGoal(newGoal)
                }

                Toast.makeText(requireContext(), "category updated!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

    }
}