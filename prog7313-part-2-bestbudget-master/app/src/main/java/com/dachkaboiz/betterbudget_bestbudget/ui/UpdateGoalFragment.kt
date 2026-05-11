package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import kotlinx.coroutines.launch

class UpdateGoalFragment : Fragment(R.layout.fragment_update_goal) {

    private var currentGoal: CategoryGoal? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())

        // Retrieve the ID passed from the previous screen
        val goalId = arguments?.getInt("goalID") ?: -1

//        val tvCategory = view.findViewById<TextView>(R.id.tvTargetCategoryName)
        val etMin = view.findViewById<EditText>(R.id.etUpdateGoalMin)
        val etMax = view.findViewById<EditText>(R.id.etUpdateGoalMax)
        val btnUpdate = view.findViewById<Button>(R.id.btnUpdateGoal)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelUpdateGoal)

        // 1. Fetch current goal data to populate fields
        lifecycleScope.launch {
            currentGoal = db.categoryGoalDao().getGoalById(goalId)
            currentGoal?.let { goal ->
                val category = db.categoryDao().getCategoryById(goal.categoryID)
//                tvCategory.text = "Category: ${category?.categoryName}"
                etMin.setText(goal.minGoal?.toString() ?: "")
                etMax.setText(goal.maxGoal?.toString() ?: "")
            }
        }

        // 2. Update logic
        btnUpdate.setOnClickListener {
            val maxText = etMax.text.toString()
            if (maxText.isBlank()) {
                Toast.makeText(requireContext(), "Max goal is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentGoal?.let { goal ->
                lifecycleScope.launch {
                    val updatedGoal = goal.copy(
                        minGoal = etMin.text.toString().toDoubleOrNull(),
                        maxGoal = maxText.toDoubleOrNull()
                    )

                    db.categoryGoalDao().updateCategoryGoal(updatedGoal)
                    Toast.makeText(requireContext(), "Goal Updated!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}