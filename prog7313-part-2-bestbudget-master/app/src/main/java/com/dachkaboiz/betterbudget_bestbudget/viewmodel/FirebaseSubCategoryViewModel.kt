package com.dachkaboiz.betterbudget_bestbudget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FirebaseSubCategoryViewModel : ViewModel() {

    private val _subCategories = MutableStateFlow<List<SubCategory>>(emptyList())
    val subCategories: StateFlow<List<SubCategory>> = _subCategories

    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val userRef = FirebaseDatabase.getInstance().reference
        .child("users")
        .child(uid ?: "")

    fun loadSubCategories(parentFirebaseId: String) {
        viewModelScope.launch {
            userRef.child("subcategories")
                .get()
                .addOnSuccessListener { snap ->

                    val list = snap.children.mapNotNull { child ->
                        val sub = child.getValue(SubCategory::class.java)
                        sub?.copy(
                            firebaseId = child.key!!  // keep real parentFirebaseId
                        )
                    }.filter { it.parentFirebaseId == parentFirebaseId }

                    _subCategories.value = list
                }
        }
    }

}
