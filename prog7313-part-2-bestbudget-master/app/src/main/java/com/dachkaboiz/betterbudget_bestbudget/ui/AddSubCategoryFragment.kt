package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Database
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal
import kotlinx.coroutines.launch
import java.util.Calendar

class AddSubCategoryFragment (private val parentID: Int) : Fragment(R.layout.fragment_add_subcategory) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val db = AppDatabase.getDatabase(requireContext())
        // UI References
        val tvCatName: TextView = view.findViewById(R.id.tvParentCategory)
        val etName = view.findViewById<EditText>(R.id.etSubcategoryName)
        val etIcon = view.findViewById<EditText>(R.id.etSubCategoryIcon)
        val etDescription = view.findViewById<EditText>(R.id.etSubDescription)
        val etMinGoal = view.findViewById<EditText>(R.id.etSubMinGoal)
        val etMaxGoal = view.findViewById<EditText>(R.id.etSubMaxGoal)
        val btnAdd = view.findViewById<Button>(R.id.btnSubAdd)
        val btnCancel = view.findViewById<Button>(R.id.btnSubCancel)


        // Add Parent Category Name
        lifecycleScope.launch {
            val parentCat = db.categoryDao().getCategoryById(parentID)
            parentCat?.let { cat ->
                tvCatName.setText(cat.categoryName)

            }

        }



        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val icon = etIcon.text.toString().trim().ifBlank { "default_subcategory_icon" }
            val description = etDescription.text.toString().trim()

            // Goals
            val minGoal = etMinGoal.text.toString().toDoubleOrNull()
            val maxGoal = etMaxGoal.text.toString().toDoubleOrNull()

            if (name.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {

                    // 0. Validate goals BEFORE inserting anything
                    val catGoal: CategoryGoal? = db.categoryGoalDao().getGoalsByCategory(parentID)
                    val subCatGoals: List<SubCategoryGoal> =
                        db.subCategoryGoalDao().getGoalsByCategory(parentID)

                    val totalMinSubGoal: Double = subCatGoals.sumOf { it.minGoal ?: 0.0 }
                    val totalMaxSubGoal: Double = subCatGoals.sumOf { it.maxGoal ?: 0.0 }

                    val catMinGoal: Double = catGoal?.minGoal ?: 0.0
                    val catMaxGoal: Double = catGoal?.maxGoal ?: 0.0

                    val safeMinGoal = minGoal ?: 0.0
                    val safeMaxGoal = maxGoal ?: 0.0

                    val goalsValid =
                        (catMinGoal >= totalMinSubGoal + safeMinGoal) &&
                                (catMaxGoal >= totalMaxSubGoal + safeMaxGoal)

                    if (!goalsValid) {
                        Toast.makeText(
                            requireContext(),
                            "Min or max goal total is greater than category min or max goal",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    // 1. Insert SubCategory ONLY after validation passes
                    val newSubCategory = SubCategory(
                        parentCategoryID = parentID,
                        subCategoryName = name,
                        subCategoryIcon = icon,
                        subCategoryDescription = description
                    )

                    val subCategoryId =
                        db.subCategoryDao().insertSubCategory(newSubCategory).toInt()

                    // 2. Insert SubCategoryGoal if provided
                    if (minGoal != null || maxGoal != null) {
                        val calendar = Calendar.getInstance()
                        val goal = SubCategoryGoal(
                            subCategoryID = subCategoryId,
                            categoryID = parentID,
                            minGoal = minGoal,
                            maxGoal = maxGoal,
                            month = calendar.get(Calendar.MONTH) + 1,
                            year = calendar.get(Calendar.YEAR)
                        )
                        db.subCategoryGoalDao().insertSubCategoryGoal(goal)
                    }

                    Toast.makeText(
                        requireContext(),
                        "Subcategory added successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()

                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }

            btnCancel.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }


    }
}