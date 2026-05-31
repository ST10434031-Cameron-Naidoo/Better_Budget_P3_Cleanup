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
    private val parentFirebaseId: String,
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

        val tvViewCatBreakdown: TextView = view.findViewById(R.id.llCategoryFullExtension)
        val progressBar: ProgressBar = view.findViewById(R.id.pbCategoryProgress)
        val tvNoGoal: TextView = view.findViewById(R.id.tvNoProgressBar)

        val ivDeleteCategory: ImageView = view.findViewById(R.id.ivDeleteCategory)
        val ivEditCategory: ImageView = view.findViewById(R.id.ivEditCategory)

        val root: View = view
    }

    private val filteredItems: List<T>
        get() = items.filter { item ->
            when (item) {
                is SubCategory ->
                    item.parentFirebaseId == parentFirebaseId

                is Triple<*, *, *> -> {
                    val first = item.first
                    when (first) {
                        is SubCategory -> first.parentFirebaseId == parentFirebaseId
                        is Category -> parentFirebaseId == "ROOT"
                        else -> false
                    }
                }

                is Category -> parentFirebaseId == "ROOT"

                else -> false
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

        // Reset UI
        holder.progressBar.visibility = View.GONE
        holder.tvNoGoal.visibility = View.VISIBLE
        holder.tvMinGoal.text = ""
        holder.tvMaxGoal.text = ""
        holder.tvTotalSpent.text = ""
        if(showBreakdownButton)
        {
            holder.tvViewCatBreakdown.visibility = View.VISIBLE
        }
        else
        {
            holder.tvViewCatBreakdown.visibility = View.VISIBLE
        }
        when (item) {

            is Category -> {
                holder.tvName.text = item.categoryName
                holder.ivIcon.text = item.categoryIcon
            }

            is SubCategory -> {
                holder.tvName.text = item.subCategoryName
                holder.ivIcon.text = item.subCategoryIcon
            }

            is Triple<*, *, *> -> {
                val catOrSub = item.first
                val goal = item.second
                val expenses = item.third as? List<Expense> ?: emptyList()

                // Name + Icon
                when (catOrSub) {
                    is Category -> {
                        holder.tvName.text = catOrSub.categoryName
                        holder.ivIcon.text = catOrSub.categoryIcon
                    }
                    is SubCategory -> {
                        holder.tvName.text = catOrSub.subCategoryName
                        holder.ivIcon.text = catOrSub.subCategoryIcon
                    }
                }

                // Total spent
                val total = expenses.sumOf { it.expenseAmount }
                holder.tvTotalSpent.text = "R $total"

                // Goal handling
                if (goal != null) {


                    val min = when (goal) {
                        is CategoryGoal -> goal.minGoal ?: 0.0
                        is SubCategoryGoal -> goal.minGoal ?: 0.0
                        else -> 0.0
                    }

                    val max = when (goal) {
                        is CategoryGoal -> goal.maxGoal ?: 0.0
                        is SubCategoryGoal -> goal.maxGoal ?: 0.0
                        else -> 0.0
                    }

                    holder.tvMinGoal.text = "Min: R $min"
                    holder.tvMaxGoal.text = "Max: R $max"

                    if (max > 0) {
                        holder.progressBar.visibility = View.VISIBLE
                        holder.tvNoGoal.visibility = View.GONE

                        val progress = if (max > min) {
                            (((total - min) / (max - min)) * 100).coerceIn(0.0, 100.0)
                        } else 0.0

                        holder.progressBar.progress = progress.toInt()
                    } else {
                        holder.progressBar.visibility = View.GONE
                        holder.tvNoGoal.visibility = View.VISIBLE
                    }

                }

            }
        }

        // Click listeners
        holder.tvViewCatBreakdown.setOnClickListener { onItemClick?.invoke(item) }
        holder.ivDeleteCategory.setOnClickListener { onDeleteClick(item) }
        holder.ivEditCategory.setOnClickListener { onEditClick(item) }

        return view
    }
}
