package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SubCategoryRepository(private val uid: String) {

    private val db = FirebaseDatabase.getInstance().reference
    private val subCatRef = db.child("users").child(uid).child("subCategories")

    fun generateSubCategoryId(): String = subCatRef.push().key!!

    suspend fun insertSubCategory(sub: SubCategory) =
        suspendCoroutine<Unit> { cont ->
            subCatRef.child(sub.firebaseId)
                .setValue(sub)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }

    suspend fun getSubCategories(categoryFirebaseId: String): List<SubCategory> =
        suspendCoroutine { cont ->
            subCatRef.orderByChild("parentFirebaseId")
                .equalTo(categoryFirebaseId)
                .get()
                .addOnSuccessListener { snap ->
                    val list = snap.children.mapNotNull { it.getValue(SubCategory::class.java) }
                    cont.resume(list)
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

    suspend fun getSubCategoryById(id: String): SubCategory? =
        suspendCoroutine { cont ->
            subCatRef.child(id)
                .get()
                .addOnSuccessListener { snap ->
                    cont.resume(snap.getValue(SubCategory::class.java))
                }
                .addOnFailureListener { cont.resume(null) }
        }

    suspend fun updateSubCategory(sub: SubCategory) =
        suspendCoroutine<Unit> { cont ->
            subCatRef.child(sub.firebaseId)
                .setValue(sub)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }

    suspend fun deleteSubCategory(id: String) =
        suspendCoroutine<Unit> { cont ->
            subCatRef.child(id)
                .removeValue()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }
}


