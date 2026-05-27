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
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeleteExpenseFragment : Fragment(R.layout.fragment_delete_expense_v2) {

    private lateinit var repository: ExpenseRepository
    private val firebaseCategoryRepository = FirebaseCategoryRepository()
    private var expenseToDelete: Expense? = null

    private val currentUserEmail: String by lazy {
        requireActivity().getSharedPreferences("auth", 0).getString("email", "") ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db        = AppDatabase.getDatabase(requireContext())
        val uid       = FirebaseAuth.getInstance().currentUser?.uid
        repository    = ExpenseRepository(db.expenseDao())
        val expenseId = arguments?.getInt("expenseId", -1) ?: -1

        if (expenseId == -1) {
            Toast.makeText(requireContext(), "No expense to delete.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        val tvCategory    = view.findViewById<TextView>(R.id.tvDeleteExpenseCategory)
        val tvAmount      = view.findViewById<TextView>(R.id.tvDeleteExpenseAmount)
        val tvSubCategory = view.findViewById<TextView>(R.id.tvDeleteExpenseSubCategory)
        val tvDate        = view.findViewById<TextView>(R.id.tvDeleteExpenseDate)
        val tvDescription = view.findViewById<TextView>(R.id.tvDeleteExpenseDescription)
        val btnCancel     = view.findViewById<Button>(R.id.btnDeleteExpenseCancel)
        val btnConfirm    = view.findViewById<Button>(R.id.btnDeleteExpenseConfirm)
        val dateFormat    = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        lifecycleScope.launch {
            val expense = repository.getExpenseById(expenseId)
            if (expense == null) {
                Toast.makeText(requireContext(), "Expense not found.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                return@launch
            }
            expenseToDelete = expense
            tvAmount.text      = "R %.2f".format(expense.expenseAmount)
            tvSubCategory.text = "Subcategory: ${expense.subCategoryID?.toString() ?: "—"}"
            tvDate.text        = "Date: ${dateFormat.format(Date(expense.expenseDate))}"
            tvDescription.text = "Description: ${expense.expenseDescription ?: "—"}"

            // TODO: once expense owner migrates categoryID to String firebaseId,
            // match on that field directly instead of converting Int to String
            if (uid != null) {
                firebaseCategoryRepository.getCategories(uid) { list ->
                    val category = list.firstOrNull { it.firebaseId == expense.categoryID.toString() }
                    requireActivity().runOnUiThread {
                        tvCategory.text = category?.categoryName ?: "Category ${expense.categoryID}"
                    }
                }
            } else {
                tvCategory.text = "Category ${expense.categoryID}"
            }
        }

        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }

        btnConfirm.setOnClickListener {
            val expense = expenseToDelete ?: run {
                Toast.makeText(requireContext(), "No expense loaded yet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                repository.deleteExpense(expense)
                Toast.makeText(requireContext(), "Expense deleted.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }
}