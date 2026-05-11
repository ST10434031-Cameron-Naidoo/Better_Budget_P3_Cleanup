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

            // Get goals (getting the first one for the current subcategory)
            val goals = db.subCategoryGoalDao().getGoalsBySubCategory(subID)
            currentGoal = goals
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
            val icon = etIcon.text.toString().trim().ifBlank { "default_subcategory_icon" }
            val description = etDescription.text.toString().trim()
            val minGoal = etMinGoal.text.toString().toDoubleOrNull()
            val maxGoal = etMaxGoal.text.toString().toDoubleOrNull()

            if (name.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Update SubCategory
                currentSubCategory?.let { sub ->
                    val updatedSub = sub.copy(
                        subCategoryName = name,
                        subCategoryIcon = icon,
                        subCategoryDescription = description
                    )
                    db.subCategoryDao().updateSubCategory(updatedSub)
                }

                // Handle Goal (Update existing or Insert new if they just added one)
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