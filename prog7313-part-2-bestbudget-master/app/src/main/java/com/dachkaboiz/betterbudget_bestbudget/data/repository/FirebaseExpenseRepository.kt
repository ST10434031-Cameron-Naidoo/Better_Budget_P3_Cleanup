package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseExpenseRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // INSERT
    fun insertExpense(uid: String, expense: Expense, onResult: (Boolean) -> Unit) {
        val ref = db.child("users").child(uid).child("expenses").push()
        val firebaseId = ref.key ?: return onResult(false)
        val withId = expense.copy(firebaseId = firebaseId)
        ref.setValue(withId)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // GET ALL BY USER
    fun getExpensesByUser(uid: String, onResult: (List<Expense>) -> Unit) {
        db.child("users").child(uid).child("expenses")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children
                        .mapNotNull { it.getValue(Expense::class.java) }
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
    }

    // GET BY CATEGORY
    fun getExpensesByCategory(
        uid: String,
        categoryFirebaseId: String,
        onResult: (List<Expense>) -> Unit
    ) {
        db.child("users").child(uid).child("expenses")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children
                        .mapNotNull { it.getValue(Expense::class.java) }
                        .filter { it.categoryFirebaseId == categoryFirebaseId }
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
    }

    // GET BY SUBCATEGORY
    fun getExpensesBySubCategory(
        uid: String,
        subCategoryFirebaseId: String,
        onResult: (List<Expense>) -> Unit
    ) {
        db.child("users").child(uid).child("expenses")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children
                        .mapNotNull { it.getValue(Expense::class.java) }
                        .filter { it.subCategoryFirebaseId == subCategoryFirebaseId }
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
    }

    // GET BY ID
    fun getExpenseById(uid: String, firebaseId: String, onResult: (Expense?) -> Unit) {
        db.child("users").child(uid).child("expenses").child(firebaseId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onResult(snapshot.getValue(Expense::class.java))
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }

    // GET TOTAL SPENT BY CATEGORY IN DATE RANGE
    fun getTotalSpentByCategory(
        uid: String,
        categoryFirebaseId: String,
        startDate: Long,
        endDate: Long,
        onResult: (Double) -> Unit
    ) {
        db.child("users").child(uid).child("expenses")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val total = snapshot.children
                        .mapNotNull { it.getValue(Expense::class.java) }
                        .filter {
                            it.categoryFirebaseId == categoryFirebaseId &&
                                    it.expenseDate >= startDate &&
                                    it.expenseDate <= endDate
                        }
                        .sumOf { it.expenseAmount }
                    onResult(total)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(0.0)
                }
            })
    }

    // UPDATE
    fun updateExpense(uid: String, expense: Expense, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("expenses").child(expense.firebaseId)
            .setValue(expense)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // DELETE
    fun deleteExpense(uid: String, firebaseId: String, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("expenses").child(firebaseId)
            .removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}