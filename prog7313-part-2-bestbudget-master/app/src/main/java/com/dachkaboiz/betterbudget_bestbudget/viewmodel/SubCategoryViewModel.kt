package com.dachkaboiz.betterbudget_bestbudget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.repository.SubCategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SubCategoryViewModel(
    private val repository: SubCategoryRepository
) : ViewModel() {

    private val _subCategories = MutableStateFlow<List<SubCategory>>(emptyList())
    val subCategories: StateFlow<List<SubCategory>> = _subCategories

    fun loadSubCategories(parentCategoryId: Int) {
        viewModelScope.launch {
            _subCategories.value = repository.getSubCategoriesByCategory(parentCategoryId)
        }
    }
}
