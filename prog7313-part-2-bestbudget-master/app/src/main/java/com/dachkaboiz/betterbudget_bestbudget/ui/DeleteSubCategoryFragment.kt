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
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class DeleteSubCategoryFragment(
    private val parentID: Int,
    private val subID: Int
) : Fragment(R.layout.fragment_delete_subcategory) {

    private var targetSubCategory: SubCategory? = null
    private val firebaseCategoryRepository = FirebaseCategoryRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db  = AppDatabase.getDatabase(requireContext())
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val tvIcon       = view.findViewById<TextView>(R.id.tvDeleteSubIcon)
        val tvName       = view.findViewById<TextView>(R.id.tvDeleteSubName)
        val tvParent     = view.findViewById<TextView>(R.id.tvDeleteSubParentCategory)
        val tvDescription= view.findViewById<TextView>(R.id.tvDeleteSubDescription)
        val tvMinGoal    = view.findViewById<TextView>(R.id.tvDeleteSubMinGoal)
        val tvMaxGoal    = view.findViewById<TextView>(R.id.tvDeleteSubMaxGoal)
        val btnConfirm   = view.findViewById<Button>(R.id.btnDeleteSubConfirm)
        val btnCancel    = view.findViewById<Button>(R.id.btnDeleteSubCancel)

        lifecycleScope.launch {
            targetSubCategory = db.subCategoryDao().getSubCategoryById(subID)
            val goal = db.subCategoryGoalDao().getGoalsBySubCategory(subID)

            targetSubCategory?.let { sub ->
                tvIcon.text        = sub.subCategoryIcon
                tvName.text        = sub.subCategoryName
                tvDescription.text = "Description: ${sub.subCategoryDescription ?: "—"}"
            }

            tvMinGoal.text = "Min Goal: ${goal?.minGoal?.toString()?.let { "R $it" } ?: "—"}"
            tvMaxGoal.text = "Max Goal: ${goal?.maxGoal?.toString()?.let { "R $it" } ?: "—"}"

            // Load parent category name from Firebase
            if (uid != null) {
                firebaseCategoryRepository.getCategoryById(uid, parentID.toString()) { cat ->
                    requireActivity().runOnUiThread {
                        tvParent.text = "Parent Category: ${cat?.categoryName ?: "—"}"
                    }
                }
            } else {
                tvParent.text = "Parent Category: —"
            }
        }

        btnConfirm.setOnClickListener {
            lifecycleScope.launch {
                val expenseCount = db.expenseDao().getExpensesBySubCategory(subID)
                if (expenseCount.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Cannot delete: this sub category has expenses.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                targetSubCategory?.let { db.subCategoryDao().deleteSubCategory(it) }
                Toast.makeText(requireContext(), "Subcategory deleted", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
    }
}