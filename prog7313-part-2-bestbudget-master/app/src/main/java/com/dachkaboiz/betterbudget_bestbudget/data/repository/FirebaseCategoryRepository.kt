package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseCategoryRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // INSERT
    fun insertCategory(uid: String, category: Category, onResult: (Boolean) -> Unit) {
        val ref = db.child("users").child(uid).child("categories").push()
        val firebaseId = ref.key ?: return onResult(false)
        val categoryWithId = category.copy(firebaseId = firebaseId)
        ref.setValue(categoryWithId)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // GET ALL FOR USER
    fun getCategories(uid: String, onResult: (List<Category>) -> Unit) {
        db.child("users").child(uid).child("categories")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children.mapNotNull { child ->
                        child.getValue(Category::class.java)
                    }
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
    }

    // GET SINGLE BY FIREBASE ID
    fun getCategoryById(uid: String, firebaseId: String, onResult: (Category?) -> Unit) {
        db.child("users").child(uid).child("categories").child(firebaseId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val category = snapshot.getValue(Category::class.java)
                    onResult(category)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }

    // UPDATE
    fun updateCategory(uid: String, category: Category, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("categories").child(category.firebaseId)
            .setValue(category)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // DELETE
    fun deleteCategory(uid: String, firebaseId: String, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("categories").child(firebaseId)
            .removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}