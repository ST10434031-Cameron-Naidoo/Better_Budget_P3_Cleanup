package com.dachkaboiz.betterbudget_bestbudget.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dachkaboiz.betterbudget_bestbudget.R
import com.dachkaboiz.betterbudget_bestbudget.data.model.Category
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoalAdapter (
    private var items: List<Triple<CategoryGoal, Category, Double>>,
    private val onCardClick: (Int) -> Unit, // New listener for the whole card
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<GoalAdapter.ViewHolder>(){

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvGoalIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvGoalTitle)
        val tvCurrent: TextView = view.findViewById(R.id.tvCurrentAmount)
        val progressBar: ProgressBar = view.findViewById(R.id.goalProgressBar)
        val tvDate: TextView = view.findViewById(R.id.tvGoalDate)
        val tvMinGoal: TextView = view.findViewById(R.id.tvMinGoal)
        val tvTarget: TextView = view.findViewById(R.id.tvTargetAmount)
        val ivEdit: ImageView = view.findViewById(R.id.ivEditGoal)
        val ivDelete: ImageView = view.findViewById(R.id.ivDeleteGoal)





    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Use the activity_goal_item_card layout you provided in the XML
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_v2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val (goal, category, totalSpent) = items[position]

        // Text & Emoji
        holder.tvIcon.text = category.categoryIcon ?: "🎯"
        holder.tvTitle.text = category.categoryName
        holder.tvCurrent.text = "R %.2f".format(totalSpent)

        // Goal Limits
        val minGoal = goal.minGoal ?: 0.0
        val maxGoal = goal.maxGoal ?: 0.0

//        // Date Formatting
//        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
//        holder.tvDate.text = "Set on: ${dateFormat.format(Date(goal.goalDate))}"

        holder.tvMinGoal.text = "Min: R %.2f".format(minGoal)
        holder.tvTarget.text = "Max: R %.2f".format(maxGoal)

        // Progress Logic (Driven by Max Goal)
        if (maxGoal > 0) {
            holder.progressBar.max = 100
            val progressPercent = ((totalSpent / maxGoal) * 100).toInt()
            holder.progressBar.progress = progressPercent.coerceAtMost(100)
        } else {
            holder.progressBar.progress = 0
        }

        // 4. Click Listeners
        // Entire card click
        holder.itemView.setOnClickListener {
            onCardClick(category.categoryID)
        }

        // Icons
        holder.ivEdit.setOnClickListener {
            onEditClick(goal.categoryGoalID)
        }

        holder.ivDelete.setOnClickListener {
            onDeleteClick(goal.categoryGoalID)
        }


    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Triple<CategoryGoal, Category, Double>>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}