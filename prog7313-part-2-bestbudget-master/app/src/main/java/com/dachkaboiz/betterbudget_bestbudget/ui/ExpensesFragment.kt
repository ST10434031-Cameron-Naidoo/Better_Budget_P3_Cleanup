package com.dachkaboiz.betterbudget_bestbudget.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.adapter.ExpenseAdapter
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpensesFragment : Fragment(R.layout.fragment_expenses) {

    private lateinit var adapter: ExpenseAdapter
    private lateinit var rvExpenses: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var rgSortOrder: RadioGroup
    private lateinit var tvDateFrom: TextView
    private lateinit var tvDateTo: TextView
    private lateinit var repository: ExpenseRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var subCategoryRepository: SubCategoryRepository

    // Cache of categories and subcategories for name/emoji lookup
    private var categoryCache: List<Category> = emptyList()
    private var subCategoryCache: List<SubCategory> = emptyList()

    private var dateFrom: Long? = null
    private var dateTo: Long? = null

    private val currentUserEmail: String by lazy {
        requireActivity()
            .getSharedPreferences("auth", 0)
            .getString("email", "") ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        repository            = ExpenseRepository(db.expenseDao())
        categoryRepository    = CategoryRepository(db.categoryDao())
        subCategoryRepository = SubCategoryRepository(db.subCategoryDao())

        rvExpenses   = view.findViewById(R.id.rvExpenses)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        rgSortOrder  = view.findViewById(R.id.rgSortOrder)
        tvDateFrom   = view.findViewById(R.id.tvDateFrom)
        tvDateTo     = view.findViewById(R.id.tvDateTo)

        // Build adapter with category/subcategory name and emoji resolvers
        adapter = ExpenseAdapter(
            onItemClick     = { expense -> showDetailDialog(expense) },
            onItemLongClick = { /* reserved */ },
            onEditClick     = { expense -> navigateToEdit(expense) },
            onDeleteClick   = { expense -> navigateToDelete(expense) },
            categoryNameResolver = { categoryId ->
                val cat = categoryCache.find { it.categoryID == categoryId }
                "${cat?.categoryIcon ?: "💰"} ${cat?.categoryName ?: "Category $categoryId"}"
            },
            subCategoryNameResolver = { subCategoryId ->
                val sub = subCategoryCache.find { it.subCategoryID == subCategoryId }
                "${sub?.subCategoryIcon ?: ""} ${sub?.subCategoryName ?: "Subcategory $subCategoryId"}"
            }
        )
        rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        rvExpenses.adapter = adapter

        // Load categories and subcategories into cache then load expenses
        lifecycleScope.launch {
            categoryCache    = categoryRepository.getCategoriesByUser(currentUserEmail)
            subCategoryCache = categoryCache.flatMap {
                subCategoryRepository.getSubCategoriesByCategory(it.categoryID)
            }
            loadExpenses()
        }

        tvDateFrom.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                dateFrom = cal.timeInMillis
                tvDateFrom.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                loadExpenses()
            }
        }

        tvDateTo.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 23, 59, 59)
                cal.set(Calendar.MILLISECOND, 999)
                dateTo = cal.timeInMillis
                tvDateTo.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                loadExpenses()
            }
        }

        rgSortOrder.setOnCheckedChangeListener { _, _ -> loadExpenses() }

        view.findViewById<MaterialButton>(R.id.btnAddExpense).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, AddExpenseFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            categoryCache    = categoryRepository.getCategoriesByUser(currentUserEmail)
            subCategoryCache = categoryCache.flatMap {
                subCategoryRepository.getSubCategoriesByCategory(it.categoryID)
            }
            loadExpenses()
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            var expenses = repository.getExpenseByUser(currentUserEmail)

            val from = dateFrom
            val to   = dateTo
            if (from != null) expenses = expenses.filter { it.expenseDate >= from }
            if (to   != null) expenses = expenses.filter { it.expenseDate <= to }

            val sorted = when (rgSortOrder.checkedRadioButtonId) {
                R.id.rbSortFirstAdded -> expenses.sortedBy { it.expenseID }
                R.id.rbSortLastAdded  -> expenses.sortedByDescending { it.expenseID }
                else                  -> expenses.sortedBy {
                    categoryCache.find { c -> c.categoryID == it.categoryID }?.categoryName ?: ""
                }
            }

            adapter.submitList(sorted.toMutableList())
            updateEmptyState(sorted)
        }
    }

    private fun updateEmptyState(list: List<Expense>) {
        if (list.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvExpenses.visibility   = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvExpenses.visibility   = View.VISIBLE
        }
    }

    private fun navigateToEdit(expense: Expense) {
        val fragment = EditExpenseFragment()
        fragment.arguments = Bundle().apply { putInt("expenseId", expense.expenseID) }
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToDelete(expense: Expense) {
        val fragment = DeleteExpenseFragment()
        fragment.arguments = Bundle().apply { putInt("expenseId", expense.expenseID) }
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showDatePicker(onDateSelected: (Int, Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day -> onDateSelected(year, month, day) },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showDetailDialog(expense: Expense) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateString = dateFormat.format(Date(expense.expenseDate))
        val catName = categoryCache.find { it.categoryID == expense.categoryID }?.categoryName
            ?: "Category ${expense.categoryID}"
        val subName = expense.subCategoryID?.let { subId ->
            subCategoryCache.find { it.subCategoryID == subId }?.subCategoryName
        }

        val message = buildString {
            append("Category: $catName\n")
            if (subName != null) append("Subcategory: $subName\n")
            append("Amount: R %.2f\n".format(expense.expenseAmount))
            append("Date: $dateString\n")
            if (!expense.expenseDescription.isNullOrBlank())
                append("Description: ${expense.expenseDescription}\n")
            if (expense.imageUri != null) append("Photo: Attached")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Expense Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }
}