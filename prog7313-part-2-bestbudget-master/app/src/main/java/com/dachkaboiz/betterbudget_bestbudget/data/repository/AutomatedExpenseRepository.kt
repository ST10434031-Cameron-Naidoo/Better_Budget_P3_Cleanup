package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.google.firebase.database.FirebaseDatabase

private lateinit var repository: ExpenseRepository
private lateinit var automatedRepo: AutomatedExpenseRepository
private val firebaseCategoryRepository = FirebaseCategoryRepository()


class AutomatedExpenseRepository (private val uid: String) {

    private val ref = FirebaseDatabase.getInstance().reference
        .child("users").child(uid).child("automatedExpenses")

    fun insertAutomatedExpense(
        expense: AutomatedExpense,
        callback: (Boolean) -> Unit
    ) {
        val key = ref.push().key ?: return callback(false)
        ref.child(key).setValue(expense.copy(firebaseId = key))
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    suspend fun getAll(): List<AutomatedExpense> = suspendCoroutine { cont ->
        ref.get()
            .addOnSuccessListener { snap ->
                val list = snap.children.mapNotNull { child ->
                    child.getValue(AutomatedExpense::class.java)
                        ?.copy(firebaseId = child.key ?: "")
                }
                cont.resume(list)
            }
            .addOnFailureListener { cont.resume(emptyList()) }
    }

    suspend fun update(expense: AutomatedExpense) = suspendCoroutine<Unit> { cont ->
        ref.child(expense.firebaseId).setValue(expense)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resume(Unit) }
    }

    suspend fun delete(firebaseId: String) = suspendCoroutine<Unit> { cont ->
        ref.child(firebaseId).removeValue()
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resume(Unit) }
    }

    suspend fun toggleActive(expense: AutomatedExpense) = suspendCoroutine<Unit> { cont ->
        ref.child(expense.firebaseId).child("active").setValue(!expense.active)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resume(Unit) }
    }

}