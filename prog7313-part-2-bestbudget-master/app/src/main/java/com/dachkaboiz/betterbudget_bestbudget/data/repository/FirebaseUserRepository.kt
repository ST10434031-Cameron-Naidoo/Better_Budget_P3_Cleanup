package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseUserRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // INSERT
    fun insertUser(user: User, onResult: (Boolean) -> Unit) {
        db.child("users").child(user.email.replace(".", ","))
            .setValue(user)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // GET BY EMAIL
    fun getUserByEmail(email: String, onResult: (User?) -> Unit) {
        db.child("users").child(email.replace(".", ","))
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onResult(snapshot.getValue(User::class.java))
                }
                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }

    // UPDATE
    fun updateUser(user: User, onResult: (Boolean) -> Unit) {
        db.child("users").child(user.email.replace(".", ","))
            .setValue(user)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // DELETE
    fun deleteUser(email: String, onResult: (Boolean) -> Unit) {
        db.child("users").child(email.replace(".", ","))
            .removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // LOGIN CHECK
    fun loginUser(email: String, password: String, onResult: (Boolean) -> Unit) {
        getUserByEmail(email) { user ->
            onResult(user != null && user.password == password)
        }
    }

    // USER EXISTS CHECK
    fun userExists(email: String, onResult: (Boolean) -> Unit) {
        getUserByEmail(email) { user ->
            onResult(user != null)
        }
    }
}