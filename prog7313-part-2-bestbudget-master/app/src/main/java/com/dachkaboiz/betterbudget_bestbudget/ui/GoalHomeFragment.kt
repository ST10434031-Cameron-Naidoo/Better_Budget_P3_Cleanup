package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.adapter.GoalAdapter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Calendar
import android.app.DatePickerDialog
import android.widget.RadioGroup


class GoalHomeFragment : Fragment(R.layout.fragment_goals) {

    private lateinit var adapter: GoalAdapter
    private lateinit var rvGoals: RecyclerView
    private lateinit var tvNoGoals: TextView
    private lateinit var rgSort: RadioGroup

    // Date filter variables
    private lateinit var tvDateFrom: TextView
    private lateinit var tvDateTo: TextView
    private var dateFrom: Long? = null
    private var dateTo: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvGoals = view.findViewById(R.id.rvGoalsList)
        tvNoGoals = view.findViewById(R.id.tvNoGoals)
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddNewGoal)
        tvDateFrom = view.findViewById(R.id.tvDateFrom)
        tvDateTo = view.findViewById(R.id.tvDateTo)
        rgSort = view.findViewById(R.id.rgSortHome)



        // Initialize Adapter with Triple data and navigation logic
        adapter = GoalAdapter(
            items = emptyList(),
            onCardClick = { categoryId -> navigateToBreakdown(categoryId) },
            onEditClick = { goalId -> navigateToUpdate(goalId) },
            onDeleteClick = { goalId -> navigateToDelete(goalId) }
        )

        rvGoals.layoutManager = LinearLayoutManager(requireContext())
        rvGoals.adapter = adapter

        // Setup Date Pickers
        tvDateFrom.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dateFrom = cal.timeInMillis
                tvDateFrom.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                loadGoals()
            }
        }

        tvDateTo.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dateTo = cal.timeInMillis
                tvDateTo.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                loadGoals()
            }
        }

        // Trigger reload when sorting changes
        rgSort.setOnCheckedChangeListener { _, _ -> loadGoals() }

        //  Load Data
        loadGoals()

        //  Add Goal Navigation
        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, AddGoalFragment())
                .addToBackStack(null)
                .commit()
        }


    }

    private fun loadGoals() {
        val db = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            // 1. Fetching all goals using the Dao method we added
            val allGoals = db.categoryGoalDao().getAllGoals()

            val displayList = allGoals.mapNotNull { goal ->
                val category = db.categoryDao().getCategoryById(goal.categoryID)

                // 2. Date Filtering using Month and Year from Goal Dao instead of goalDate
                // We convert the selected dateFrom/dateTo back to month/year for comparison
                val calFrom = dateFrom?.let { Calendar.getInstance().apply { timeInMillis = it } }
                val calTo = dateTo?.let { Calendar.getInstance().apply { timeInMillis = it } }

                val goalMonth = goal.month // From Dao
                val goalYear = goal.year   // From Dao

                val isAfterStart = calFrom?.let {
                    val startMonth = it.get(Calendar.MONTH) + 1
                    val startYear = it.get(Calendar.YEAR)
                    (goalYear > startYear) || (goalYear == startYear && goalMonth >= startMonth)
                } ?: true

                val isBeforeEnd = calTo?.let {
                    val endMonth = it.get(Calendar.MONTH) + 1
                    val endYear = it.get(Calendar.YEAR)
                    (goalYear < endYear) || (goalYear == endYear && goalMonth <= endMonth)
                } ?: true

                if (category != null && isAfterStart && isBeforeEnd) {
                    // Fetch expenses for progress tracking
                    val expenses = db.expenseDao().getExpensesByCategory(goal.categoryID) ?: emptyList()

                    // Expense filtering still uses expenseDate for precision
                    val filteredExpenses = expenses.filter { exp ->
                        val fromOk = dateFrom?.let { exp.expenseDate >= it } ?: true
                        val toOk = dateTo?.let { exp.expenseDate <= it } ?: true
                        fromOk && toOk
                    }

                    val totalSpent = filteredExpenses.sumOf { it.expenseAmount }
                    Triple(goal, category, totalSpent)
                } else null
            }

            // 3. Sorting logic using ID (Since ID increments as they are added)
            val sortedList = when (rgSort.checkedRadioButtonId) {
                // Sort by ID is a safe proxy for "Date Added" without using a date field
                R.id.rbSortLastAdded -> displayList.sortedByDescending { it.first.categoryGoalID }
                R.id.rbSortFirstAdded -> displayList.sortedBy { it.first.categoryGoalID }
                else -> displayList.sortedBy { it.second.categoryName }
            }

            // 4. Update UI
            if (sortedList.isEmpty()) {
                tvNoGoals.visibility = View.VISIBLE
                rvGoals.visibility = View.GONE
            } else {
                tvNoGoals.visibility = View.GONE
                rvGoals.visibility = View.VISIBLE
                adapter.updateData(sortedList)
            }
        }
    }

    private fun showDatePicker(onDateSelected: (Int, Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            onDateSelected(year, month, day)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun navigateToBreakdown(categoryId: Int) {
        // Pass the categoryId directly into the constructor as the original code requires
        val fragment = CategoryBreakdownFragment(categoryId)

        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToUpdate(goalId: Int) {
        val fragment = UpdateGoalFragment().apply {
            arguments = Bundle().apply { putInt("goalID", goalId) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToDelete(goalId: Int) {
        val fragment = DeleteGoalFragment().apply {
            arguments = Bundle().apply { putInt("goalID", goalId) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        loadGoals() // Refresh when coming back from Update/Delete
    }
}