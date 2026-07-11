package com.example.spendbitepro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SpendingHistoryBottomSheet : BottomSheetDialogFragment() {

    private lateinit var vBarChart: MonthlyBarChartView
    private lateinit var rvMonths: RecyclerView
    private lateinit var monthAdapter: HistoryMonthAdapter
    private var txListener: Any? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_spending_history, container, false)
        vBarChart = view.findViewById(R.id.v_history_bar_chart)
        rvMonths = view.findViewById(R.id.rv_history_months_list)
        rvMonths.layoutManager = LinearLayoutManager(context)

        monthAdapter = HistoryMonthAdapter(emptyList(), emptyMap()) { transaction ->
            val detailSheet = TransactionDetailBottomSheet.newInstance(transaction)
            detailSheet.show(childFragmentManager, "TransactionDetailBottomSheet")
        }
        rvMonths.adapter = monthAdapter

        loadHistoricalData()
        return view
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun loadHistoricalData() {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        txListener = repository.observeTransactions(userId) { transactions ->
            if (context == null) return@observeTransactions

            // Group transactions by month-year (e.g. "July 2026", "June 2026")
            val formatKey = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val formatMonthShort = SimpleDateFormat("MMM", Locale.getDefault())
            
            val monthlyGroups = mutableMapOf<String, MutableList<Transaction>>()

            for (tx in transactions) {
                val date = parseTransactionTimestamp(tx.timestamp)
                val key = formatKey.format(date)
                if (!monthlyGroups.containsKey(key)) {
                    monthlyGroups[key] = mutableListOf()
                }
                monthlyGroups[key]?.add(tx)
            }

            // Sum up the last 6 months for chart values
            val last6MonthsData = mutableListOf<Pair<String, Double>>()
            val displayList = mutableListOf<MonthHeaderItem>()
            val monthlyTxsMap = mutableMapOf<String, List<Transaction>>()

            val cal = Calendar.getInstance()
            // We go 5 months back to current month
            for (i in 5 downTo 0) {
                val tempCal = Calendar.getInstance()
                tempCal.add(Calendar.MONTH, -i)
                val key = formatKey.format(tempCal.time)
                val shortLabel = formatMonthShort.format(tempCal.time)

                val list = monthlyGroups[key] ?: emptyList()
                val total = list.sumOf { it.amount }

                last6MonthsData.add(shortLabel to total)
                
                if (list.isNotEmpty() || i == 0) {
                    displayList.add(MonthHeaderItem(key, total))
                    monthlyTxsMap[key] = list
                }
            }

            // Update custom drawing chart
            vBarChart.setChartData(last6MonthsData)

            // Update month list adapter (reversed so newest month shows first in list)
            monthAdapter.updateData(displayList.reversed(), monthlyTxsMap)
        }
    }

    private fun parseTransactionTimestamp(timestamp: String): Date {
        val now = Date()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val sdfFull = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
        
        var datePartStr = when {
            timestamp.startsWith("Today,", ignoreCase = true) -> {
                val currentDayPrefix = SimpleDateFormat("MMM dd,", Locale.getDefault()).format(now)
                timestamp.replace("Today,", currentDayPrefix)
            }
            timestamp.startsWith("Yesterday,", ignoreCase = true) -> {
                val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
                val yesterdayPrefix = SimpleDateFormat("MMM dd,", Locale.getDefault()).format(yesterday)
                timestamp.replace("Yesterday,", yesterdayPrefix)
            }
            else -> timestamp
        }
        
        // Append current year to datePartStr if it doesn't already contain it
        if (!datePartStr.contains(currentYear.toString())) {
            val commaIndex = datePartStr.indexOf(",")
            if (commaIndex != -1) {
                datePartStr = datePartStr.substring(0, commaIndex) + ", " + currentYear + datePartStr.substring(commaIndex)
            } else {
                datePartStr = "$datePartStr, $currentYear"
            }
        }
        
        return try {
            sdfFull.parse(datePartStr) ?: now
        } catch (e: Exception) {
            now
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (txListener as? com.google.firebase.firestore.ListenerRegistration)?.remove()
    }

    data class MonthHeaderItem(
        val monthYear: String,
        val totalSpent: Double
    )

    inner class HistoryMonthAdapter(
        private var months: List<MonthHeaderItem>,
        private var transactionsMap: Map<String, List<Transaction>>,
        private val onTxClick: (Transaction) -> Unit
    ) : RecyclerView.Adapter<HistoryMonthAdapter.MonthViewHolder>() {

        private val expandedStates = mutableMapOf<String, Boolean>()

        fun updateData(newMonths: List<MonthHeaderItem>, newMap: Map<String, List<Transaction>>) {
            months = newMonths
            transactionsMap = newMap
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history_month, parent, false)
            return MonthViewHolder(v)
        }

        override fun getItemCount(): Int = months.size

        override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
            val item = months[position]
            holder.tvName.text = item.monthYear
            holder.tvTotal.text = String.format("₹%,.2f", item.totalSpent)

            val txList = transactionsMap[item.monthYear] ?: emptyList()
            val isExpanded = expandedStates[item.monthYear] ?: false

            holder.rlHeader.setOnClickListener {
                val nextState = !isExpanded
                expandedStates[item.monthYear] = nextState
                notifyItemChanged(position)
            }

            if (isExpanded && txList.isNotEmpty()) {
                holder.llContainer.visibility = View.VISIBLE
                holder.ivChevron.setImageResource(android.R.drawable.arrow_up_float)
                
                holder.rvTransactions.visibility = View.VISIBLE
                holder.rvTransactions.layoutManager = LinearLayoutManager(holder.itemView.context)
                holder.rvTransactions.adapter = TransactionAdapter(txList) { tx ->
                    onTxClick(tx)
                }
                holder.rvTransactions.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                    override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                        if (e.action == android.view.MotionEvent.ACTION_DOWN) {
                            rv.parent.requestDisallowInterceptTouchEvent(true)
                        }
                        return false
                    }
                    override fun onTouchEvent(rv: RecyclerView, e: android.view.MotionEvent) {}
                    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
                })
            } else {
                holder.llContainer.visibility = View.GONE
                holder.ivChevron.setImageResource(android.R.drawable.arrow_down_float)
            }
        }

        inner class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val rlHeader: View = itemView.findViewById(R.id.rl_month_header)
            val tvName: TextView = itemView.findViewById(R.id.tv_history_month_name)
            val tvTotal: TextView = itemView.findViewById(R.id.tv_history_month_total)
            val ivChevron: ImageView = itemView.findViewById(R.id.iv_history_month_chevron)
            val llContainer: LinearLayout = itemView.findViewById(R.id.ll_expanded_transactions_container)
            val rvTransactions: RecyclerView = itemView.findViewById(R.id.rv_month_transactions)
        }
    }
}
