package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseCategoryGoalRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // INSERT
    fun insertCategoryGoal(uid: String, categoryGoal: CategoryGoal, onResult: (Boolean) -> Unit) {
        val ref = db.child("users").child(uid).child("categoryGoals").push()
        val firebaseId = ref.key ?: return onResult(false)
        val withId = categoryGoal.copy(firebaseId = firebaseId)
        ref.setValue(withId)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // GET BY CATEGORY
    fun getGoalByCategory(
        uid: String,
        categoryFirebaseId: String,
        onResult: (CategoryGoal?) -> Unit
    ) {
        db.child("users").child(uid).child("categoryGoals")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val goal = snapshot.children
                        .mapNotNull { it.getValue(CategoryGoal::class.java) }
                        .firstOrNull { it.categoryFirebaseId == categoryFirebaseId }
                    onResult(goal)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }

    // GET BY CATEGORY AND MONTH
    fun getGoalByCategoryAndMonth(
        uid: String,
        categoryFirebaseId: String,
        month: Int,
        year: Int,
        onResult: (CategoryGoal?) -> Unit
    ) {
        db.child("users").child(uid).child("categoryGoals")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val goal = snapshot.children
                        .mapNotNull { it.getValue(CategoryGoal::class.java) }
                        .firstOrNull {
                            it.categoryFirebaseId == categoryFirebaseId &&
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

    // GET ALL BY MONTH
    fun getAllGoalsByMonth(
        uid: String,
        month: Int,
        year: Int,
        onResult: (List<CategoryGoal>) -> Unit
    ) {
        db.child("users").child(uid).child("categoryGoals")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children
                        .mapNotNull { it.getValue(CategoryGoal::class.java) }
                        .filter { it.month == month && it.year == year }
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
    }

    // UPDATE
    fun updateCategoryGoal(uid: String, categoryGoal: CategoryGoal, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("categoryGoals").child(categoryGoal.firebaseId)
            .setValue(categoryGoal)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // DELETE
    fun deleteCategoryGoal(uid: String, firebaseId: String, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("categoryGoals").child(firebaseId)
            .removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}