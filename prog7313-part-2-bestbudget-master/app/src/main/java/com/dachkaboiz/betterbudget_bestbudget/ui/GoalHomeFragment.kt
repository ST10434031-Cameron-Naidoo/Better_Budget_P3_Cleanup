package com.dachkaboiz.betterbudget_bestbudget.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.adapter.GoalAdapter
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class GoalHomeFragment : Fragment(R.layout.fragment_goals) {

    private lateinit var adapter: GoalAdapter
    private lateinit var rvGoals: RecyclerView
    private lateinit var tvNoGoals: TextView
    private lateinit var rgSort: RadioGroup
    private lateinit var tvDateFrom: TextView
    private lateinit var tvDateTo: TextView

    private var dateFrom: Long? = null
    private var dateTo: Long? = null

    private lateinit var goalRepo: FirebaseCategoryGoalRepository
    private lateinit var categoryRepo: FirebaseCategoryRepository
    private lateinit var expenseRepo: ExpenseRepository

    private var categoryCache: List<Category> = emptyList()
    private var goalCache: List<CategoryGoal> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        goalRepo = FirebaseCategoryGoalRepository(uid)
        categoryRepo = FirebaseCategoryRepository()
        expenseRepo = ExpenseRepository(uid)

        rvGoals    = view.findViewById(R.id.rvGoalsList)
        tvNoGoals  = view.findViewById(R.id.tvNoGoals)
        tvDateFrom = view.findViewById(R.id.tvDateFrom)
        tvDateTo   = view.findViewById(R.id.tvDateTo)
        rgSort     = view.findViewById(R.id.rgSortHome)
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddNewGoal)

        adapter = GoalAdapter(
            items         = emptyList(),
            onCardClick   = { categoryId -> navigateToBreakdown(categoryId) },
            onEditClick   = { goalId -> navigateToUpdate(goalId) },
            onDeleteClick = { goalId -> navigateToDelete(goalId) }
        )

        rvGoals.layoutManager = LinearLayoutManager(requireContext())
        rvGoals.adapter = adapter

        tvDateFrom.setOnClickListener {
            showDatePicker { y, m, d ->
                val cal = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dateFrom = cal.timeInMillis
                tvDateFrom.text = "%02d-%02d-%04d ⌵".format(d, m + 1, y)
                loadGoals()
            }
        }

        tvDateTo.setOnClickListener {
            showDatePicker { y, m, d ->
                val cal = Calendar.getInstance().apply {
                    set(y, m, d, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dateTo = cal.timeInMillis
                tvDateTo.text = "%02d-%02d-%04d ⌵".format(d, m + 1, y)
                loadGoals()
            }
        }

        rgSort.setOnCheckedChangeListener { _, _ -> loadGoals() }

        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, AddGoalFragment())
                .addToBackStack(null)
                .commit()
        }

        loadGoals()
    }

    override fun onResume() {
        super.onResume()
        loadGoals()
    }

    private fun loadGoals() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {

            // Load categories
            val categories = categoryRepo.getCategoriesSuspend(uid)
            categoryCache = categories

            // Load goals
            val goals = goalRepo.getAllGoals()
            goalCache = goals

            // Build display list
            val displayList = mutableListOf<Triple<CategoryGoal, Category?, Double>>()

            for (goal in goals) {

                val category = categoryCache.find { it.firebaseId == goal.categoryId }

                // Filter by month/year range
                val goalCal = Calendar.getInstance().apply {
                    set(goal.year, goal.month - 1, 1)
                }

                val fromOk = dateFrom?.let { goalCal.timeInMillis >= it } ?: true
                val toOk   = dateTo?.let { goalCal.timeInMillis <= it } ?: true

                if (!fromOk || !toOk) continue

                // Load expenses for this category
                val expenses = expenseRepo.getExpensesByCategory(goal.categoryId)
                val totalSpent = expenses.sumOf { it.expenseAmount }

                displayList.add(Triple(goal, category, totalSpent))
            }

            // Sorting
            val sorted = when (rgSort.checkedRadioButtonId) {
                R.id.rbSortLastAdded  -> displayList.sortedByDescending { it.first.goalId }
                R.id.rbSortFirstAdded -> displayList.sortedBy { it.first.goalId }
                else -> displayList.sortedBy { it.second?.categoryName ?: "" }
            }

            if (sorted.isEmpty()) {
                tvNoGoals.visibility = View.VISIBLE
                rvGoals.visibility = View.GONE
            } else {
                tvNoGoals.visibility = View.GONE
                rvGoals.visibility = View.VISIBLE
                adapter.updateData(sorted)
            }
        }
    }

    private fun showDatePicker(onDateSelected: (Int, Int, Int) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d -> onDateSelected(y, m, d) },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun navigateToBreakdown(categoryId: String) {
        val fragment = CategoryBreakdownFragment(categoryId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToUpdate(goalId: String) {
        val fragment = UpdateGoalFragment().apply {
            arguments = Bundle().apply { putString("goalId", goalId) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToDelete(goalId: String) {
        val fragment = DeleteGoalFragment().apply {
            arguments = Bundle().apply { putString("goalId", goalId) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }
}
