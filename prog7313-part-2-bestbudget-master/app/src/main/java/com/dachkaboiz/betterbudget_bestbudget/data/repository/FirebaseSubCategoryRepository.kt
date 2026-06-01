package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseSubCategoryRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // INSERT
    fun insertSubCategory(uid: String, subCategory: SubCategory, onResult: (Boolean) -> Unit) {
        val ref = db.child("users").child(uid).child("subCategories").push()
        val firebaseId = ref.key ?: return onResult(false)
        val withId = subCategory.copy(firebaseId = firebaseId)
        ref.setValue(withId)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // GET ALL BY PARENT CATEGORY
    fun getSubCategoriesByCategory(
        uid: String,
        parentFirebaseId: String,
        onResult: (List<SubCategory>) -> Unit
    ) {
        db.child("users").child(uid).child("subCategories")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children
                        .mapNotNull { it.getValue(SubCategory::class.java) }
                        .filter { it.parentFirebaseId == parentFirebaseId }
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
    }

    // GET ALL SUBCATEGORIES FOR USER
    fun getAllSubCategories(uid: String, onResult: (List<SubCategory>) -> Unit) {
        db.child("users").child(uid).child("subCategories")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children
                        .mapNotNull { it.getValue(SubCategory::class.java) }
                    onResult(list)
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
    }

    // GET SINGLE BY FIREBASE ID
    fun getSubCategoryById(
        uid: String,
        firebaseId: String,
        onResult: (SubCategory?) -> Unit
    ) {
        db.child("users").child(uid).child("subCategories").child(firebaseId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onResult(snapshot.getValue(SubCategory::class.java))
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }

    // UPDATE
    fun updateSubCategory(uid: String, subCategory: SubCategory, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("subCategories").child(subCategory.firebaseId)
            .setValue(subCategory)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // DELETE
    fun deleteSubCategory(uid: String, firebaseId: String, onResult: (Boolean) -> Unit) {
        db.child("users").child(uid).child("subCategories").child(firebaseId)
            .removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}