package com.dachkaboiz.betterbudget_bestbudget.ui

import android.R.attr.category
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import kotlinx.coroutines.launch

class DeleteCategoryFragment  (
    private val categoryID: Int,

) : Fragment(R.layout.fragment_delete_category){
    private var targetCategory: Category? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())

        val tvIcon = view.findViewById<TextView>(R.id.tvDeleteCategoryIcon)
        val tvName = view.findViewById<TextView>(R.id.tvDeleteCategoryName)
        val tvDescription = view.findViewById<TextView>(R.id.tvDeleteCategoryDescription)
        val tvMinGoal = view.findViewById<TextView>(R.id.tvDeleteCategoryMinGoal)
        val tvMaxGoal = view.findViewById<TextView>(R.id.tvDeleteCategoryMaxGoal)
        val btnConfirm = view.findViewById<Button>(R.id.btnDeleteCategoryConfirm)
        val btnCancel = view.findViewById<Button>(R.id.btnDeleteCategoryCancel)

        // 1. Load Data for the summary card
        lifecycleScope.launch {
            targetCategory = db.categoryDao().getCategoryById(categoryID)
            val goal = db.categoryGoalDao().getGoalsByCategory(categoryID)

            targetCategory?.let { cat ->
                // Note: If you are using real image drawables instead of emojis, you might need an ImageView instead of TextView for the icon.
                tvIcon.text = cat.categoryIcon
                tvName.text = cat.categoryName
                tvDescription.text = "Description: ${cat.categoryDescription ?: "—"}"
            }

            tvMinGoal.text = "Min Goal: ${goal?.minGoal?.toString()?.let { "R $it" } ?: "—"}"
            tvMaxGoal.text = "Max Goal: ${goal?.maxGoal?.toString()?.let { "R $it" } ?: "—"}"
        }

        // 2. Delete Logic
        btnConfirm.setOnClickListener {
            lifecycleScope.launch {

                // 1. Check if category has expenses
                val expenseCount = db.expenseDao().getExpensesByCategory(categoryID)

                if (expenseCount.count() > 0) {
                    Toast.makeText(
                        requireContext(),
                        "Cannot delete: this category has expenses.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // 2. Safe to delete
                targetCategory?.let { cat ->
                    db.categoryDao().deleteCategory(cat)
                }

                Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }


        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}