package com.dachkaboiz.betterbudget_bestbudget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository

class SubCategoryViewModelFactory(
    private val repository: SubCategoryRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubCategoryViewModel::class.java)) {
            return SubCategoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
