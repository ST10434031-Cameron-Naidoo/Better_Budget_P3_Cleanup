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
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class AddGoalFragment : Fragment(R.layout.fragment_add_goal) {

    private var categoryList: List<Category> = listOf()
    private val firebaseCategoryRepository = FirebaseCategoryRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db  = AppDatabase.getDatabase(requireContext())

        val spinner   = view.findViewById<Spinner>(R.id.spCategorySelector)
        val etMin     = view.findViewById<EditText>(R.id.etGoalMinAmount)
        val etMax     = view.findViewById<EditText>(R.id.etGoalMaxAmount)
        val btnAdd    = view.findViewById<Button>(R.id.btnAddGoal)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelGoal)

        if (uid != null) {
            firebaseCategoryRepository.getCategories(uid) { list ->
                categoryList = list
                val names = list.map { it.categoryName }
                requireActivity().runOnUiThread {
                    spinner.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        names
                    )
                }
            }
        }

        btnAdd.setOnClickListener {
            val selectedPosition = spinner.selectedItemPosition
            val maxText = etMax.text.toString()

            if (selectedPosition == AdapterView.INVALID_POSITION || categoryList.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (maxText.isBlank()) {
                Toast.makeText(requireContext(), "Max goal is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cal = Calendar.getInstance()

            // TODO: CategoryGoal owner to replace categoryID = 0
            // with firebaseId link when they migrate CategoryGoal
            lifecycleScope.launch {
                val goal = CategoryGoal(
                    categoryID = 0,
                    minGoal    = etMin.text.toString().toDoubleOrNull(),
                    maxGoal    = maxText.toDoubleOrNull(),
                    month      = cal.get(Calendar.MONTH) + 1,
                    year       = cal.get(Calendar.YEAR)
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