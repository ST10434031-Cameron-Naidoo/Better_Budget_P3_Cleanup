package com.dachkaboiz.betterbudget_bestbudget.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.adapter.CategoryAdapter
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryGoalRepository
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.CategoryViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class CategoryFragment : Fragment(R.layout.fragment_category) {

    private lateinit var viewModel: CategoryViewModel
    private lateinit var adapter: CategoryAdapter<Triple<Category, CategoryGoal?, List<Expense>>>

    private var dateFrom: Long? = null
    private var dateTo: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val rgSortOrder = view.findViewById<RadioGroup>(R.id.rgFullCategorySort)
        val dpStart     = view.findViewById<TextView>(R.id.dpFullCategoryStartDate)
        val dpFinish    = view.findViewById<TextView>(R.id.dpFullCategoryEndDate)
        val listView    = view.findViewById<ListView>(R.id.lvFullCategories)
        val btnAdd      = view.findViewById<Button>(R.id.btnAddFullCategory)

        viewModel = ViewModelProvider(this)[CategoryViewModel::class.java]

        btnAdd.setOnClickListener {
            swapToFragment(AddCategoryFragment())
        }

        adapter = CategoryAdapter(
            context = requireActivity(),
            items = emptyList(),
            parentFirebaseId = "ROOT",
            onItemClick = { triple ->
                val cat = triple.first
                swapToFragment(CategoryBreakdownFragment(cat.firebaseId))
            },
            onEditClick = { triple ->
                val cat = triple.first
                swapToFragment(UpdateCategoryFragment(cat.firebaseId))
            },
            onDeleteClick = { triple ->
                val cat = triple.first
                swapToFragment(DeleteCategoryFragment(cat.firebaseId))
            }
        )

        listView.adapter = adapter

        viewModel.loadCategories(uid)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collect { categoryList ->
                buildAndDisplayList(categoryList, rgSortOrder)
            }
        }

        rgSortOrder.setOnCheckedChangeListener { _, _ ->
            buildAndDisplayList(viewModel.categories.value, rgSortOrder)
        }

        dpStart.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dateFrom = cal.timeInMillis
                dpStart.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                buildAndDisplayList(viewModel.categories.value, rgSortOrder)
            }
        }

        dpFinish.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                dateTo = cal.timeInMillis
                dpFinish.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                buildAndDisplayList(viewModel.categories.value, rgSortOrder)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModel.loadCategories(uid)
    }

    private fun buildAndDisplayList(
        categoryList: List<Category>,
        rgSortOrder: RadioGroup
    ) {
        lifecycleScope.launch {

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            val goalRepo = FirebaseCategoryGoalRepository(uid)
            val expenseRepo = ExpenseRepository(uid)

            val allGoals = goalRepo.getAllGoals()

            val combined = categoryList.map { cat ->

                val goal = allGoals.firstOrNull { it.categoryId == cat.firebaseId }

                val expenses = expenseRepo.getExpensesByCategory(cat.firebaseId)

                val filteredExpenses = expenses.filter { exp ->
                    val fromOk = dateFrom?.let { exp.expenseDate >= it } ?: true
                    val toOk   = dateTo?.let { exp.expenseDate <= it } ?: true
                    fromOk && toOk
                }

                Triple(cat, goal, filteredExpenses)
            }

            val sorted = when (rgSortOrder.checkedRadioButtonId) {
                R.id.rbFullCategoryFirstAdded -> combined.sortedBy { it.first.firebaseId }
                R.id.rbFullCategoryLastAdded  -> combined.sortedByDescending { it.first.firebaseId }
                else                          -> combined.sortedBy { it.first.categoryName }
            }

            adapter.updateItems(sorted)
        }
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

    private fun swapToFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(null)
            .commit()
    }
}
