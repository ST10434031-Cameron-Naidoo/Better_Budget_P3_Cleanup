package com.dachkaboiz.betterbudget_bestbudget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CategoryViewModel(private val repo: CategoryRepository) : ViewModel() {

    val categories = MutableStateFlow<List<Category>>(emptyList())
    private val _category = MutableStateFlow<Category?>(null)
    val category = _category


    fun loadCategories(email: String) {
        viewModelScope.launch {
            val list = repo.getCategoriesByUser(email)
            categories.value = list
        }
    }
    fun loadCategory(id: Int) {
        viewModelScope.launch {
            val category = repo.getCategoryById(id)
            _category.value = category
        }
    }

}
