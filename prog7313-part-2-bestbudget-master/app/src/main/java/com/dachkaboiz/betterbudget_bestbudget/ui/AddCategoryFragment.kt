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

class AddCategoryFragment : Fragment(R.layout.fragment_add_category) {

    private val repository = FirebaseCategoryRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val email = requireActivity()
            .getSharedPreferences("auth", 0)
            .getString("email", "") ?: ""

        val etName        = view.findViewById<EditText>(R.id.etCategoryName)
        val etIcon        = view.findViewById<EditText>(R.id.etCategoryIcon)
        val etDescription = view.findViewById<EditText>(R.id.etCategoryDescription)
        val btnCancel     = view.findViewById<Button>(R.id.btnCategoryCancel)
        val btnAdd        = view.findViewById<Button>(R.id.btnCategoryAdd)

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnAdd.setOnClickListener {
            if (uid == null) {
                Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name        = etName.text.toString().trim()
            val icon        = etIcon.text.toString().trim()
            val description = etDescription.text.toString().trim()

            var hasError = false

            if (name.isEmpty()) {
                etName.error = "Category name is required"
                hasError = true
            }

            if (icon.isEmpty()) {
                etIcon.error = "Icon is required"
                hasError = true
            } else if (!isEmojiOnly(icon)) {
                etIcon.error = "Please enter only emojis"
                hasError = true
            }

            if (description.isEmpty()) {
                etDescription.error = "Description is required"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            val category = Category(
                userEmail           = email,
                categoryName        = name,
                categoryIcon        = icon,
                categoryDescription = description
            )

            repository.insertCategory(uid, category) { success ->
                requireActivity().runOnUiThread {
                    if (success) {
                        // Option A — goals are set separately
                        // Goal fields are still visible in the XML layout
                        // but are not saved here because CategoryGoal
                        // is owned by a teammate and still uses Room.
                        // Once they migrate, they will wire goal saving
                        // back into this screen using firebaseId.
                        Toast.makeText(
                            requireContext(),
                            "Category added! Set goals from the Goals screen.",
                            Toast.LENGTH_LONG
                        ).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to add category",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun isEmojiOnly(text: String): Boolean {
        if (text.isEmpty()) return false
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val type = Character.getType(codePoint)
            if (type != Character.SURROGATE.toInt() &&
                type != Character.OTHER_SYMBOL.toInt() &&
                type != Character.NON_SPACING_MARK.toInt() &&
                type != Character.COMBINING_SPACING_MARK.toInt()
            ) {
                return false
            }
            i += Character.charCount(codePoint)
        }
        return true
    }
}