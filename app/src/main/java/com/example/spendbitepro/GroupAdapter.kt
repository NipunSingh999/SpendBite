package com.example.spendbitepro

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class GroupAdapter(
    private var groups: List<Group>,
    private val onGroupClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    fun updateList(newList: List<Group>) {
        groups = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position], onGroupClick)
    }

    override fun getItemCount(): Int = groups.size

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flIconBg: View = itemView.findViewById(R.id.fl_group_icon_bg)
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_group_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_group_name)
        private val tvDesc: TextView = itemView.findViewById(R.id.tv_group_desc)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_group_status)
        private val tvMembersCount: TextView = itemView.findViewById(R.id.tv_group_members_count)
        private val llMembersAvatars: LinearLayout = itemView.findViewById(R.id.ll_group_member_avatars)

        fun bind(group: Group, onGroupClick: (Group) -> Unit) {
            tvName.text = group.name
            tvDesc.text = group.description
            tvStatus.text = group.statusText
            tvMembersCount.text = "${group.membersCount} members"

            val context = itemView.context

            // Icon select
            val iconRes = when (group.categoryIcon.lowercase()) {
                "home" -> R.drawable.ic_home
                "plane" -> R.drawable.ic_plane
                "shopping" -> R.drawable.ic_shopping
                else -> R.drawable.ic_coffee
            }
            ivIcon.setImageResource(iconRes)

            // Status color coding (reconciled is Emerald, you owe is Amber, you are owed is Red)
            val colorRes = when (group.statusType.lowercase()) {
                "settled" -> R.color.emerald_success
                "owe" -> R.color.amber_caution
                "owed" -> R.color.red_error
                else -> R.color.text_zinc_500
            }
            tvStatus.setTextColor(ContextCompat.getColor(context, colorRes))

            // Overlapping initials avatars
            llMembersAvatars.removeAllViews()
            val membersList = group.members ?: listOf("You", "Alex M.", "Meera")
            val colorsList = listOf("#6366F1", "#10B981", "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6")

            val maxAvatars = 3
            val showMembers = if (membersList.size > maxAvatars + 1) {
                membersList.take(maxAvatars)
            } else {
                membersList
            }

            showMembers.forEachIndexed { i, member ->
                val initial = if (member.equals("You", ignoreCase = true)) "ME" else {
                    member.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
                }

                val avatarFrame = FrameLayout(context).apply {
                    val size = dpToPx(28, context)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        if (i > 0) {
                            setMargins(-dpToPx(8, context), 0, 0, 0)
                        } else {
                            setMargins(0, 0, 0, 0)
                        }
                    }
                    
                    val shape = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(colorsList[i % colorsList.size]))
                        setStroke(dpToPx(2, context), Color.WHITE)
                    }
                    background = shape
                }

                val initialsText = TextView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER
                    }
                    text = initial
                    setTextColor(Color.WHITE)
                    textSize = 9f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                avatarFrame.addView(initialsText)
                llMembersAvatars.addView(avatarFrame)
            }

            if (membersList.size > maxAvatars + 1) {
                val plusCount = membersList.size - maxAvatars
                val avatarFrame = FrameLayout(context).apply {
                    val size = dpToPx(28, context)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(-dpToPx(8, context), 0, 0, 0)
                    }
                    
                    val shape = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#E4E4E7"))
                        setStroke(dpToPx(2, context), Color.WHITE)
                    }
                    background = shape
                }

                val initialsText = TextView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER
                    }
                    text = "+$plusCount"
                    setTextColor(Color.parseColor("#454652"))
                    textSize = 9f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                avatarFrame.addView(initialsText)
                llMembersAvatars.addView(avatarFrame)
            }

            itemView.setOnClickListener { onGroupClick(group) }
        }

        private fun dpToPx(dp: Int, context: android.content.Context): Int {
            val density = context.resources.displayMetrics.density
            return (dp * density).toInt()
        }
    }
}
