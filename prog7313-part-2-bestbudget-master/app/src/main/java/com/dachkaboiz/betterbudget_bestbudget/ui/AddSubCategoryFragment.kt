package com.dachkaboiz.betterbudget_bestbudget.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.repository.FirebaseCategoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class AddSubCategoryFragment(private val parentFirebaseId: String)
    : Fragment(R.layout.fragment_add_subcategory) {

    private val firebaseCategoryRepository = FirebaseCategoryRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val tvCatName     = view.findViewById<TextView>(R.id.tvParentCategory)
        val etName        = view.findViewById<EditText>(R.id.etSubcategoryName)
        val etIcon        = view.findViewById<EditText>(R.id.etSubCategoryIcon)
        val etDescription = view.findViewById<EditText>(R.id.etSubDescription)
        val etMinGoal     = view.findViewById<EditText>(R.id.etSubMinGoal)
        val etMaxGoal     = view.findViewById<EditText>(R.id.etSubMaxGoal)
        val btnAdd        = view.findViewById<Button>(R.id.btnSubAdd)
        val btnCancel     = view.findViewById<Button>(R.id.btnSubCancel)

        // Load parent category name from Firebase
        if (uid != null) {
            firebaseCategoryRepository.getCategoryById(uid, parentFirebaseId) { cat ->
                requireActivity().runOnUiThread {
                    tvCatName.text = cat?.categoryName ?: ""
                }
            }
        }

        btnAdd.setOnClickListener {
            val name        = etName.text.toString().trim()
            val icon        = etIcon.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val minGoal     = etMinGoal.text.toString().toDoubleOrNull()
            val maxGoal     = etMaxGoal.text.toString().toDoubleOrNull()

            if (name.isEmpty()) {
                etName.error = "Name is required"
                return@setOnClickListener
            }
            if (icon.isBlank()) {
                etIcon.error = "Icon is required"
                return@setOnClickListener
            }

            val uidNow = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)

            val userRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(uidNow)

            // 1️⃣ Read category goal
            userRef.child("categoryGoals")
                .child(parentFirebaseId)
                .child("$year-$month")
                .get()
                .addOnSuccessListener { catGoalSnap ->

                    val catMinGoal = catGoalSnap.child("minGoal").getValue(Double::class.java) ?: 0.0
                    val catMaxGoal = catGoalSnap.child("maxGoal").getValue(Double::class.java) ?: 0.0

                    // 2️⃣ Read all subcategory goals for this category
                    userRef.child("subCategoryGoals")
                        .child(parentFirebaseId)
                        .child("$year-$month")
                        .get()
                        .addOnSuccessListener { subGoalsSnap ->

                            var totalMinSubGoal = 0.0
                            var totalMaxSubGoal = 0.0

                            for (child in subGoalsSnap.children) {
                                totalMinSubGoal += child.child("minGoal").getValue(Double::class.java) ?: 0.0
                                totalMaxSubGoal += child.child("maxGoal").getValue(Double::class.java) ?: 0.0
                            }

                            val safeMinGoal = minGoal ?: 0.0
                            val safeMaxGoal = maxGoal ?: 0.0

//                            val goalsValid =
//                                (catMinGoal >= totalMinSubGoal + safeMinGoal) &&
//                                        (catMaxGoal >= totalMaxSubGoal + safeMaxGoal)
//
//                            if (!goalsValid) {
//                                Toast.makeText(
//                                    requireContext(),
//                                    "Min or max goal total is greater than category min or max goal",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                                return@addOnSuccessListener
//                            }

                            // 3️⃣ Create subcategory
                            val subCatId = userRef.child("subcategories").push().key!!

                            val newSubCategory = mapOf(
                                "parentFirebaseId" to parentFirebaseId,
                                "subCategoryName" to name,
                                "subCategoryIcon" to icon,
                                "subCategoryDescription" to description
                            )

                            userRef.child("subcategories").child(subCatId)
                                .setValue(newSubCategory)
                                .addOnSuccessListener {

                                    // 4️⃣ Add subcategory goal
                                    if (minGoal != null || maxGoal != null) {
                                        val goal = mapOf(
                                            "subCategoryID" to subCatId,
                                            "categoryID" to parentFirebaseId,
                                            "minGoal" to minGoal,
                                            "maxGoal" to maxGoal,
                                            "month" to month,
                                            "year" to year
                                        )

                                        userRef.child("subCategoryGoals")
                                            .child(parentFirebaseId)
                                            .child("$year-$month")
                                            .child(subCatId)
                                            .setValue(goal)
                                    }

                                    Toast.makeText(
                                        requireContext(),
                                        "Subcategory added successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    parentFragmentManager.popBackStack()
                                }
                        }
                }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
