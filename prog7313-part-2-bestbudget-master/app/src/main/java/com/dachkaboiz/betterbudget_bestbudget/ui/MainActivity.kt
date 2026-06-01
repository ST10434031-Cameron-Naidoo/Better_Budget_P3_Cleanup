package com.dachkaboiz.betterbudget_bestbudget.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dachkaboiz.betterbudget_bestbudget.R
import androidx.work.*
import com.dachkaboiz.betterbudget_bestbudget.ui.AutomationWorker
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    var email: String? = null
    private lateinit var tvScreenTitle: TextView

    // ── Production: runs once per day ────────────────────────────────────────



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        email = prefs.getString("email", null)

        if(email == null){
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        tvScreenTitle = findViewById(R.id.tvScreenTitle)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, ExpensesFragment())
                .commit()
            tvScreenTitle.text = "EXPENSES"
        }

        findViewById<ImageButton>(R.id.btnExpense).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, ExpensesFragment())
                .commit()
            tvScreenTitle.text = "EXPENSES"
        }

        findViewById<ImageButton>(R.id.btnCategories).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, CategoryFragment())
                .commit()
            tvScreenTitle.text = "CATEGORIES"
        }

        findViewById<ImageButton>(R.id.btnGoals).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, GoalHomeFragment())
                .addToBackStack(null)
                .commit()
            tvScreenTitle.text = "GOALS"
        }

        findViewById<ImageButton>(R.id.btnRewards).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, RewardFragment())
                .addToBackStack(null)
                .commit()
            tvScreenTitle.text = "REWARDS"
        }

        findViewById<ImageButton>(R.id.btnCategoryViewProfile).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragment, ProfileFragment())
                .addToBackStack(null)
                .commit()
            tvScreenTitle.text = "PROFILE"
        }

        val dailyRequest = PeriodicWorkRequestBuilder<AutomationWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()


        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "automation_daily",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )

// ── Testing only: runs immediately when app opens ─────────────────────────
// Remove this block before releasing to production
        val testRequest = OneTimeWorkRequestBuilder<AutomationWorker>().build()
        WorkManager.getInstance(this).enqueue(testRequest)
    }
}
