package com.dachkaboiz.betterbudget_bestbudget.ui

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.adapter.CategoryAdapter
import com.dachkaboiz.betterbudget_bestbudget.data.dao.CategoryGoalDao
import com.dachkaboiz.betterbudget_bestbudget.data.dao.ExpenseDao
import com.dachkaboiz.betterbudget_bestbudget.data.dao.SubCategoryGoalDao
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.CategoryViewModel
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.CategoryViewModelFactory
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.SubCategoryViewModel
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.SubCategoryViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar


class CategoryBreakdownFragment(
    val parentCategoryID: Int
) : Fragment(R.layout.fragment_category_breakdown_v2) {

    private lateinit var catViewModel: CategoryViewModel
    private lateinit var subCatViewModel: SubCategoryViewModel

    private var dateFrom: Long? = null
    private var dateTo: Long? = null
    private var sortMode: Int = 0
    private val SORT_AZ = 0
    private val SORT_LAST_USED = 1
    private val SORT_MOST_USED = 2



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rgSortOrder: RadioGroup = view.findViewById(R.id.rgCategorySort)
        val dpStart: TextView = view.findViewById(R.id.dpCategoryStartDate)
        val dpFinish: TextView = view.findViewById(R.id.dpCategoryEndDate)
        val prefs = requireActivity().getSharedPreferences("auth", 0)
        val email = prefs.getString("email", null)

        val db = AppDatabase.getDatabase(requireContext())
        val dao = db.categoryDao()
        val subdao = db.subCategoryDao()

        catViewModel = ViewModelProvider(
            this,
            CategoryViewModelFactory(CategoryRepository(dao))
        )[CategoryViewModel::class.java]

        subCatViewModel = ViewModelProvider(
            this,
            SubCategoryViewModelFactory(SubCategoryRepository(subdao))
        )[SubCategoryViewModel::class.java]

        val lvPrimaryCategory = view.findViewById<ListView>(R.id.lvPrimary)
        val lvSubCategories = view.findViewById<ListView>(R.id.lvCategories)
        val btnAddSubCategory: Button = view.findViewById(R.id.btnAddSubCategory)

        catViewModel.loadCategory(parentCategoryID)

        btnAddSubCategory.setOnClickListener {
            swapToFragment(AddSubCategoryFragment(parentID = parentCategoryID))
        }

        // PRIMARY CATEGORY ADAPTER
        val primaryAdapter = CategoryAdapter<Any>(
            context = requireActivity(),
            items = emptyList(),
            intParentID = -1,
            showBreakdownButton = false,
            onItemClick = null,
            onEditClick = { item ->
                val cat = (item as? Category) ?: (item as? Triple<*, *,*>)?.first as? Category
                cat?.let { swapToFragment(UpdateCategoryFragment(it.categoryID )) }
            },
            onDeleteClick = { item ->
                val cat = (item as? Category) ?: (item as? Triple<*, *,*>)?.first as? Category
                cat?.let { swapToFragment(DeleteCategoryFragment(it.categoryID)) }
            }
        )
        lvPrimaryCategory.adapter = primaryAdapter

        val goalDao = db.categoryGoalDao()
        val expenseDao = db.expenseDao()

        viewLifecycleOwner.lifecycleScope.launch {
            catViewModel.category.collect { cat ->
                if (cat != null) {

                    val goal = goalDao.getGoalsByCategory(cat.categoryID)
                    val expenses: List<Expense> = expenseDao.getExpensesByCategory(cat.categoryID) ?: emptyList()


                    val combined = listOf(Triple(cat, goal, expenses))

                    primaryAdapter.updateItems(combined as List<Any>)
                }
            }
        }



        // SUBCATEGORY ADAPTER
        val subAdapter = CategoryAdapter<Any>(
            context = requireActivity(),
            items = emptyList(),
            intParentID = parentCategoryID,
            showBreakdownButton = false,
            onItemClick = null,
            onEditClick = { item ->
                val sub = (item as? SubCategory) ?: (item as? Triple<*, *,*>)?.first as? SubCategory
                sub?.let { swapToFragment(UpdateSubCategoryFragment(parentCategoryID, it.subCategoryID)) }
            },
            onDeleteClick = { item ->
                val sub = (item as? SubCategory) ?: (item as? Triple<*, *,*>)?.first as? SubCategory
                sub?.let { swapToFragment(DeleteSubCategoryFragment(parentCategoryID, it.subCategoryID)) }
            }
        )
        lvSubCategories.adapter = subAdapter

        subCatViewModel.loadSubCategories(parentCategoryID)

        val subGoalDao = db.subCategoryGoalDao()


        viewLifecycleOwner.lifecycleScope.launch {
            subCatViewModel.subCategories.collect { list ->
                val combined = list.map { sub ->
                    val goal = subGoalDao.getGoalsBySubCategory(sub.subCategoryID)
                    val expense = expenseDao.getExpensesBySubCategory(sub.subCategoryID)?: emptyList()
                    Triple(sub, goal, expense)
                }
                subAdapter.updateItems(combined as List<Any>)
            }
        }

        // FROM date picker — tap the label to pick a start date
        dpStart.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                dateFrom = cal.timeInMillis
                dpStart.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                refreshAdapters(primaryAdapter, subAdapter, goalDao, expenseDao, subGoalDao)
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
                refreshAdapters(primaryAdapter, subAdapter, goalDao, expenseDao, subGoalDao)
            }
        }
        rgSortOrder.setOnCheckedChangeListener { _, checkedId ->

            sortMode = when (checkedId) {

                R.id.rbCategorySortAZ -> SORT_AZ

                R.id.rbCategorySortLastUsed -> SORT_LAST_USED

                R.id.rbCategorySortMostUsed -> SORT_MOST_USED

                else -> SORT_AZ
            }

            refreshAdapters(
                primaryAdapter,
                subAdapter,
                goalDao,
                expenseDao,
                subGoalDao
            )
        }


    }

    private fun swapToFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
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
    private fun refreshAdapters(
        primaryAdapter: CategoryAdapter<Any>,
        subAdapter: CategoryAdapter<Any>,
        goalDao: CategoryGoalDao,
        expenseDao: ExpenseDao,
        subGoalDao: SubCategoryGoalDao
    ) {
        viewLifecycleOwner.lifecycleScope.launch {

            // -----------------------------
            // PRIMARY CATEGORY
            // -----------------------------
            val cat = catViewModel.category.value
            if (cat != null) {
                val goal = goalDao.getGoalsByCategory(cat.categoryID)
                val allExpenses = expenseDao.getExpensesByCategory(cat.categoryID) ?: emptyList()

                val filtered = allExpenses.filter { exp ->
                    val afterFrom = dateFrom?.let { exp.expenseDate >= it } ?: true
                    val beforeTo  = dateTo?.let { exp.expenseDate <= it } ?: true
                    afterFrom && beforeTo
                }

                val combined = listOf(Triple(cat, goal, filtered))

                // SORT PRIMARY
                val sortedPrimary = when (sortMode) {
                    SORT_AZ -> combined.sortedBy { it.first.categoryName.lowercase() }

                    SORT_LAST_USED -> combined.sortedByDescending {
                        it.third.maxOfOrNull { e -> e.expenseDate } ?: 0L
                    }

                    SORT_MOST_USED -> combined.sortedByDescending {
                        it.third.size
                    }

                    else -> combined
                }

                primaryAdapter.updateItems(sortedPrimary as List<Any>)
            }


            // -----------------------------
            // SUBCATEGORIES
            // -----------------------------
            val subList = subCatViewModel.subCategories.value ?: emptyList()

            val combinedSub = subList.map { sub ->
                val goal = subGoalDao.getGoalsBySubCategory(sub.subCategoryID)
                val allExpenses = expenseDao.getExpensesBySubCategory(sub.subCategoryID) ?: emptyList()

                val filtered = allExpenses.filter { exp ->
                    val afterFrom = dateFrom?.let { exp.expenseDate >= it } ?: true
                    val beforeTo  = dateTo?.let { exp.expenseDate <= it } ?: true
                    afterFrom && beforeTo
                }

                Triple(sub, goal, filtered)
            }

            // SORT SUBCATEGORIES
            val sortedSub = when (sortMode) {
                SORT_AZ -> combinedSub.sortedBy { it.first.subCategoryName.lowercase() }

                SORT_LAST_USED -> combinedSub.sortedByDescending {
                    it.third.maxOfOrNull { e -> e.expenseDate } ?: 0L
                }

                SORT_MOST_USED -> combinedSub.sortedByDescending {
                    it.third.size
                }

                else -> combinedSub
            }

            subAdapter.updateItems(sortedSub as List<Any>)
        }
    }


}
