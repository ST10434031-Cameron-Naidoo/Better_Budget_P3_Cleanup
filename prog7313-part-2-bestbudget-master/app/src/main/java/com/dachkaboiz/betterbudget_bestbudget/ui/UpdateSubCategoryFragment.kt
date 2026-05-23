package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal
import kotlinx.coroutines.launch
import java.util.Calendar

class UpdateSubCategoryFragment (
    private val parentID: Int,
    private val subID: Int
) : Fragment(R.layout.fragment_edit_subcategory) {
    private var currentSubCategory: SubCategory? = null
    private var currentGoal: SubCategoryGoal? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val primaryName: TextView = view.findViewById(R.id.tvEditParentCategory)

        val etName = view.findViewById<EditText>(R.id.etEditSubcategoryName)
        val etIcon = view.findViewById<EditText>(R.id.etEditSubCategoryIcon)
        val etDescription = view.findViewById<EditText>(R.id.etEditSubDescription)
        val etMinGoal = view.findViewById<EditText>(R.id.etEditSubMinGoal)
        val etMaxGoal = view.findViewById<EditText>(R.id.etEditSubMaxGoal)
        val btnUpdate = view.findViewById<Button>(R.id.btnEditSubUpdate)
        val btnCancel = view.findViewById<Button>(R.id.btnEditSubCancel)

        // 1. Load existing data
        lifecycleScope.launch {
            currentSubCategory = db.subCategoryDao().getSubCategoryById(subID)
            val currentCategory = db.categoryDao().getCategoryById(parentID)

            // FIX 4: Load goal for current month only, not LIMIT 1 across all months
            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            currentGoal = db.subCategoryGoalDao().getGoalBySubCategoryAndMonth(subID, month, year)

            currentCategory?.let { cat ->
                primaryName.setText(cat.categoryName)
            }

            // Populate UI
            currentSubCategory?.let { sub ->
                etName.setText(sub.subCategoryName)
                etIcon.setText(sub.subCategoryIcon)
                etDescription.setText(sub.subCategoryDescription ?: "")
            }

            currentGoal?.let { goal ->
                etMinGoal.setText(goal.minGoal?.toString() ?: "")
                etMaxGoal.setText(goal.maxGoal?.toString() ?: "")
            }
        }

        // 2. Handle Update
        btnUpdate.setOnClickListener {
            val name = etName.text.toString().trim()
            val icon = etIcon.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val minGoal = etMinGoal.text.toString().toDoubleOrNull()
            val maxGoal = etMaxGoal.text.toString().toDoubleOrNull()

            if (name.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }

            if (icon.isBlank()) {
                etIcon.error = "Icon is required"
                return@setOnClickListener
            }

            lifecycleScope.launch {

                // 0. Load parent + existing sub goals BEFORE updating anything
                // FIX 2: Use month-filtered queries instead of unfiltered ones
                val calendar = Calendar.getInstance()
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)

                val catGoal = db.categoryGoalDao().getGoalByCategoryAndMonth(parentID, month, year)
                val subCatGoals = db.subCategoryGoalDao().getGoalsByCategoryAndMonth(parentID, month, year)

                // Remove current goal from totals (because we are updating it)
                val filteredSubGoals = subCatGoals.filter { it.subCategoryID != subID }

                val totalMinSubGoal = filteredSubGoals.sumOf { it.minGoal ?: 0.0 }
                val totalMaxSubGoal = filteredSubGoals.sumOf { it.maxGoal ?: 0.0 }

                val catMinGoal = catGoal?.minGoal ?: 0.0
                val catMaxGoal = catGoal?.maxGoal ?: 0.0

                val safeMinGoal = minGoal ?: 0.0
                val safeMaxGoal = maxGoal ?: 0.0

                // Original logic preserved — blocks subcategory goals if no parent goal exists
                val goalsValid =
                    (catMinGoal >= totalMinSubGoal + safeMinGoal) &&
                            (catMaxGoal >= totalMaxSubGoal + safeMaxGoal)

                if (!goalsValid) {
                    Toast.makeText(
                        requireContext(),
                        "Min or max goal total exceeds category goal",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // 1. Update SubCategory
                currentSubCategory?.let { sub ->
                    val updatedSub = sub.copy(
                        subCategoryName = name,
                        subCategoryIcon = icon,
                        subCategoryDescription = description
                    )
                    db.subCategoryDao().updateSubCategory(updatedSub)
                }

                // 2. Update or Insert SubCategoryGoal
                val cal = Calendar.getInstance()

                if (currentGoal != null) {
                    val updatedGoal = currentGoal!!.copy(
                        minGoal = minGoal,
                        maxGoal = maxGoal
                    )
                    db.subCategoryGoalDao().updateSubCategoryGoal(updatedGoal)

                } else if (minGoal != null || maxGoal != null) {
                    val newGoal = SubCategoryGoal(
                        subCategoryID = subID,
                        categoryID = parentID,
                        minGoal = minGoal,
                        maxGoal = maxGoal,
                        month = cal.get(Calendar.MONTH) + 1,
                        year = cal.get(Calendar.YEAR)
                    )
                    db.subCategoryGoalDao().insertSubCategoryGoal(newGoal)
                }

                Toast.makeText(requireContext(), "Subcategory updated!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}