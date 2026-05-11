package com.dachkaboiz.betterbudget_bestbudget.ui

import android.R.attr.category
import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.InvalidationTracker
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.CategoryViewModel
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.CategoryViewModelFactory
import com.dachkaboiz.betterbudget_bestbudget.adapter.CategoryAdapter
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase.Companion.getDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.collections.emptyList

class CategoryFragment : Fragment(R.layout.fragment_category) {

    private lateinit var viewModel: CategoryViewModel
    private lateinit var adapter: CategoryAdapter<Any>
    private var categoryGoal: CategoryGoal? = null

    private lateinit var expRepository: ExpenseRepository


    private var dateFrom: Long? = null
    private var dateTo: Long? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = AppDatabase.getDatabase(requireContext())
        expRepository = ExpenseRepository(db.expenseDao())
        //Bind Views
        val rgSortOrder: RadioGroup = view.findViewById(R.id.rgFullCategorySort)
        val dpStart: TextView = view.findViewById(R.id.dpFullCategoryStartDate)
        val dpFinish: TextView = view.findViewById(R.id.dpFullCategoryEndDate)
        // 1. Setup ViewModel
        val dao = getDatabase(requireContext()).categoryDao()
        val repo = CategoryRepository(dao)
        val factory = CategoryViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[CategoryViewModel::class.java]
        val btnAddCategory = view.findViewById<Button>(R.id.btnAddFullCategory)
        btnAddCategory.setOnClickListener {
            swapToFragment(AddCategoryFragment())
        }

        // 2. Get email from MainActivity
        val prefs = requireActivity().getSharedPreferences("auth", 0)
        val email = prefs.getString("email", null)

        // Re-sort whenever the user changes the sort radio buttons
        rgSortOrder.setOnCheckedChangeListener { _, _ -> observeCategories() }

        // 3. Setup ListView + empty adapter
        val listView = view.findViewById<ListView>(R.id.lvFullCategories)



        adapter = CategoryAdapter<Any>(
            context = requireActivity(),
            items = emptyList(),
            intParentID = -1,
            onItemClick = { item ->
                when (item) {
                    is Category -> swapToFragment(CategoryBreakdownFragment(item.categoryID))
                    is Triple<*, *,*> -> {
                        val cat = item.first as? Category
                        if (cat != null) {
                            swapToFragment(CategoryBreakdownFragment(cat.categoryID))
                        }
                    }
                }
            },
            onEditClick = { item ->
                when (item) {
                    is Category -> swapToFragment(UpdateCategoryFragment(item.categoryID))
                    is Triple<*, *,*> -> {
                        val cat = item.first as? Category
                        if (cat != null) {
                            swapToFragment(UpdateCategoryFragment(cat.categoryID))
                        }
                    }
                }
            },
            onDeleteClick = { item ->
                when (item) {
                    is Category -> swapToFragment(DeleteCategoryFragment(item.categoryID))
                    is Triple<*, *,*> -> {
                        val cat = item.first as? Category
                        if (cat != null) {
                            swapToFragment(DeleteCategoryFragment(cat.categoryID))
                        }
                    }
                }
            }
        )


                    listView.adapter = adapter

        // 4. Load categories
        if (email != null) {
            viewModel.loadCategories(email)

            val goalDao = getDatabase(requireContext()).categoryGoalDao()
            val expenseDao= AppDatabase.getDatabase(requireContext()).expenseDao()

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.categories.collect { categoryList ->

                    val combined = categoryList.map { cat ->
                        val goal = goalDao.getGoalsByCategory(cat.categoryID)
                        val expenses: List<Expense> = expenseDao.getExpensesByCategory(cat.categoryID) ?: emptyList()

                        Triple(cat, goal, expenses)
                    }


                    adapter.updateItems(combined as List<Any>)
                }
            }
        }


        // 5. Observe categories
        observeCategories()
        // FROM date picker — tap the label to pick a start date
        dpStart.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                dateFrom = cal.timeInMillis
                dpStart.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                observeCategories()
            }
        }

        // TO date picker — tap the label to pick an end date
        dpFinish.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 23, 59, 59)
                cal.set(Calendar.MILLISECOND, 999)
                dateTo = cal.timeInMillis
                dpFinish.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                observeCategories()
            }
        }



    }

    private fun observeCategories() {
        val rgSortOrder = requireView().findViewById<RadioGroup>(R.id.rgFullCategorySort)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collect { categoryList ->

                val db = AppDatabase.getDatabase(requireContext())
                val goalDao = db.categoryGoalDao()
                val expenseDao = db.expenseDao()

                // Build combined list
                val combined = categoryList.map { cat ->

                    val goal = goalDao.getGoalsByCategory(cat.categoryID)
                    val expenses = expenseDao.getExpensesByCategory(cat.categoryID) ?: emptyList()

                    // Apply date filters
                    val filteredExpenses = expenses.filter { exp ->
                        val fromOk = dateFrom?.let { exp.expenseDate >= it } ?: true
                        val toOk   = dateTo?.let { exp.expenseDate <= it } ?: true
                        fromOk && toOk
                    }

                    Triple(cat, goal, filteredExpenses)
                }

                // Sorting
                val sorted = when (rgSortOrder.checkedRadioButtonId) {
                    R.id.rbFullCategoryFirstAdded -> combined.sortedBy { it.first.categoryID }
                    R.id.rbFullCategoryLastAdded  -> combined.sortedByDescending { it.first.categoryID }
                    else                  -> combined.sortedBy { it.first.categoryName }
                }

                adapter.updateItems(sorted)
            }
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


