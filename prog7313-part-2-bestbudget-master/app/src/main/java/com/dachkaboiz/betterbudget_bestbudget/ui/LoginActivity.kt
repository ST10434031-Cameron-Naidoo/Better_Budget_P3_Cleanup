package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.UserViewModel
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.database.AppDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.repository.UserRepository
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.AuthState
import com.dachkaboiz.betterbudget_bestbudget.viewmodel.UserViewModelFactory

class LoginActivity : AppCompatActivity() {
    private lateinit var userViewModel: UserViewModel
    private lateinit var emailAddress: String

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showEmailDialog() {
        val emailInput = EditText(this).apply {
            hint = "Enter your email"
            setPadding(40, 40, 40, 40)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Email Required")
            .setMessage("Please enter your email address")
            .setView(emailInput)
            .setPositiveButton("Submit", null) // we override later
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Override positive button AFTER show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty() || !isValidEmail(email)) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Email entered: $email", Toast.LENGTH_SHORT).show()
                dialog.dismiss() // only close when valid
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        val emailInput = findViewById<EditText>(R.id.etLIEmailAddress)
        val passwordInput = findViewById<EditText>(R.id.etLIPassword)
        val loginBtn = findViewById<Button>(R.id.btnLogin)
        val forgotPasswordTv = findViewById<TextView>(R.id.tvForgotPassword)
        val signUpTv = findViewById<TextView>(R.id.tvRegister)
        val dao = AppDatabase.getDatabase(this).userDao()
        val repository = UserRepository(dao)
        val factory = UserViewModelFactory(repository)
        userViewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]

        // LOGIN BUTTON
        loginBtn.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            userViewModel.login(email, password)
            emailAddress=email


        }
        observeLoginState()
        // FORGOT PASSWORD
        forgotPasswordTv.setOnClickListener {
            showEmailDialog()
        }

//         Register
        signUpTv.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

    }
    private fun observeLoginState() {
        lifecycleScope.launchWhenStarted {
            userViewModel.loginState.collect { state ->
                when (state) {

                    is AuthState.Loading -> {
                        // Optional: show a loading spinner
                    }

                    is AuthState.Success -> {
                        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
                        prefs.edit().putString("email", emailAddress).apply()
                        AlertDialog.Builder(this@LoginActivity)
                            .setTitle("Success")
                            .setMessage("Login successful")
                            .setPositiveButton("Continue") { _, _ ->
                                //set to mainActivity once it has been set up
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("email", emailAddress)
                                startActivity(intent)
                            }
                            .setCancelable(false)
                            .show()

                    }

                    is AuthState.Error -> {
                        Toast.makeText(
                            this@LoginActivity,
                            state.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> Unit
                }
            }
        }
    }


}