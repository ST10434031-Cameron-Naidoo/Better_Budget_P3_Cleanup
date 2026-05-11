package com.dachkaboiz.betterbudget_bestbudget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryRepository

class CategoryViewModelFactory(
    private val repository: CategoryRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            return CategoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
