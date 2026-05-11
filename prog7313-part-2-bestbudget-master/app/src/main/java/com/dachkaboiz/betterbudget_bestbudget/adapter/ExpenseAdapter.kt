package com.dachkaboiz.betterbudget_bestbudget.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter(
    private val onItemClick: (Expense) -> Unit,
    private val onItemLongClick: (Expense) -> Unit,
    private val onEditClick: (Expense) -> Unit = {},
    private val onDeleteClick: (Expense) -> Unit = {},
    private val categoryNameResolver: ((Int) -> String)? = null,
    private val subCategoryNameResolver: ((Int) -> String)? = null
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryIcon: TextView     = itemView.findViewById(R.id.tvExpenseCategoryIcon)
        val tvCategoryName: TextView     = itemView.findViewById(R.id.tvExpenseCategoryName)
        val tvSubCategoryName: TextView  = itemView.findViewById(R.id.tvExpenseSubCategoryName)
        val tvAmount: TextView           = itemView.findViewById(R.id.tvExpenseAmount)
        val tvDate: TextView             = itemView.findViewById(R.id.tvExpenseDate)
        val tvDescription: TextView      = itemView.findViewById(R.id.tvExpenseDescription)
        val ivExpensePhoto: ImageView    = itemView.findViewById(R.id.ivExpensePhoto)
        val ivEditExpense: ImageView     = itemView.findViewById(R.id.ivEditExpense)
        val ivDeleteExpense: ImageView   = itemView.findViewById(R.id.ivDeleteExpense)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_v2, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = getItem(position)

        // Category name and icon — resolver returns "emoji name"
        val resolved = categoryNameResolver?.invoke(expense.categoryID)
            ?: "💰 Category ${expense.categoryID}"
        val spaceIndex = resolved.indexOf(' ')
        if (spaceIndex != -1) {
            holder.tvCategoryIcon.text = resolved.substring(0, spaceIndex)
            holder.tvCategoryName.text = resolved.substring(spaceIndex + 1)
        } else {
            holder.tvCategoryIcon.text = "💰"
            holder.tvCategoryName.text = resolved
        }

        // Subcategory icon and name
        // Subcategory
        if (expense.subCategoryID != null) {
            holder.tvSubCategoryName.visibility = View.VISIBLE
            holder.tvSubCategoryName.text = subCategoryNameResolver?.invoke(expense.subCategoryID)
                ?: "Subcategory ${expense.subCategoryID}"
        } else {
            holder.tvSubCategoryName.visibility = View.GONE
        }

        // Subcategory
        if (expense.subCategoryID != null) {
            holder.tvSubCategoryName.visibility = View.VISIBLE
            holder.tvSubCategoryName.text = subCategoryNameResolver?.invoke(expense.subCategoryID)
                ?: "Subcategory ${expense.subCategoryID}"
        } else {
            holder.tvSubCategoryName.visibility = View.GONE
        }

        // Amount
        holder.tvAmount.text = "R %.2f".format(expense.expenseAmount)

        // Date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(Date(expense.expenseDate))

        // Description
        if (expense.expenseDescription.isNullOrBlank()) {
            holder.tvDescription.visibility = View.GONE
        } else {
            holder.tvDescription.visibility = View.VISIBLE
            holder.tvDescription.text = expense.expenseDescription
        }

        // Photo
//        if (!expense.imageUri.isNullOrEmpty()) {
//            holder.ivExpensePhoto.visibility = View.VISIBLE
//            holder.ivExpensePhoto.setImageURI(Uri.parse(expense.imageUri))
//        } else {
//            holder.ivExpensePhoto.visibility = View.GONE
//        }

        if (!expense.imageUri.isNullOrEmpty()) {
            holder.ivExpensePhoto.visibility = View.VISIBLE
            try {
                val uri = Uri.parse(expense.imageUri)
                holder.ivExpensePhoto.setImageURI(uri)
            } catch (e: SecurityException) {
                // This is what stops the FATAL EXCEPTION
                holder.ivExpensePhoto.setImageResource(android.R.drawable.stat_notify_error) // Use any error icon
                // Optional: Log it so you know which items are failing
            }
        } else {
            holder.ivExpensePhoto.visibility = View.GONE
        }

        // Click handlers
        holder.itemView.setOnClickListener { onItemClick(expense) }
        holder.itemView.setOnLongClickListener { onItemLongClick(expense); true }
        holder.ivEditExpense.setOnClickListener { onEditClick(expense) }
        holder.ivDeleteExpense.setOnClickListener { onDeleteClick(expense) }
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense) =
            oldItem.expenseID == newItem.expenseID
        override fun areContentsTheSame(oldItem: Expense, newItem: Expense) =
            oldItem == newItem
    }
}