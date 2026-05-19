package com.dachkaboiz.betterbudget_bestbudget.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dachkaboiz.betterbudget_bestbudget.data.dao.PhotoDao
import com.dachkaboiz.betterbudget_bestbudget.data.dao.CategoryDao
import com.dachkaboiz.betterbudget_bestbudget.data.dao.CategoryGoalDao
import com.dachkaboiz.betterbudget_bestbudget.data.dao.ExpenseDao
import com.dachkaboiz.betterbudget_bestbudget.data.dao.SubCategoryDao
import com.dachkaboiz.betterbudget_bestbudget.data.dao.SubCategoryGoalDao
import com.dachkaboiz.betterbudget_bestbudget.data.dao.UserDao
import com.dachkaboiz.betterbudget_bestbudget.data.model.Photo
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.model.User

@Database(
    entities = [
        User::class,
        Category::class,
        SubCategory::class,
        Expense::class,
        CategoryGoal::class,
        SubCategoryGoal::class,
        Photo:: class


    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryGoalDao(): CategoryGoalDao
    abstract fun subCategoryGoalDao(): SubCategoryGoalDao
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "betterbudget_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}