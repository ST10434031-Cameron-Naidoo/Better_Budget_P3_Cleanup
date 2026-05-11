package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import kotlinx.coroutines.launch

class DeleteGoalFragment : Fragment(R.layout.fragment_delete_goal) {

    private var targetGoal: CategoryGoal? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val goalId = arguments?.getInt("goalID") ?: -1

        // UI References
        val tvCategory = view.findViewById<TextView>(R.id.tvDeleteGoalCategory)
        val tvMax = view.findViewById<TextView>(R.id.tvDeleteGoalMax)
        val tvMin = view.findViewById<TextView>(R.id.tvDeleteGoalMin)
        val tvPeriod = view.findViewById<TextView>(R.id.tvDeleteGoalPeriod)
        val btnConfirm = view.findViewById<Button>(R.id.btnDeleteGoalConfirm)
        val btnCancel = view.findViewById<Button>(R.id.btnDeleteGoalCancel)

        // 1. Load Goal details for the summary card
        lifecycleScope.launch {
            targetGoal = db.categoryGoalDao().getGoalById(goalId)
            targetGoal?.let { goal ->
                val category = db.categoryDao().getCategoryById(goal.categoryID)

                tvCategory.text = category?.categoryName ?: "Unknown Category"
                tvMax.text = "Limit: R${String.format("%.2f", goal.maxGoal ?: 0.0)}"
                tvMin.text = "Min Target: R${String.format("%.2f", goal.minGoal ?: 0.0)}"
                tvPeriod.text = "Period: ${goal.month} - ${goal.year}"
            }
        }

        // 2. Delete Confirmation
        btnConfirm.setOnClickListener {
            targetGoal?.let { goal ->
                lifecycleScope.launch {
                    db.categoryGoalDao().deleteCategoryGoal(goal)
                    Toast.makeText(requireContext(), "Goal deleted successfully", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }

        // 3. Cancel
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}