package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.firebase.auth.FirebaseAuth

class UpdateCategoryFragment(
    private val firebaseId: String
) : Fragment(R.layout.fragment_edit_category) {

    private val repository = FirebaseCategoryRepository()
    private var currentCategory: Category? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val etName        = view.findViewById<EditText>(R.id.etEditCategoryName)
        val etIcon        = view.findViewById<EditText>(R.id.etEditCategoryIcon)
        val etDescription = view.findViewById<EditText>(R.id.etEditCategoryDescription)
        val btnUpdate     = view.findViewById<Button>(R.id.btnEditCategoryUpdate)
        val btnCancel     = view.findViewById<Button>(R.id.btnEditCategoryCancel)

        // Load existing category from Firebase using firebaseId
        // firebaseId is passed in from CategoryFragment or
        // CategoryBreakdownFragment when the user taps edit.
        // Without this load, the form would be blank on open.
        if (uid != null) {
            repository.getCategoryById(uid, firebaseId) { cat ->
                currentCategory = cat
                requireActivity().runOnUiThread {
                    cat?.let {
                        etName.setText(it.categoryName)
                        etIcon.setText(it.categoryIcon)
                        etDescription.setText(it.categoryDescription)
                    }
                }
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnUpdate.setOnClickListener {
            if (uid == null) {
                Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name        = etName.text.toString().trim()
            val icon        = etIcon.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }

            if (icon.isEmpty()) {
                etIcon.error = "Icon is required"
                return@setOnClickListener
            }

            if (description.isEmpty()) {
                etDescription.error = "Description is required"
                return@setOnClickListener
            }

            // currentCategory will be null if the Firebase load
            // hasn't returned yet when the user taps Update.
            // The ?: return@setOnClickListener guards against this.
            val updatedCategory = currentCategory?.copy(
                categoryName        = name,
                categoryIcon        = icon,
                categoryDescription = description
            ) ?: return@setOnClickListener

            repository.updateCategory(uid, updatedCategory) { success ->
                requireActivity().runOnUiThread {
                    if (success) {
                        // Option A — goal editing is done from the Goals screen
                        // Goal fields in the XML are visible but not saved here.
                        // CategoryGoal owner will wire goal update back into
                        // this screen after their Firebase migration.
                        Toast.makeText(
                            requireContext(),
                            "Category updated! Edit goals from the Goals screen.",
                            Toast.LENGTH_LONG
                        ).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to update category",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}