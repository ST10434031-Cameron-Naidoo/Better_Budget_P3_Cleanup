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
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.CategoryViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class CategoryFragment : Fragment(R.layout.fragment_category_breakdown_v2) {

    private lateinit var viewModel: CategoryViewModel
    private lateinit var adapter: CategoryAdapter<Any>

    private var dateFrom: Long? = null
    private var dateTo: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val rgSortOrder = view.findViewById<RadioGroup>(R.id.rgFullCategorySort)
        val dpStart     = view.findViewById<TextView>(R.id.dpFullCategoryStartDate)
        val dpFinish    = view.findViewById<TextView>(R.id.dpFullCategoryEndDate)
        val listView    = view.findViewById<ListView>(R.id.lvFullCategories)
        val btnAdd      = view.findViewById<Button>(R.id.btnAddFullCategory)

        // ViewModel — no factory needed anymore
        viewModel = ViewModelProvider(this)[CategoryViewModel::class.java]

        btnAdd.setOnClickListener {
            swapToFragment(AddCategoryFragment())
        }

        // Adapter setup
        adapter = CategoryAdapter(
            context     = requireActivity(),
            items       = emptyList(),
            parentFirebaseId = "ROOT",
            onItemClick = { item ->
                val cat = extractCategory(item)
                cat?.let { swapToFragment(CategoryBreakdownFragment(it.firebaseId)) }
            },
            onEditClick = { item ->
                val cat = extractCategory(item)
                cat?.let { swapToFragment(UpdateCategoryFragment(it.firebaseId)) }
            },
            onDeleteClick = { item ->
                val cat = extractCategory(item)
                cat?.let { swapToFragment(DeleteCategoryFragment(it.firebaseId)) }
            }
        )
        listView.adapter = adapter

        // Load categories
        if (uid != null) {
            viewModel.loadCategories(uid)
        }

        // Observe and display
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collect { categoryList ->
                buildAndDisplayList(categoryList, rgSortOrder)
            }
        }

        // Sort changes
        rgSortOrder.setOnCheckedChangeListener { _, _ ->
            buildAndDisplayList(viewModel.categories.value, rgSortOrder)
        }

        // Date pickers
        dpStart.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                dateFrom = cal.timeInMillis
                dpStart.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                buildAndDisplayList(viewModel.categories.value, rgSortOrder)
            }
        }

        dpFinish.setOnClickListener {
            showDatePicker { year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 23, 59, 59)
                cal.set(Calendar.MILLISECOND, 999)
                dateTo = cal.timeInMillis
                dpFinish.text = "%02d-%02d-%04d ⌵".format(day, month + 1, year)
                buildAndDisplayList(viewModel.categories.value, rgSortOrder)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        uid?.let { viewModel.loadCategories(it) }
    }

    private fun buildAndDisplayList(
        categoryList: List<Category>,
        rgSortOrder: RadioGroup
    ) {
        // Build Triples — expenses and goals are null for now
        // TODO: wire expenses after expense migration
        // TODO: wire goals after CategoryGoal migration
        val combined = categoryList.map { cat ->
            Triple(cat, null, emptyList<Any>())
        }

        // Sort
        val sorted = when (rgSortOrder.checkedRadioButtonId) {
            R.id.rbFullCategoryFirstAdded -> combined.sortedBy { it.first.firebaseId }
            R.id.rbFullCategoryLastAdded  -> combined.sortedByDescending { it.first.firebaseId }
            else                          -> combined.sortedBy { it.first.categoryName }
        }

        adapter.updateItems(sorted)
    }

    private fun extractCategory(item: Any): Category? {
        return when (item) {
            is Category -> item
            is Triple<*, *, *> -> item.first as? Category
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