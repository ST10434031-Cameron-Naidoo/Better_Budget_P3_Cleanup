package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import kotlinx.coroutines.launch
import java.util.Calendar

class AddGoalFragment : Fragment(R.layout.fragment_add_goal){

    private var categoryList: List<Category> = listOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = (requireActivity() as MainActivity).email ?: ""
        val db = AppDatabase.getDatabase(requireContext())

        val spinner = view.findViewById<Spinner>(R.id.spCategorySelector)
        val etMin = view.findViewById<EditText>(R.id.etGoalMinAmount)
        val etMax = view.findViewById<EditText>(R.id.etGoalMaxAmount)
        val btnAdd = view.findViewById<Button>(R.id.btnAddGoal)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelGoal)

        // 1. Fetch Categories to populate the Spinner
        lifecycleScope.launch {
            categoryList = db.categoryDao().getCategoriesByUser(email)
            val names = categoryList.map { it.categoryName }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
            spinner.adapter = adapter
        }

        // 2. Save logic
        btnAdd.setOnClickListener {
            val selectedPosition = spinner.selectedItemPosition
            val maxText = etMax.text.toString()

            if (selectedPosition == AdapterView.INVALID_POSITION) {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (maxText.isBlank()) {
                Toast.makeText(requireContext(), "Max goal is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCategory = categoryList[selectedPosition]
            val cal = Calendar.getInstance()

            lifecycleScope.launch {
                val goal = CategoryGoal(
                    categoryID = selectedCategory.categoryID,
                    minGoal = etMin.text.toString().toDoubleOrNull(),
                    maxGoal = maxText.toDoubleOrNull(),
                    month = cal.get(Calendar.MONTH) + 1,
                    year = cal.get(Calendar.YEAR)
                    
                    
                )

                db.categoryGoalDao().insertCategoryGoal(goal)
                Toast.makeText(requireContext(), "Goal Added!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}