package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseSubCategoryGoalRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // INSERT
    fun insertSubCategoryGoal(uid: String, subCategoryGoal: SubCategoryGoal, onResult: (Boolean) -> Unit) {
        val ref = db.child("users").child(uid).child("subCategoryGoals").push()
        val firebaseId = ref.key ?: return onResult(false)
        val withId = subCategoryGoal.copy(firebaseId = firebaseId)
        ref.setValue(withId)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // GET BY SUBCATEGORY
    fun getGoalsBySubCategory(
        uid: String,
        subCategoryFirebaseId: String,
        onResult: (SubCategoryGoal?) -> Unit
    ) {
        db.child("users").child(uid).child("subCategoryGoals")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val goal = snapshot.children
                        .mapNotNull { it.getValue(SubCategoryGoal::class.java) }
                        .firstOrNull { it.subCategoryFirebaseId == subCategoryFirebaseId }
                    onResult(goal)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }

    // GET BY SUBCATEGORY AND MONTH
    fun getGoalBySubCategoryAndMonth(
        uid: String,
        subCategoryFirebaseId: String,
        month: Int,
        year: Int,
        onResult: (SubCategoryGoal?) -> Unit
    ) {
        db.child("users").child(uid).child("subCategoryGoals")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val goal = snapshot.children
                        .mapNotNull { it.getValue(SubCategoryGoal::class.java) }
                        .firstOrNull {
                            it.subCategoryFirebaseId == subCategoryFirebaseId &&
                                    it.month == month &&
                                    it.year == year
                        }
                    onResult(goal)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }

    // GET BY CATEGORY AND MONTH
    fun getGoalsByCategoryAndMonth(
        uid: String,
        categoryFirebaseId: String,
        month: Int,
        year: Int,
        onResult: (List<SubCategoryGoal>) -> Unit
    ) {
        db.child("users").child(uid).child("subCategoryGoals")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children
                        .mapNotNull { it.getValue(SubCategoryGoal::class.java) }
                        .filter {
                            it.categoryFirebaseId == categoryFirebaseId &&
                                    it.month == month &&
                                    it.year == year
                        }
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
    }

    // UPDATE
    fun updateSubCategoryGoal(uid: String, subCategoryGoal: SubCategoryGoal, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("subCategoryGoals").child(subCategoryGoal.firebaseId)
            .setValue(subCategoryGoal)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // DELETE
    fun deleteSubCategoryGoal(uid: String, firebaseId: String, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("subCategoryGoals").child(firebaseId)
            .removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}