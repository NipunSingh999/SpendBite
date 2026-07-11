package com.example.spendbitepro

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import android.widget.RelativeLayout
import android.widget.ImageView
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class SplitBiteFragment : Fragment() {

    // View Containers
    private lateinit var llGroupsDashboard: View
    private lateinit var llGroupDetail: View
    private lateinit var tvHeaderTitle: TextView

    // Overall Balance Summary
    private lateinit var tvTotalSettlement: TextView
    private lateinit var tvSettlementCaption: TextView
    private lateinit var tvOverallYouOwe: TextView
    private lateinit var tvOverallOwedToYou: TextView

    // Dashboard widgets
    private lateinit var rvGroups: RecyclerView
    private lateinit var rvSettlements: RecyclerView
    private lateinit var btnCreateGroup: View

    // Shared Ledger widgets
    private lateinit var rvSharedLedger: RecyclerView
    private lateinit var btnViewLedgerDetails: View
    private lateinit var llActivityStreamContainer: LinearLayout

    // Detail widgets
    private lateinit var btnBackToGroups: View
    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailDesc: TextView
    private lateinit var llMembersAvatars: LinearLayout
    private lateinit var viewDebtColorBar: View
    private lateinit var tvDetailStatus: TextView
    private lateinit var btnAddSharedBill: Button
    private lateinit var rvLedgerItems: RecyclerView

    // Adapters
    private lateinit var groupAdapter: GroupAdapter
    private lateinit var settlementAdapter: SettlementAdapter
    private lateinit var ledgerAdapter: LedgerAdapter
    private lateinit var globalLedgerAdapter: LedgerAdapter

    // State
    private var activeGroup: Group? = null
    private var activeLedgerItems: List<SharedLedgerItem> = emptyList()
    private var activeGroupsList: List<Group> = emptyList()
    private var pendingSettlement: QuickSettlement? = null
    private val UPI_PAYMENT_REQUEST_CODE = 999

    private val colorsList = listOf("#10B981", "#F59E0B", "#EF4444", "#6366F1", "#EC4899", "#3B82F6")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_split_bite, container, false)

        llGroupsDashboard = view.findViewById(R.id.ll_groups_dashboard)
        llGroupDetail = view.findViewById(R.id.ll_group_detail)
        tvHeaderTitle = view.findViewById(R.id.tv_header_title)

        // Dashboard Summary bindings
        tvTotalSettlement = view.findViewById(R.id.tv_total_settlement)
        tvSettlementCaption = view.findViewById(R.id.tv_settlement_caption)
        tvOverallYouOwe = view.findViewById(R.id.tv_overall_you_owe)
        tvOverallOwedToYou = view.findViewById(R.id.tv_overall_owed_to_you)

        // Dashboard setup
        rvGroups = view.findViewById(R.id.rv_groups)
        rvSettlements = view.findViewById(R.id.rv_settlements)
        btnCreateGroup = view.findViewById(R.id.btn_create_group)

        // Shared Ledger bindings
        rvSharedLedger = view.findViewById(R.id.rv_shared_ledger)
        btnViewLedgerDetails = view.findViewById(R.id.btn_view_ledger_details)
        llActivityStreamContainer = view.findViewById(R.id.ll_activity_stream_container)

        // Detail setup
        btnBackToGroups = view.findViewById(R.id.btn_back_to_groups)
        tvDetailName = view.findViewById(R.id.tv_group_detail_name)
        tvDetailDesc = view.findViewById(R.id.tv_group_detail_desc)
        llMembersAvatars = view.findViewById(R.id.ll_members_avatars)
        viewDebtColorBar = view.findViewById(R.id.view_debt_color_bar)
        tvDetailStatus = view.findViewById(R.id.tv_group_detail_status)
        btnAddSharedBill = view.findViewById(R.id.btn_add_shared_bill)
        rvLedgerItems = view.findViewById(R.id.rv_ledger_items)

        setupRecyclerViews()

        // Button actions
        btnCreateGroup.setOnClickListener { showCreateGroupDialog() }
        btnBackToGroups.setOnClickListener { closeGroupDetail() }
        btnAddSharedBill.setOnClickListener { showAddSharedBillDialog() }

        // Shared Ledger header link toggle
        btnViewLedgerDetails.setOnClickListener {
            openGlobalLedger()
        }

        val btnClearHistory = view.findViewById<View>(R.id.btn_clear_ledger_history)
        btnClearHistory?.setOnClickListener {
            confirmClearLedgerHistory()
        }

        val btnDeleteGroup = view.findViewById<View>(R.id.btn_delete_group)
        btnDeleteGroup?.setOnClickListener {
            confirmDeleteGroup()
        }

        // Hamburger Menu click
        val btnMenu = view.findViewById<View>(R.id.btn_menu)
        btnMenu?.setOnClickListener {
            val menuSheet = NavigationDrawerBottomSheet()
            menuSheet.show(childFragmentManager, "NavigationDrawerBottomSheet")
        }

        // Circular Avatar Click
        val cvAvatar = view.findViewById<View>(R.id.cv_avatar)
        cvAvatar?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo("profile")
        }

        startObservers()

        return view
    }

    private fun setupRecyclerViews() {
        // Groups RV (Horizontal Carousel!)
        rvGroups.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        groupAdapter = GroupAdapter(emptyList()) { group ->
            openGroupDetail(group)
        }
        rvGroups.adapter = groupAdapter

        // Global Shared Ledger RV
        rvSharedLedger.layoutManager = LinearLayoutManager(context)
        globalLedgerAdapter = LedgerAdapter(emptyList()) { item ->
            confirmDeleteLedgerItem(item)
        }
        rvSharedLedger.adapter = globalLedgerAdapter

        // Settlements RV
        rvSettlements.layoutManager = LinearLayoutManager(context)
        settlementAdapter = SettlementAdapter(emptyList()) { settlement ->
            if (settlement.type == "owe") {
                launchDirectUPI(settlement)
            } else {
                showOwedSettlementOptionsDialog(settlement)
            }
        }
        rvSettlements.adapter = settlementAdapter

        // Detail Ledger Items RV
        rvLedgerItems.layoutManager = LinearLayoutManager(context)
        ledgerAdapter = LedgerAdapter(emptyList()) { item ->
            confirmDeleteLedgerItem(item)
        }
        rvLedgerItems.adapter = ledgerAdapter
    }

    private fun startObservers() {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        // Observe Groups
        repository.observeGroups(userId) { list ->
            if (isAdded) {
                activeGroupsList = list
                groupAdapter.updateList(list)
                val openGroup = activeGroup
                if (openGroup != null) {
                    val refreshed = list.firstOrNull { it.id == openGroup.id }
                    if (refreshed != null) {
                        updateGroupDetailUi(refreshed)
                    }
                }
                updateActivityStream()
            }
        }

        // Observe Settlements
        repository.observeSettlements(userId) { list ->
            if (isAdded) {
                settlementAdapter.updateList(list)
                recalculateOverallSettlements(list)
            }
        }

        // Observe Ledger Items
        repository.observeLedgerItems(userId) { list ->
            if (isAdded) {
                activeLedgerItems = list
                globalLedgerAdapter.updateList(list)
                refreshActiveLedgerItems()
                updateActivityStream()
            }
        }
    }

    private fun updateActivityStream() {
        if (!isAdded) return
        llActivityStreamContainer.removeAllViews()

        val activities = mutableListOf<Pair<String, Int>>()

        // 1. Group creation events
        activeGroupsList.forEach { group ->
            activities.add(Pair(
                "New circle '${group.name}' was created.",
                ContextCompat.getColor(requireContext(), R.color.brand_primary)
            ))
        }

        // 2. Bill addition events
        activeLedgerItems.forEach { item ->
            val color = if (item.paidBy.equals("You", ignoreCase = true)) {
                ContextCompat.getColor(requireContext(), R.color.brand_secondary)
            } else {
                ContextCompat.getColor(requireContext(), R.color.amber_caution)
            }
            activities.add(Pair(
                "${item.paidBy} added a bill for '${item.title}' of ₹${String.format("%,.0f", item.amount)} in ${item.groupName}.",
                color
            ))
        }

        if (activities.isEmpty()) {
            val emptyView = layoutInflater.inflate(R.layout.item_activity_stream, llActivityStreamContainer, false)
            val tvText = emptyView.findViewById<TextView>(R.id.tv_activity_text)
            val bullet = emptyView.findViewById<View>(R.id.view_activity_bullet)
            
            bullet.visibility = View.GONE
            tvText.text = "No recent activity. Create a circle or add a bill to get started!"
            tvText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_zinc_400))
            llActivityStreamContainer.addView(emptyView)
        } else {
            // Display latest activities first
            activities.reversed().forEach { act ->
                val rowView = layoutInflater.inflate(R.layout.item_activity_stream, llActivityStreamContainer, false)
                val tvText = rowView.findViewById<TextView>(R.id.tv_activity_text)
                val bullet = rowView.findViewById<View>(R.id.view_activity_bullet)

                bullet.backgroundTintList = ColorStateList.valueOf(act.second)
                tvText.text = act.first
                llActivityStreamContainer.addView(rowView)
            }
        }
    }

    private fun recalculateOverallSettlements(settlements: List<QuickSettlement>) {
        if (!isAdded) return
        var youOwe = 0.0
        var owedToYou = 0.0

        settlements.forEach {
            if (it.type == "owe") {
                youOwe += it.amount
            } else {
                owedToYou += it.amount
            }
        }

        val netSettlement = owedToYou - youOwe

        tvOverallYouOwe.text = "₹${String.format("%,.2f", youOwe)}"
        tvOverallOwedToYou.text = "₹${String.format("%,.2f", owedToYou)}"
        tvTotalSettlement.text = "₹${String.format("%,.2f", Math.abs(netSettlement))}"

        val context = requireContext()
        if (netSettlement > 0) {
            tvSettlementCaption.text = "You are owed by others"
            tvTotalSettlement.setTextColor(ContextCompat.getColor(context, R.color.brand_secondary))
        } else if (netSettlement < 0) {
            tvSettlementCaption.text = "You owe others"
            tvTotalSettlement.setTextColor(ContextCompat.getColor(context, R.color.red_error))
        } else {
            tvSettlementCaption.text = "All settled up"
            tvTotalSettlement.setTextColor(ContextCompat.getColor(context, R.color.text_zinc_500))
        }
    }

    private fun openGroupDetail(group: Group) {
        activeGroup = group
        updateGroupDetailUi(group)
        refreshActiveLedgerItems()

        btnAddSharedBill.visibility = View.VISIBLE
        llGroupsDashboard.visibility = View.GONE
        llGroupDetail.visibility = View.VISIBLE
        tvHeaderTitle.text = group.name
    }

    private fun openGlobalLedger() {
        activeGroup = null
        tvDetailName.text = "Shared Ledger"
        tvDetailDesc.text = "Global history across all circles"
        tvDetailStatus.text = "All settlements and bills combined"
        viewDebtColorBar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
        llMembersAvatars.removeAllViews()
        
        refreshActiveLedgerItems()

        btnAddSharedBill.visibility = View.GONE
        llGroupsDashboard.visibility = View.GONE
        llGroupDetail.visibility = View.VISIBLE
        tvHeaderTitle.text = "Shared Ledger"
    }

    private fun closeGroupDetail() {
        activeGroup = null
        llGroupsDashboard.visibility = View.VISIBLE
        llGroupDetail.visibility = View.GONE
        tvHeaderTitle.text = "Split-Bite Hub"
    }

    private fun updateGroupDetailUi(group: Group) {
        tvDetailName.text = group.name
        tvDetailDesc.text = group.description
        tvDetailStatus.text = group.statusText

        val context = requireContext()

        // Accent indicator
        val accentColor = when (group.statusType.lowercase()) {
            "settled" -> R.color.emerald_success
            "owe" -> R.color.amber_caution
            "owed" -> R.color.red_error
            else -> R.color.text_zinc_500
        }
        viewDebtColorBar.setBackgroundColor(ContextCompat.getColor(context, accentColor))

        // Create Circular Initials for members
        llMembersAvatars.removeAllViews()
        
        // Members header
        val header = TextView(context).apply {
            text = "Members:  "
            setTextColor(ContextCompat.getColor(context, R.color.text_zinc_500))
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        llMembersAvatars.addView(header)

        group.members.forEachIndexed { i, member ->
            val initial = if (member.equals("You", ignoreCase = true)) "ME" else {
                member.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
            }

            val avatarFrame = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(28.dpToPx(), 28.dpToPx()).apply {
                    setMargins(0, 0, 8.dpToPx(), 0)
                }
                
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(colorsList[i % colorsList.size]))
                }
                background = shape
            }

            val initialsText = TextView(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                }
                text = initial
                setTextColor(Color.WHITE)
                textSize = 10f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            avatarFrame.addView(initialsText)
            llMembersAvatars.addView(avatarFrame)
        }

        // Programmatic Manage Button
        val manageBtn = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 12.dpToPx()
            }
            text = "[Manage]"
            setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                showManageMembersDialog(group)
            }
        }
        llMembersAvatars.addView(manageBtn)
    }

    private fun refreshActiveLedgerItems() {
        val group = activeGroup
        if (group == null) {
            ledgerAdapter.updateList(activeLedgerItems)
        } else {
            val filtered = activeLedgerItems.filter { it.groupName.equals(group.name, ignoreCase = true) }
            ledgerAdapter.updateList(filtered)
        }
    }

    private fun showCreateGroupDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_group, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_group_dialog_name)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_group_dialog_desc)
        val etMembers = dialogView.findViewById<EditText>(R.id.et_group_dialog_members)
        val spCategory = dialogView.findViewById<Spinner>(R.id.sp_group_dialog_icon)

        // Setup Spinner
        val categories = listOf("Home", "Trip", "Office", "Dining")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btn_group_dialog_cancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btn_group_dialog_save).setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val membersStr = etMembers.text.toString().trim()
            val category = spBrandToIcon(spCategory.selectedItem.toString())

            if (name.isEmpty()) {
                Toast.makeText(context, "Group name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val membersList = mutableListOf("You")
            if (membersStr.isNotEmpty()) {
                membersStr.split(",").forEach {
                    val m = it.trim()
                    if (m.isNotEmpty() && !m.equals("You", ignoreCase = true)) {
                        membersList.add(m)
                    }
                }
            }

            val newGroup = Group(
                id = "",
                name = name,
                categoryIcon = category,
                statusText = "All settled",
                statusType = "settled",
                membersCount = membersList.size,
                description = desc,
                members = membersList
            )

            val repository = RepositoryProvider.getRepository()
            val userId = repository.getCurrentUserId() ?: "demo_user"

            repository.addGroup(userId, newGroup) { success ->
                if (success) {
                    Toast.makeText(context, "Group created!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Failed to create group", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun spBrandToIcon(selection: String): String = when (selection.lowercase()) {
        "home" -> "home"
        "trip" -> "plane"
        "office" -> "shopping"
        else -> "coffee"
    }

    private fun showAddSharedBillDialog() {
        val group = activeGroup ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_ledger_item, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_ledger_dialog_title)
        val etAmount = dialogView.findViewById<EditText>(R.id.et_ledger_dialog_amount)
        val spPaidBy = dialogView.findViewById<Spinner>(R.id.sp_ledger_dialog_payer)
        val switchSplit = dialogView.findViewById<SwitchCompat>(R.id.switch_ledger_dialog_receipt)

        // Populate PaidBy spinner with group members
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, group.members)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPaidBy.adapter = adapter

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btn_ledger_dialog_cancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btn_ledger_dialog_save).setOnClickListener {
            val title = etTitle.text.toString().trim()
            val amountStr = etAmount.text.toString().trim()
            val paidBy = spPaidBy.selectedItem.toString()
            val splitNow = switchSplit.isChecked

            if (title.isEmpty()) {
                Toast.makeText(context, "Bill title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val splitStatus = if (splitNow) "split_now" else "split_shared"
            val perMemberShare = amount / group.members.size

            val oweText: String
            val oweType: String

            if (paidBy.equals("You", ignoreCase = true)) {
                val othersShare = amount - perMemberShare
                oweText = "Owed ₹${String.format("%.2f", othersShare)}"
                oweType = "success"
            } else {
                oweText = "You owe ₹${String.format("%.2f", perMemberShare)}"
                oweType = "error"
            }

            val item = SharedLedgerItem(
                id = "",
                title = title,
                paidBy = paidBy,
                amount = amount,
                splitStatus = splitStatus,
                groupName = group.name,
                image = null,
                oweText = oweText,
                oweType = oweType
            )

            val repository = RepositoryProvider.getRepository()
            val userId = repository.getCurrentUserId() ?: "demo_user"

            repository.addLedgerItem(userId, item) { success ->
                if (success) {
                    Toast.makeText(context, "Bill added to ledger", Toast.LENGTH_SHORT).show()
                    
                    // If split now was checked, add quick settlement transaction(s)
                    if (splitNow) {
                        if (paidBy.equals("You", ignoreCase = true)) {
                            // If You paid, all other members owe you their individual share
                            group.members.forEach { member ->
                                if (!member.equals("You", ignoreCase = true)) {
                                    val settlement = QuickSettlement(
                                        id = "",
                                        name = member,
                                        amount = perMemberShare,
                                        type = "owed",
                                        initials = member.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase(),
                                        bgClass = colorsList[RandomInitialsBgColor()]
                                    )
                                    repository.addSettlement(userId, settlement) {}
                                }
                            }
                        } else {
                            // If someone else paid, You owe them your individual share
                            val settlement = QuickSettlement(
                                id = "",
                                name = paidBy,
                                amount = perMemberShare,
                                type = "owe",
                                initials = paidBy.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase(),
                                bgClass = colorsList[RandomInitialsBgColor()]
                            )
                            repository.addSettlement(userId, settlement) {}
                        }
                    }
                    
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Failed to add bill", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun RandomInitialsBgColor(): Int = (colorsList.indices).random()

    private fun performQuickSettle(settlement: QuickSettlement) {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"
        val matchingGroup = activeGroupsList.firstOrNull { it.members.contains(settlement.name) }
        val finalGroupName = activeGroup?.name ?: matchingGroup?.name ?: "Flat 402 roommates"

        val finalAmount = settlement.amount
        val name = settlement.name
        val splitStatus = "none"
        
        val oweText: String
        val oweType: String
        val paidBy: String
        
        if (settlement.type == "owe") {
            // You are paying them
            oweText = "Settled ₹${String.format("%.2f", finalAmount)}"
            oweType = "success"
            paidBy = "You"
        } else {
            // They are paying you
            oweText = "Settled ₹${String.format("%.2f", finalAmount)}"
            oweType = "success"
            paidBy = name
        }

        val item = SharedLedgerItem(
            id = "",
            title = "Quick Settle - $name",
            paidBy = paidBy,
            amount = finalAmount,
            splitStatus = splitStatus,
            groupName = finalGroupName,
            image = null,
            oweText = oweText,
            oweType = oweType
        )

        repository.addLedgerItem(userId, item) { success ->
            if (success) {
                Toast.makeText(context, "Settled $finalAmount with $name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun launchDirectUPI(settlement: QuickSettlement) {
        pendingSettlement = settlement
        // Construct a generic UPI URI with no pre-filled query parameters to open the app home screen
        val upiUri = android.net.Uri.parse("upi://pay")

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = upiUri
        }

        try {
            val chooser = android.content.Intent.createChooser(intent, "Pay via UPI App")
            startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(context, "No UPI apps found. Performing offline settlement.", Toast.LENGTH_LONG).show()
            performQuickSettle(settlement)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            val settlement = pendingSettlement
            if (settlement != null) {
                performQuickSettle(settlement)
                pendingSettlement = null
            }
        }
    }

    private fun showManageMembersDialog(group: Group) {
        val context = requireContext()
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        // Keep local mutable state for smooth modifications before saving
        val tempMembers = group.members.toMutableList()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = 16.dpToPx()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val listLabel = TextView(context).apply {
            text = "Current Members:"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.text_zinc_900))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
        }
        container.addView(listLabel)

        val rowsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(rowsContainer)

        fun refreshRows() {
            rowsContainer.removeAllViews()
            tempMembers.forEach { member ->
                val row = RelativeLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 4.dpToPx(), 0, 4.dpToPx())
                    }
                    val paddingAmount = 8.dpToPx()
                    setPadding(paddingAmount, paddingAmount, paddingAmount, paddingAmount)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#F9FAFB"))
                        cornerRadius = 8.dpToPx().toFloat()
                    }
                }

                val nameText = TextView(context).apply {
                    text = member
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.text_zinc_900))
                    layoutParams = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        addRule(RelativeLayout.ALIGN_PARENT_START)
                        addRule(RelativeLayout.CENTER_VERTICAL)
                    }
                }
                row.addView(nameText)

                if (!member.equals("You", ignoreCase = true)) {
                    val removeBtn = ImageView(context).apply {
                        setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                        imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_error))
                        layoutParams = RelativeLayout.LayoutParams(24.dpToPx(), 24.dpToPx()).apply {
                            addRule(RelativeLayout.ALIGN_PARENT_END)
                            addRule(RelativeLayout.CENTER_VERTICAL)
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#FEF2F2"))
                        }
                        val btnPad = 4.dpToPx()
                        setPadding(btnPad, btnPad, btnPad, btnPad)
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            tempMembers.remove(member)
                            refreshRows() // Instant UI update without DB writes
                        }
                    }
                    row.addView(removeBtn)
                }
                rowsContainer.addView(row)
            }
        }
        
        refreshRows()

        val separator = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.dpToPx()
            ).apply {
                setMargins(0, 16.dpToPx(), 0, 16.dpToPx())
            }
            setBackgroundColor(Color.parseColor("#E5E7EB"))
        }
        container.addView(separator)

        val addLabel = TextView(context).apply {
            text = "Add New Member:"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.text_zinc_900))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
        }
        container.addView(addLabel)

        val inputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val etNewMemberName = EditText(context).apply {
            hint = "e.g. Mike L."
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputRow.addView(etNewMemberName)

        val btnAdd = Button(context).apply {
            text = "Add"
            isAllCaps = false
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                40.dpToPx()
            ).apply {
                marginStart = 8.dpToPx()
            }
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(context, R.color.brand_primary))
                cornerRadius = 8.dpToPx().toFloat()
            }
            setTextColor(Color.WHITE)
            setOnClickListener {
                val name = etNewMemberName.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (tempMembers.any { it.equals(name, ignoreCase = true) }) {
                        Toast.makeText(context, "$name is already a member", Toast.LENGTH_SHORT).show()
                    } else {
                        tempMembers.add(name)
                        etNewMemberName.text?.clear()
                        refreshRows() // Instant UI update without DB writes
                    }
                }
            }
        }
        inputRow.addView(btnAdd)
        container.addView(inputRow)

        AlertDialog.Builder(context)
            .setTitle("Manage Circle Members")
            .setView(container)
            .setPositiveButton("Save") { dialog, _ ->
                val updatedGroup = group.copy(members = tempMembers)
                repository.updateGroup(userId, updatedGroup) { success ->
                    if (success) {
                        activeGroup = updatedGroup
                        updateGroupDetailUi(updatedGroup)
                        Toast.makeText(context, "Group members updated!", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        displayTopAvatar()
    }

    fun refreshAvatar() {
        if (isAdded) {
            displayTopAvatar()
        }
    }

    private fun displayTopAvatar() {
        val view = view ?: return
        val ivTopAvatar = view.findViewById<ImageView>(R.id.iv_top_avatar) ?: return
        val context = context ?: return
        val sharedPref = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
        val photoPath = sharedPref.getString("user_profile_photo", null)
        if (!photoPath.isNullOrEmpty()) {
            val file = java.io.File(photoPath)
            if (file.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    ivTopAvatar.setImageBitmap(bitmap)
                    ivTopAvatar.imageTintList = null
                }
            }
        } else {
            ivTopAvatar.setImageResource(R.drawable.ic_profile)
            ivTopAvatar.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.brand_primary)
            )
        }
    }

    private fun confirmDeleteLedgerItem(item: SharedLedgerItem) {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_confirm, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvMsg = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
        val btnPos = dialogView.findViewById<Button>(R.id.btn_dialog_positive)
        val btnNeg = dialogView.findViewById<Button>(R.id.btn_dialog_negative)

        tvTitle.text = "Delete Bill"
        tvMsg.text = "Are you sure you want to delete \"${item.title}\" from the ledger? This will reverse its settlement impact."
        btnPos.text = "Delete"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnPos.setOnClickListener {
            val repository = RepositoryProvider.getRepository()
            val userId = repository.getCurrentUserId() ?: "demo_user"
            repository.deleteLedgerItem(userId, item.id) { success ->
                if (success) {
                    Toast.makeText(context, "Bill deleted successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Failed to delete bill", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnNeg.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun confirmClearLedgerHistory() {
        val context = context ?: return
        val currentGroup = activeGroup
        val message = if (currentGroup == null) {
            "Are you sure you want to clear the entire global ledger history? This will delete all bills and reset all settlements to zero."
        } else {
            "Are you sure you want to clear the history for circle \"${currentGroup.name}\"? This will delete all bills in this circle and reset member settlements."
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_confirm, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvMsg = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
        val btnPos = dialogView.findViewById<Button>(R.id.btn_dialog_positive)
        val btnNeg = dialogView.findViewById<Button>(R.id.btn_dialog_negative)

        tvTitle.text = "Clear History"
        tvMsg.text = message
        btnPos.text = "Clear"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnPos.setOnClickListener {
            val repository = RepositoryProvider.getRepository()
            val userId = repository.getCurrentUserId() ?: "demo_user"
            val groupName = currentGroup?.name
            repository.clearLedgerHistory(userId, groupName) { success ->
                if (success) {
                    Toast.makeText(context, "History cleared successfully", Toast.LENGTH_SHORT).show()
                    if (currentGroup == null) {
                        closeGroupDetail()
                    } else {
                        openGroupDetail(currentGroup)
                    }
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Failed to clear history", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnNeg.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun confirmDeleteGroup() {
        val context = context ?: return
        val currentGroup = activeGroup ?: return

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_confirm, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvMsg = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
        val btnPos = dialogView.findViewById<Button>(R.id.btn_dialog_positive)
        val btnNeg = dialogView.findViewById<Button>(R.id.btn_dialog_negative)

        tvTitle.text = "Delete Circle"
        tvMsg.text = "Are you sure you want to delete circle \"${currentGroup.name}\"? This will delete the circle itself, all its shared bills, and reset all its settlements."
        btnPos.text = "Delete"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnPos.setOnClickListener {
            val repository = RepositoryProvider.getRepository()
            val userId = repository.getCurrentUserId() ?: "demo_user"
            repository.deleteGroup(userId, currentGroup.id) { success ->
                if (success) {
                    Toast.makeText(context, "Circle deleted successfully", Toast.LENGTH_SHORT).show()
                    closeGroupDetail()
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Failed to delete circle", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnNeg.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showOwedSettlementOptionsDialog(settlement: QuickSettlement) {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_settlement, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_settle_title)
        val tvMsg = dialogView.findViewById<TextView>(R.id.tv_settle_message)
        val btnRemind = dialogView.findViewById<Button>(R.id.btn_settle_remind)
        val btnMark = dialogView.findViewById<Button>(R.id.btn_settle_mark)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_settle_cancel)

        tvTitle.text = "Settle with ${settlement.name}"
        tvMsg.text = "Choose an action for the ₹${settlement.amount} owed to you:"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnRemind.setOnClickListener {
            Toast.makeText(context, "Payment reminder sent to ${settlement.name}!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnMark.setOnClickListener {
            performQuickSettle(settlement)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
