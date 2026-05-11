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
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DeleteExpenseFragment : Fragment(R.layout.fragment_delete_expense_v2) {

    private lateinit var repository: ExpenseRepository
    private lateinit var categoryRepository: CategoryRepository

    // The expense loaded from the database
    private var expenseToDelete: Expense? = null

    //Logged-in user's email

    private val currentUserEmail: String by lazy {
        requireActivity()
            .getSharedPreferences("auth", 0)
            .getString("email", "") ?: ""
    }


    // onViewCreated


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup repositories
        val db = AppDatabase.getDatabase(requireContext())
        repository         = ExpenseRepository(db.expenseDao())
        categoryRepository = CategoryRepository(db.categoryDao())

        // Get the expense ID passed from ExpensesFragment
        val expenseId = arguments?.getInt("expenseId", -1) ?: -1

        if (expenseId == -1) {
            Toast.makeText(requireContext(), "No expense to delete.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Bind views using IDs from fragment_delete_expense_.xml
        val tvCategory    = view.findViewById<TextView>(R.id.tvDeleteExpenseCategory)
        val tvAmount      = view.findViewById<TextView>(R.id.tvDeleteExpenseAmount)
        val tvSubCategory = view.findViewById<TextView>(R.id.tvDeleteExpenseSubCategory)
        val tvDate        = view.findViewById<TextView>(R.id.tvDeleteExpenseDate)
        val tvDescription = view.findViewById<TextView>(R.id.tvDeleteExpenseDescription)
        //val tvFrequency   = view.findViewById<TextView>(R.id.tvDeleteExpenseFrequency)
        val btnCancel     = view.findViewById<Button>(R.id.btnDeleteExpenseCancel)
        val btnConfirm    = view.findViewById<Button>(R.id.btnDeleteExpenseConfirm)

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())


        // Load expense from DB and populate the summary card

        lifecycleScope.launch {
            val expense = repository.getExpenseById(expenseId)

            if (expense == null) {
                Toast.makeText(requireContext(), "Expense not found.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                return@launch
            }

            // Store reference so CONFIRM button can use it
            expenseToDelete = expense

            // Look up the category name from Room
            val category = categoryRepository.getCategoryById(expense.categoryID)
            tvCategory.text = category?.categoryName ?: "Category ${expense.categoryID}"

            // Amount
            tvAmount.text = "R %.2f".format(expense.expenseAmount)

            // Sub-category
            tvSubCategory.text = "Subcategory: ${expense.subCategoryID?.toString() ?: "—"}"

            // Date
            tvDate.text = "Date: ${dateFormat.format(Date(expense.expenseDate))}"

            // Description
            tvDescription.text = "Description: ${expense.expenseDescription ?: "—"}"

            // Automation frequency
            //tvFrequency.text = "Automation: ${expense.automationFrequency ?: "—"}"
        }


        // Button listeners

        btnCancel.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        btnConfirm.setOnClickListener {
            val expense = expenseToDelete
            if (expense == null) {
                Toast.makeText(requireContext(), "No expense loaded yet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Delete from Room
                repository.deleteExpense(expense)
                Toast.makeText(requireContext(), "Expense deleted.", Toast.LENGTH_SHORT).show()
                // Pop back to ExpensesFragment
                parentFragmentManager.popBackStack()
            }
        }
    }
}