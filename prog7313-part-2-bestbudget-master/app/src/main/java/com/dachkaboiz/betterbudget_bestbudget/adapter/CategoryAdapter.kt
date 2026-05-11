package com.dachkaboiz.betterbudget_bestbudget.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal

class CategoryAdapter<T>(
    private val context: Activity,
    private var items: List<T>,
    private val intParentID: Int,
    private val showBreakdownButton: Boolean = true,
    private val onItemClick: ((T) -> Unit)?,
    private val onEditClick: (T) -> Unit,
    private val onDeleteClick: (T) -> Unit
) : ArrayAdapter<T>(context, R.layout.item_category_v2, items) {

    fun updateItems(newItems: List<T>) {
        items = newItems
        notifyDataSetChanged()
    }

    private class ViewHolder(view: View) {
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val ivIcon: TextView = view.findViewById(R.id.tvCategoryIcon)
        val tvMinGoal: TextView = view.findViewById(R.id.tvMinGoal)
        val tvMaxGoal: TextView = view.findViewById(R.id.tvMaxGoal)
        val tvTotalSpent: TextView = view.findViewById(R.id.tvCategoryTotalSpent)
        val llGoalSection: TextView = view.findViewById(R.id.llCategoryFullExtension)
        val progressBar: ProgressBar = view.findViewById(R.id.pbCategoryProgress)
        val tvNoGoal: TextView = view.findViewById(R.id.tvNoProgressBar)
        val ivDeleteCategory: ImageView = view.findViewById(R.id.ivDeleteCategory)
        val ivEditCategory: ImageView = view.findViewById(R.id.ivEditCategory)
        val root: View = view
    }

    private val filteredItems: List<T>
        get() = items.filter { item ->
            when (item) {
                is SubCategory -> item.parentCategoryID == intParentID
                is SubCategoryGoal -> item.categoryID == intParentID
                else -> true
            }
        }

    override fun getCount(): Int = filteredItems.size
    override fun getItem(position: Int): T = filteredItems[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = context.layoutInflater.inflate(R.layout.item_category_v2, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = getItem(position)

        // Reset all states on every bind to avoid recycling issues
        holder.llGoalSection.visibility = View.GONE
        holder.progressBar.visibility = View.GONE
        holder.tvNoGoal.visibility = View.GONE
        holder.tvTotalSpent.text = ""
        holder.tvMinGoal.text = ""
        holder.tvMaxGoal.text = ""

        when (item) {

            is Category -> {
                holder.tvName.text = item.categoryName
                holder.ivIcon.text = item.categoryIcon
            }

            is SubCategory -> {
                holder.tvName.text = item.subCategoryName
                holder.ivIcon.text = item.subCategoryIcon
            }

            is CategoryGoal -> {
                holder.tvMinGoal.text = "R ${item.minGoal ?: "—"}"
                holder.tvMaxGoal.text = "R ${item.maxGoal ?: "—"}"
            }

            is SubCategoryGoal -> {
                holder.tvMinGoal.text = "R ${item.minGoal ?: "—"}"
                holder.tvMaxGoal.text = "R ${item.maxGoal ?: "—"}"
            }

            is Triple<*, *, *> -> {
                val catOrSub = item.first
                val goal = item.second
                val expenses = item.third as? List<Expense> ?: emptyList()

                // 1. Set Name and Icon (Unified)
                if (catOrSub is Category) {
                    holder.tvName.text = catOrSub.categoryName
                    holder.ivIcon.text = catOrSub.categoryIcon
                    // Only show breakdown for main categories, and only if allowed
                    holder.llGoalSection.visibility = if (showBreakdownButton) View.VISIBLE else View.GONE
                } else if (catOrSub is SubCategory) {
                    holder.tvName.text = catOrSub.subCategoryName
                    holder.ivIcon.text = catOrSub.subCategoryIcon
                    holder.llGoalSection.visibility = View.GONE // Subcategories don't break down further
                }

                // Set Total Spent
                val total = expenses.sumOf { it.expenseAmount }
                holder.tvTotalSpent.text = "R $total"

                //  Handle Goals and Progress Bar (Unified for CategoryGoal and SubCategoryGoal)
                // treat both goal types the same way to ensure the UI matches
                val min: Double
                val max: Double

                when (goal) {
                    is CategoryGoal -> {
                        min = goal.minGoal ?: 0.0
                        max = goal.maxGoal ?: 0.0
                    }
                    is SubCategoryGoal -> {
                        min = goal.minGoal ?: 0.0
                        max = goal.maxGoal ?: 0.0
                    }
                    else -> {
                        min = 0.0
                        max = 0.0
                    }
                }

                if (max > 0) {
                    holder.tvMinGoal.text = "Min: R $min"
                    holder.tvMaxGoal.text = "Max: R $max"

                    holder.progressBar.visibility = View.VISIBLE
                    holder.tvNoGoal.visibility = View.GONE

                    // Progress calculation: If total is less than min, progress is 0.
                    // If it's between min and max, calculate percentage.
                    val progress = if (max > min) {
                        (((total - min) / (max - min)) * 100).coerceIn(0.0, 100.0)
                    } else {
                        0.0
                    }
                    holder.progressBar.progress = progress.toInt()
                } else {
                    holder.progressBar.visibility = View.GONE
                    holder.tvNoGoal.visibility = View.VISIBLE
                }
            }
        }

        holder.llGoalSection.setOnClickListener {
            onItemClick?.invoke(item)
        }

        holder.root.setOnClickListener(null)

        holder.ivDeleteCategory.setOnClickListener {
            onDeleteClick(item)
        }

        holder.ivEditCategory.setOnClickListener {
            onEditClick(item)
        }

        return view
    }
}