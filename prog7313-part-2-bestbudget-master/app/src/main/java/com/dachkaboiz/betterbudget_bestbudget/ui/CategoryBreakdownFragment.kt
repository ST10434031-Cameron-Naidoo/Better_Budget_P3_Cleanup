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
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.CategoryViewModel
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.SubCategoryViewModel
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.SubCategoryViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class CategoryBreakdownFragment(
    val parentCategoryId: String  // was: parentCategoryID: Int
) : Fragment(R.layout.fragment_category_breakdown_v2) {

    private lateinit var catViewModel: CategoryViewModel
    private lateinit var subCatViewModel: SubCategoryViewModel

    private var dateFrom: Long? = null
    private var dateTo: Long? = null
    private var sortMode = SORT_AZ

    companion object {
        const val SORT_AZ        = 0
        const val SORT_LAST_USED = 1
        const val SORT_MOST_USED = 2
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid          = FirebaseAuth.getInstance().currentUser?.uid
        val rgSortOrder  = view.findViewById<RadioGroup>(R.id.rgCategorySort)
        val dpStart      = view.findViewById<TextView>(R.id.dpCategoryStartDate)
        val dpFinish     = view.findViewById<TextView>(R.id.dpCategoryEndDate)
        val lvPrimary    = view.findViewById<ListView>(R.id.lvPrimary)
        val lvSub        = view.findViewById<ListView>(R.id.lvCategories)
        val btnAddSub    = view.findViewById<Button>(R.id.btnAddSubCategory)

        val db     = AppDatabase.getDatabase(requireContext())
        val subDao = db.subCategoryDao()

        // ViewModels
        catViewModel = ViewModelProvider(this)[CategoryViewModel::class.java]
        subCatViewModel = ViewModelProvider(
            this,
            SubCategoryViewModelFactory(SubCategoryRepository(subDao))
        )[SubCategoryViewModel::class.java]

        // PRIMARY CATEGORY ADAPTER
        val primaryAdapter = CategoryAdapter<Any>(
            context           = requireActivity(),
            items             = emptyList(),
            intParentID       = -1,
            showBreakdownButton = false,
            onItemClick       = null,
            onEditClick       = { item ->
                val cat = extractCategory(item)
                cat?.let { swapToFragment(UpdateCategoryFragment(it.firebaseId)) }
            },
            onDeleteClick     = { item ->
                val cat = extractCategory(item)
                cat?.let { swapToFragment(DeleteCategoryFragment(it.firebaseId)) }
            }
        )
        lvPrimary.adapter = primaryAdapter

        // SUBCATEGORY ADAPTER — still uses Room for now
        val subAdapter = CategoryAdapter<Any>(
            context           = requireActivity(),
            items             = emptyList(),
            intParentID       = -1,
            showBreakdownButton = false,
            onItemClick       = null,
            onEditClick       = { item ->
                val sub = extractSubCategory(item)
                // TODO: update when SubCategory migrates to Firebase
                sub?.let { swapToFragment(UpdateSubCategoryFragment(0, it.subCategoryID)) }
            },
            onDeleteClick     = { item ->
                val sub = extractSubCategory(item)
                // TODO: update when SubCategory migrates to Firebase
                sub?.let { swapToFragment(DeleteSubCategoryFragment(0, it.subCategoryID)) }
            }
        )
        lvSub.adapter = subAdapter

        btnAddSub.setOnClickListener {
            // TODO: update parentID when SubCategory migrates to Firebase
            swapToFragment(AddSubCategoryFragment(parentID = 0))
        }

        // Load primary category
        if (uid != null) {
            catViewModel.loadCategory(uid, parentCategoryId)
        }

        // Observe primary category
        viewLifecycleOwner.lifecycleScope.launch {
            catViewModel.category.collect { cat ->
                if (cat != null) {
                    // TODO: wire goal after CategoryGoal migration
                    // TODO: wire expenses after expense migration
                    val combined = listOf(Triple(cat, null, emptyList<Any>()))
                    primaryAdapter.updateItems(combined)
                }
            }
        }

        // Subcategories — still Room
        // TODO: migrate to Firebase when SubCategory migration is done
        subCatViewModel.loadSubCategories(0)
        viewLifecycleOwner.lifecycleScope.launch {
            subCatViewModel.subCategories.collect { list ->
                val combined = list.map { sub ->
                    Triple(sub, null, emptyList<Any>())
                }
                subAdapter.updateItems(combined)
            }
        }

        // Date pickers
        dpStart.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                dateFrom = cal.timeInMillis
                dpStart.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
            }
        }

        dpFinish.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 23, 59, 59)
                cal.set(Calendar.MILLISECOND, 999)
                dateTo = cal.timeInMillis
                dpFinish.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
            }
        }

        rgSortOrder.setOnCheckedChangeListener { _, checkedId ->
            sortMode = when (checkedId) {
                R.id.rbCategorySortAZ       -> SORT_AZ
                R.id.rbCategorySortLastUsed -> SORT_LAST_USED
                R.id.rbCategorySortMostUsed -> SORT_MOST_USED
                else                        -> SORT_AZ
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        uid?.let { catViewModel.loadCategory(it, parentCategoryId) }
    }

    private fun extractCategory(item: Any): Category? {
        return when (item) {
            is Category -> item
            is Triple<*, *, *> -> item.first as? Category
            else -> null
        }
    }

    private fun extractSubCategory(item: Any): SubCategory? {
        return when (item) {
            is SubCategory -> item
            is Triple<*, *, *> -> item.first as? SubCategory
            else -> null
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