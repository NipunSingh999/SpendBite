package com.example.spendbitepro

import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

interface SpendBiteRepository {
    fun getCurrentUserId(): String?
    fun logout()
    
    // Observers for Real-time synchronization
    fun observeTransactions(userId: String, callback: (List<Transaction>) -> Unit): Any?
    fun observeGroups(userId: String, callback: (List<Group>) -> Unit): Any?
    fun observeLedgerItems(userId: String, callback: (List<SharedLedgerItem>) -> Unit): Any?
    fun observeSettlements(userId: String, callback: (List<QuickSettlement>) -> Unit): Any?
    fun observeBudgetSettings(userId: String, callback: (BudgetSettings) -> Unit): Any?
    fun observeSubscriptions(userId: String, callback: (List<SubscriptionItem>) -> Unit): Any?

    // Add / Update / Delete mutations
    fun addTransaction(userId: String, transaction: Transaction, callback: (Boolean) -> Unit)
    fun updateTransaction(userId: String, transaction: Transaction, callback: (Boolean) -> Unit)
    fun deleteTransaction(userId: String, transactionId: String, callback: (Boolean) -> Unit)
    fun addGroup(userId: String, group: Group, callback: (Boolean) -> Unit)
    fun updateGroup(userId: String, group: Group, callback: (Boolean) -> Unit)
    fun deleteGroup(userId: String, groupId: String, callback: (Boolean) -> Unit)
    fun addLedgerItem(userId: String, item: SharedLedgerItem, callback: (Boolean) -> Unit)
    fun updateLedgerItem(userId: String, item: SharedLedgerItem, callback: (Boolean) -> Unit)
    fun deleteLedgerItem(userId: String, itemId: String, callback: (Boolean) -> Unit)
    fun clearLedgerHistory(userId: String, groupName: String?, callback: (Boolean) -> Unit)
    fun addSettlement(userId: String, settlement: QuickSettlement, callback: (Boolean) -> Unit)
    fun deleteSettlement(userId: String, settlementId: String, callback: (Boolean) -> Unit)
    fun updateBudgetSettings(userId: String, settings: BudgetSettings, callback: (Boolean) -> Unit)
    fun updateSubscription(userId: String, subscription: SubscriptionItem, callback: (Boolean) -> Unit)
    fun addSubscription(userId: String, subscription: SubscriptionItem, callback: (Boolean) -> Unit)
    fun getUserProfile(userId: String, callback: (UserProfile?) -> Unit)
    fun saveUserProfile(userId: String, profile: UserProfile, callback: (Boolean) -> Unit)

    // Seeding logic
    fun seedInitialData(userId: String, callback: (Boolean) -> Unit)
}

object RepositoryProvider {
    private var isDemoMode: Boolean = true
    private val demoRepository = DemoRepository()
    private val firestoreRepository = FirestoreRepository()

    fun setDemoMode(demo: Boolean) {
        isDemoMode = demo
    }

    fun getRepository(): SpendBiteRepository {
        // Auto-disable demo mode if a real Firebase user is already authenticated
        if (FirebaseManager.isInitialized && FirebaseManager.auth?.currentUser != null) {
            isDemoMode = false
        }

        return if (isDemoMode || !FirebaseManager.isInitialized) {
            demoRepository
        } else {
            firestoreRepository
        }
    }
}

// ==================== IN-MEMORY DEMO REPOSITORY ====================
class DemoRepository : SpendBiteRepository {
    private var activeUserId: String? = "demo_user"
    
    private val transactions = mutableListOf<Transaction>()
    private val groups = mutableListOf<Group>()
    private val ledgerItems = mutableListOf<SharedLedgerItem>()
    private val settlements = mutableListOf<QuickSettlement>()
    private var budgetSettings = BudgetSettings()
    private val subscriptions = mutableListOf<SubscriptionItem>()

    // Callbacks to simulate Firestore real-time onSnapshot listeners
    private val transactionListeners = mutableListOf<(List<Transaction>) -> Unit>()
    private val groupListeners = mutableListOf<(List<Group>) -> Unit>()
    private val ledgerListeners = mutableListOf<(List<SharedLedgerItem>) -> Unit>()
    private val settlementListeners = mutableListOf<(List<QuickSettlement>) -> Unit>()
    private val budgetListeners = mutableListOf<(BudgetSettings) -> Unit>()
    private val subscriptionListeners = mutableListOf<(List<SubscriptionItem>) -> Unit>()

    init {
        // Seed default demo data on creation
        seedInitialData("demo_user") {}
    }

    override fun getCurrentUserId(): String? = activeUserId

    override fun logout() {
        activeUserId = null
    }

    private fun notifyTransactions() = transactionListeners.forEach { it(transactions.toList()) }
    private fun notifyGroups() = groupListeners.forEach { it(groups.toList()) }
    private fun notifyLedgers() = ledgerListeners.forEach { it(ledgerItems.toList()) }
    private fun notifySettlements() = settlementListeners.forEach { it(settlements.toList()) }
    private fun notifyBudget() = budgetListeners.forEach { it(budgetSettings) }
    private fun notifySubscriptions() = subscriptionListeners.forEach { it(subscriptions.toList()) }

    override fun observeTransactions(userId: String, callback: (List<Transaction>) -> Unit): Any {
        transactionListeners.add(callback)
        callback(transactions.toList())
        return callback
    }

    override fun observeGroups(userId: String, callback: (List<Group>) -> Unit): Any {
        groupListeners.add(callback)
        callback(groups.toList())
        return callback
    }

    override fun observeLedgerItems(userId: String, callback: (List<SharedLedgerItem>) -> Unit): Any {
        ledgerListeners.add(callback)
        callback(ledgerItems.toList())
        return callback
    }

    override fun observeSettlements(userId: String, callback: (List<QuickSettlement>) -> Unit): Any {
        settlementListeners.add(callback)
        callback(settlements.toList())
        return callback
    }

    override fun observeBudgetSettings(userId: String, callback: (BudgetSettings) -> Unit): Any {
        budgetListeners.add(callback)
        callback(budgetSettings)
        return callback
    }

    override fun observeSubscriptions(userId: String, callback: (List<SubscriptionItem>) -> Unit): Any {
        subscriptionListeners.add(callback)
        callback(subscriptions.toList())
        return callback
    }

    override fun addTransaction(userId: String, transaction: Transaction, callback: (Boolean) -> Unit) {
        val existingIndex = transactions.indexOfFirst { it.id == transaction.id }
        if (existingIndex != -1) {
            transactions[existingIndex] = transaction
            notifyTransactions()
            callback(true)
            return
        }
        val newTx = if (transaction.id.isEmpty()) transaction.copy(id = "tx_${System.currentTimeMillis()}") else transaction
        transactions.add(0, newTx)
        notifyTransactions()
        callback(true)
    }

    override fun updateTransaction(userId: String, transaction: Transaction, callback: (Boolean) -> Unit) {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            notifyTransactions()
            callback(true)
        } else {
            callback(false)
        }
    }

    override fun deleteTransaction(userId: String, transactionId: String, callback: (Boolean) -> Unit) {
        val removed = transactions.removeIf { it.id == transactionId }
        notifyTransactions()
        callback(removed)
    }

    override fun addGroup(userId: String, group: Group, callback: (Boolean) -> Unit) {
        val newGroup = if (group.id.isEmpty()) group.copy(id = "group_${System.currentTimeMillis()}") else group
        groups.add(newGroup)
        notifyGroups()
        callback(true)
    }

    override fun updateGroup(userId: String, group: Group, callback: (Boolean) -> Unit) {
        val index = groups.indexOfFirst { it.id == group.id }
        if (index != -1) {
            groups[index] = group
            notifyGroups()
            callback(true)
        } else {
            callback(false)
        }
    }

    override fun deleteGroup(userId: String, groupId: String, callback: (Boolean) -> Unit) {
        val group = groups.firstOrNull { it.id == groupId }
        if (group != null) {
            groups.removeIf { it.id == groupId }
            ledgerItems.removeIf { it.groupName.equals(group.name, ignoreCase = true) }
            settlements.removeIf { group.members.contains(it.name) }
            notifyGroups()
            notifyLedgers()
            notifySettlements()
            callback(true)
        } else {
            callback(false)
        }
    }

    override fun addLedgerItem(userId: String, item: SharedLedgerItem, callback: (Boolean) -> Unit) {
        val newItem = if (item.id.isEmpty()) item.copy(id = "ledger_${System.currentTimeMillis()}") else item
        ledgerItems.add(newItem)
        notifyLedgers()
        
        // Offset quick settlements dynamically
        val group = groups.firstOrNull { it.name.equals(item.groupName, ignoreCase = true) }
        if (group != null) {
            val N = group.members.size
            if (N > 1) {
                if (item.splitStatus == "none") {
                    val targetName = if (item.title.startsWith("Quick Settle - ")) {
                        item.title.substringAfter("Quick Settle - ")
                    } else {
                        null
                    }
                    if (targetName != null) {
                        if (item.paidBy.equals("You", ignoreCase = true)) {
                            offsetSettlement(userId, targetName, item.amount)
                        } else {
                            offsetSettlement(userId, targetName, -item.amount)
                        }
                    }
                } else {
                    val perMemberShare = item.amount / N
                    if (item.paidBy.equals("You", ignoreCase = true)) {
                        group.members.forEach { member ->
                            if (!member.equals("You", ignoreCase = true)) {
                                offsetSettlement(userId, member, perMemberShare)
                            }
                        }
                    } else {
                        offsetSettlement(userId, item.paidBy, -perMemberShare)
                    }
                }
            }
        }
        
        recalculateGroupBalances()
        callback(true)
    }

    override fun updateLedgerItem(userId: String, item: SharedLedgerItem, callback: (Boolean) -> Unit) {
        val index = ledgerItems.indexOfFirst { it.id == item.id }
        if (index != -1) {
            ledgerItems[index] = item
            notifyLedgers()
            recalculateGroupBalances()
            callback(true)
        } else {
            callback(false)
        }
    }

    override fun deleteLedgerItem(userId: String, itemId: String, callback: (Boolean) -> Unit) {
        // When a bill is deleted, we should reverse its impact on group balances.
        // Recalculating group balances takes care of this by scanning all remaining ledger items!
        val index = ledgerItems.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = ledgerItems[index]
            ledgerItems.removeAt(index)
            notifyLedgers()
            
            // Reverse settlements offset
            val group = groups.firstOrNull { it.name.equals(item.groupName, ignoreCase = true) }
            if (group != null) {
                val N = group.members.size
                if (N > 1) {
                    if (item.splitStatus == "none") {
                        val targetName = if (item.title.startsWith("Quick Settle - ")) {
                            item.title.substringAfter("Quick Settle - ")
                        } else {
                            null
                        }
                        if (targetName != null) {
                            if (item.paidBy.equals("You", ignoreCase = true)) {
                                offsetSettlement(userId, targetName, -item.amount)
                            } else {
                                offsetSettlement(userId, targetName, item.amount)
                            }
                        }
                    } else {
                        val perMemberShare = item.amount / N
                        if (item.paidBy.equals("You", ignoreCase = true)) {
                            group.members.forEach { member ->
                                if (!member.equals("You", ignoreCase = true)) {
                                    offsetSettlement(userId, member, -perMemberShare)
                                }
                            }
                        } else {
                            offsetSettlement(userId, item.paidBy, perMemberShare)
                        }
                    }
                }
            }
            
            recalculateGroupBalances()
            callback(true)
        } else {
            callback(false)
        }
    }

    override fun clearLedgerHistory(userId: String, groupName: String?, callback: (Boolean) -> Unit) {
        if (groupName == null) {
            ledgerItems.clear()
            settlements.clear()
        } else {
            ledgerItems.removeAll { it.groupName.equals(groupName, ignoreCase = true) }
            val group = groups.firstOrNull { it.name.equals(groupName, ignoreCase = true) }
            if (group != null) {
                settlements.removeAll { group.members.contains(it.name) }
            }
        }
        notifyLedgers()
        notifySettlements()
        recalculateGroupBalances()
        callback(true)
    }

    override fun addSettlement(userId: String, settlement: QuickSettlement, callback: (Boolean) -> Unit) {
        val newSet = if (settlement.id.isEmpty()) settlement.copy(id = "set_${System.currentTimeMillis()}") else settlement
        settlements.add(newSet)
        notifySettlements()
        callback(true)
    }

    override fun deleteSettlement(userId: String, settlementId: String, callback: (Boolean) -> Unit) {
        val removed = settlements.removeIf { it.id == settlementId }
        notifySettlements()
        callback(removed)
    }

    override fun updateBudgetSettings(userId: String, settings: BudgetSettings, callback: (Boolean) -> Unit) {
        budgetSettings = settings
        notifyBudget()
        callback(true)
    }

    override fun updateSubscription(userId: String, subscription: SubscriptionItem, callback: (Boolean) -> Unit) {
        val index = subscriptions.indexOfFirst { it.id == subscription.id }
        if (index != -1) {
            subscriptions[index] = subscription
            notifySubscriptions()
            callback(true)
        } else {
            callback(false)
        }
    }

    override fun addSubscription(userId: String, subscription: SubscriptionItem, callback: (Boolean) -> Unit) {
        val newSub = if (subscription.id.isEmpty()) subscription.copy(id = "sub_${System.currentTimeMillis()}") else subscription
        subscriptions.add(newSub)
        notifySubscriptions()
        callback(true)
    }

    private fun offsetSettlement(userId: String, name: String, amount: Double) {
        val index = settlements.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (index != -1) {
            val current = settlements[index]
            var newNet = if (current.type == "owed") current.amount else -current.amount
            newNet += amount
            
            if (Math.abs(newNet) < 0.01) {
                settlements.removeAt(index)
            } else if (newNet > 0) {
                settlements[index] = current.copy(amount = newNet, type = "owed")
            } else {
                settlements[index] = current.copy(amount = -newNet, type = "owe")
            }
        } else {
            val type = if (amount > 0) "owed" else "owe"
            val initials = name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
            val colorsList = listOf("#6366F1", "#10B981", "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6")
            val bg = colorsList[Math.abs(name.hashCode()) % colorsList.size]
            val newSet = QuickSettlement(
                id = "settle_${System.currentTimeMillis()}_${name.hashCode()}",
                name = name,
                amount = Math.abs(amount),
                type = type,
                initials = initials,
                bgClass = bg
            )
            settlements.add(newSet)
        }
        notifySettlements()
    }

    private fun recalculateGroupBalances() {
        groups.forEachIndexed { idx, group ->
            val N = group.members.size
            if (N <= 1) return@forEachIndexed
            
            val groupItems = ledgerItems.filter { it.groupName.equals(group.name, ignoreCase = true) }
            var youPaidTotal = 0.0
            var othersPaidTotal = 0.0
            
            groupItems.forEach { item ->
                if (item.splitStatus == "none") {
                    if (item.paidBy.equals("You", ignoreCase = true)) {
                        youPaidTotal += item.amount
                    } else {
                        othersPaidTotal += item.amount
                    }
                } else {
                    if (item.paidBy.equals("You", ignoreCase = true)) {
                        youPaidTotal += item.amount * (N - 1) / N
                    } else {
                        othersPaidTotal += item.amount / N
                    }
                }
            }
            
            val netBalance = youPaidTotal - othersPaidTotal
            val updated = if (Math.abs(netBalance) < 0.01) {
                group.copy(statusText = "All settled", statusType = "settled")
            } else if (netBalance < 0.0) {
                group.copy(statusText = "You owe ₹${String.format("%.2f", -netBalance)}", statusType = "owe")
            } else {
                group.copy(statusText = "You are owed ₹${String.format("%.2f", netBalance)}", statusType = "owed")
            }
            groups[idx] = updated
        }
        notifyGroups()
    }

    override fun seedInitialData(userId: String, callback: (Boolean) -> Unit) {
        activeUserId = userId
        
        if (userId.equals("demo_user", ignoreCase = true)) {
            // Seed Subscriptions
            subscriptions.clear()
            subscriptions.add(SubscriptionItem("sub_zomato", "Zomato Gold", 149.0, false, "#EF4444", "truck"))
            subscriptions.add(SubscriptionItem("sub_swiggy", "Swiggy One", 129.0, false, "#F59E0B", "utensils"))

            // Seed Group
            groups.clear()
            groups.add(Group(
                id = "group_1",
                name = "Flat 402 roommates",
                categoryIcon = "home",
                statusText = "You owe Alex ₹150.00",
                statusType = "owe",
                membersCount = 3,
                description = "Shared apartment groceries and dining bills",
                members = listOf("You", "Alex M.", "Meera")
            ))

            // Seed Ledger
            ledgerItems.clear()
            ledgerItems.add(SharedLedgerItem("ledger_1", "Organic Fruits & Eggs", "Alex M.", 450.0, "split_shared", "Flat 402 roommates", null, "You owe ₹150", "error"))
            ledgerItems.add(SharedLedgerItem("ledger_2", "Friday Pizza Night", "You", 900.0, "split_shared", "Flat 402 roommates", null, "Owed ₹300", "success"))

            // Seed Quick Settlements
            settlements.clear()
            settlements.add(QuickSettlement("settle_1", "Alex M.", 150.0, "owe", "AM", "#F59E0B"))
            settlements.add(QuickSettlement("settle_2", "Meera", 300.0, "owed", "MR", "#10B981"))

            // Seed Budget Settings
            budgetSettings = BudgetSettings(15000.0, 5000.0, 4000.0, true)

            // Seed Transactions
            transactions.clear()
            transactions.add(Transaction("tx_1", "Zomato Delivery", "Meals", 350.0, "Today, 1:15 PM", "UPI", "SMS Parse Check", true, "Indiranagar", 12, "up"))
            transactions.add(Transaction("tx_2", "Nature's Basket", "Groceries", 1250.0, "Yesterday, 6:30 PM", "Credit Card", "Manual Entry", false, "Koramangala", 4, "stable"))
            transactions.add(Transaction("tx_3", "Starbucks Coffee", "Meals", 280.0, "2 days ago, 10:00 AM", "Cash", "Manual Entry", false, "MG Road", 8, "down"))
            transactions.add(Transaction("tx_4", "Electricity Bill", "Utilities", 1200.0, "3 days ago, 4:00 PM", "UPI", "Manual Entry", false, null, null, null))
        } else {
            // Real User in local/demo repository mode: initialize zeroed-out values
            subscriptions.clear()
            subscriptions.add(SubscriptionItem("sub_zomato", "Zomato Gold", 149.0, false, "#EF4444", "truck"))
            subscriptions.add(SubscriptionItem("sub_swiggy", "Swiggy One", 129.0, false, "#F59E0B", "utensils"))

            groups.clear()
            ledgerItems.clear()
            settlements.clear()
            transactions.clear()
            budgetSettings = BudgetSettings(0.0, 0.0, 0.0, false)
        }

        notifyTransactions()
        notifyGroups()
        notifyLedgers()
        notifySettlements()
        notifyBudget()
        notifySubscriptions()

        callback(true)
    }

    private var demoProfile = UserProfile("Alex", "")
    override fun getUserProfile(userId: String, callback: (UserProfile?) -> Unit) {
        callback(demoProfile)
    }
    override fun saveUserProfile(userId: String, profile: UserProfile, callback: (Boolean) -> Unit) {
        demoProfile = profile
        callback(true)
    }
}

// ==================== FIREBASE FIRESTORE REPOSITORY ====================
class FirestoreRepository : SpendBiteRepository {
    private val db get() = FirebaseManager.firestore
    private val auth get() = FirebaseManager.auth

    override fun getCurrentUserId(): String? = auth?.currentUser?.uid

    override fun logout() {
        auth?.signOut()
    }

    override fun observeTransactions(userId: String, callback: (List<Transaction>) -> Unit): ListenerRegistration? {
        val ref = db?.collection("users")?.document(userId)?.collection("transactions")
        return ref?.orderBy("timestamp", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "Observe transactions failed: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) } ?: emptyList()
                callback(list)
            }
    }

    override fun observeGroups(userId: String, callback: (List<Group>) -> Unit): ListenerRegistration? {
        val ref = db?.collection("users")?.document(userId)?.collection("groups")
        return ref?.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.mapNotNull { it.toObject(Group::class.java)?.copy(id = it.id) } ?: emptyList()
            callback(list)
        }
    }

    override fun observeLedgerItems(userId: String, callback: (List<SharedLedgerItem>) -> Unit): ListenerRegistration? {
        val ref = db?.collection("users")?.document(userId)?.collection("ledgerItems")
        return ref?.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.mapNotNull { it.toObject(SharedLedgerItem::class.java)?.copy(id = it.id) } ?: emptyList()
            callback(list)
        }
    }

    override fun observeSettlements(userId: String, callback: (List<QuickSettlement>) -> Unit): ListenerRegistration? {
        val ref = db?.collection("users")?.document(userId)?.collection("settlements")
        return ref?.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.mapNotNull { it.toObject(QuickSettlement::class.java)?.copy(id = it.id) } ?: emptyList()
            callback(list)
        }
    }

    override fun observeBudgetSettings(userId: String, callback: (BudgetSettings) -> Unit): ListenerRegistration? {
        val ref = db?.collection("users")?.document(userId)?.collection("budget")?.document("settings")
        return ref?.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val settings = snapshot?.toObject(BudgetSettings::class.java) ?: BudgetSettings()
            callback(settings)
        }
    }

    override fun observeSubscriptions(userId: String, callback: (List<SubscriptionItem>) -> Unit): ListenerRegistration? {
        val ref = db?.collection("users")?.document(userId)?.collection("subscriptions")
        return ref?.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.mapNotNull { it.toObject(SubscriptionItem::class.java)?.copy(id = it.id) } ?: emptyList()
            callback(list)
        }
    }

    override fun addTransaction(userId: String, transaction: Transaction, callback: (Boolean) -> Unit) {
        val ref = db?.collection("users")?.document(userId)?.collection("transactions")
        val docRef = if (transaction.id.isEmpty()) ref?.document() else ref?.document(transaction.id)
        val finalTx = transaction.copy(id = docRef?.id ?: "")
        docRef?.set(finalTx)
            ?.addOnSuccessListener { callback(true) }
            ?.addOnFailureListener { callback(false) }
    }

    override fun updateTransaction(userId: String, transaction: Transaction, callback: (Boolean) -> Unit) {
        if (transaction.id.isEmpty()) {
            callback(false)
            return
        }
        db?.collection("users")?.document(userId)?.collection("transactions")?.document(transaction.id)
            ?.set(transaction)
            ?.addOnSuccessListener { callback(true) }
            ?.addOnFailureListener { callback(false) }
    }

    override fun deleteTransaction(userId: String, transactionId: String, callback: (Boolean) -> Unit) {
        db?.collection("users")?.document(userId)?.collection("transactions")?.document(transactionId)
            ?.delete()
            ?.addOnSuccessListener { callback(true) }
            ?.addOnFailureListener { callback(false) }
    }

    override fun addGroup(userId: String, group: Group, callback: (Boolean) -> Unit) {
        val ref = db?.collection("users")?.document(userId)?.collection("groups")
        val docRef = if (group.id.isEmpty()) ref?.document() else ref?.document(group.id)
        val finalGroup = group.copy(id = docRef?.id ?: "")
        docRef?.set(finalGroup)
            ?.addOnSuccessListener { callback(true) }
            ?.addOnFailureListener { callback(false) }
    }

    override fun updateGroup(userId: String, group: Group, callback: (Boolean) -> Unit) {
        if (group.id.isEmpty()) {
            callback(false)
            return
        }
        db?.collection("users")?.document(userId)?.collection("groups")?.document(group.id)
            ?.set(group)
            ?.addOnSuccessListener { callback(true) }
            ?.addOnFailureListener { callback(false) }
    }

    override fun deleteGroup(userId: String, groupId: String, callback: (Boolean) -> Unit) {
        val userRef = db?.collection("users")?.document(userId) ?: return callback(false)
        val groupDocRef = userRef.collection("groups").document(groupId)
        
        groupDocRef.get().addOnSuccessListener { doc ->
            val group = doc.toObject(Group::class.java)
            if (group != null) {
                val batch = db?.batch() ?: return@addOnSuccessListener callback(false)
                batch.delete(groupDocRef)
                
                // Clear ledger items in Firestore for this group name
                userRef.collection("ledgerItems").get().addOnSuccessListener { ledgerSnap ->
                    ledgerSnap.documents.forEach { ledgerDoc ->
                        val item = ledgerDoc.toObject(SharedLedgerItem::class.java)
                        if (item != null && item.groupName.equals(group.name, ignoreCase = true)) {
                            batch.delete(ledgerDoc.reference)
                        }
                    }
                    
                    // Clear settlements in Firestore for group members
                    userRef.collection("settlements").get().addOnSuccessListener { settleSnap ->
                        settleSnap.documents.forEach { settleDoc ->
                            val set = settleDoc.toObject(QuickSettlement::class.java)
                            if (set != null && group.members.contains(set.name)) {
                                batch.delete(settleDoc.reference)
                            }
                        }
                        
                        batch.commit().addOnSuccessListener { callback(true) }
                            .addOnFailureListener { callback(false) }
                    }
                }
            } else {
                callback(false)
            }
        }.addOnFailureListener { callback(false) }
    }

    override fun addLedgerItem(userId: String, item: SharedLedgerItem, callback: (Boolean) -> Unit) {
        val ref = db?.collection("users")?.document(userId)?.collection("ledgerItems")
        val docRef = if (item.id.isEmpty()) ref?.document() else ref?.document(item.id)
        val finalItem = item.copy(id = docRef?.id ?: "")
        
        docRef?.set(finalItem)
            ?.addOnSuccessListener {
                val userRef = db?.collection("users")?.document(userId)
                userRef?.collection("groups")?.get()?.addOnSuccessListener { groupsSnap ->
                    val groupsList = groupsSnap.documents.mapNotNull { it.toObject(Group::class.java) }
                    val group = groupsList.firstOrNull { it.name.equals(item.groupName, ignoreCase = true) }
                    if (group != null) {
                        val N = group.members.size
                        if (N > 1) {
                            if (item.splitStatus == "none") {
                                val targetName = if (item.title.startsWith("Quick Settle - ")) {
                                    item.title.substringAfter("Quick Settle - ")
                                } else {
                                    null
                                }
                                if (targetName != null) {
                                    if (item.paidBy.equals("You", ignoreCase = true)) {
                                        offsetFirestoreSettlement(userId, targetName, item.amount)
                                    } else {
                                        offsetFirestoreSettlement(userId, targetName, -item.amount)
                                    }
                                }
                            } else {
                                val perMemberShare = item.amount / N
                                if (item.paidBy.equals("You", ignoreCase = true)) {
                                    group.members.forEach { member ->
                                        if (!member.equals("You", ignoreCase = true)) {
                                            offsetFirestoreSettlement(userId, member, perMemberShare)
                                        }
                                    }
                                } else {
                                    offsetFirestoreSettlement(userId, item.paidBy, -perMemberShare)
                                }
                            }
                        }
                    }
                    recalculateFirestoreBalances(userId)
                    callback(true)
                }
            }
            ?.addOnFailureListener { callback(false) }
    }

    override fun updateLedgerItem(userId: String, item: SharedLedgerItem, callback: (Boolean) -> Unit) {
        if (item.id.isEmpty()) {
            callback(false)
            return
        }
        db?.collection("users")?.document(userId)?.collection("ledgerItems")?.document(item.id)
            ?.set(item)
            ?.addOnSuccessListener {
                recalculateFirestoreBalances(userId)
                callback(true)
            }
            ?.addOnFailureListener { callback(false) }
    }

    override fun deleteLedgerItem(userId: String, itemId: String, callback: (Boolean) -> Unit) {
        val ledgerDocRef = db?.collection("users")?.document(userId)?.collection("ledgerItems")?.document(itemId) ?: return callback(false)
        
        // Fetch item first to reverse its settlement impact
        ledgerDocRef.get().addOnSuccessListener { doc ->
            val item = doc.toObject(SharedLedgerItem::class.java)
            if (item != null) {
                ledgerDocRef.delete().addOnSuccessListener {
                    val userRef = db?.collection("users")?.document(userId)
                    userRef?.collection("groups")?.get()?.addOnSuccessListener { groupsSnap ->
                        val groupsList = groupsSnap.documents.mapNotNull { it.toObject(Group::class.java) }
                        val group = groupsList.firstOrNull { it.name.equals(item.groupName, ignoreCase = true) }
                        if (group != null) {
                            val N = group.members.size
                            if (N > 1) {
                                if (item.splitStatus == "none") {
                                    val targetName = if (item.title.startsWith("Quick Settle - ")) {
                                        item.title.substringAfter("Quick Settle - ")
                                    } else {
                                        null
                                    }
                                    if (targetName != null) {
                                        if (item.paidBy.equals("You", ignoreCase = true)) {
                                            offsetFirestoreSettlement(userId, targetName, -item.amount)
                                        } else {
                                            offsetFirestoreSettlement(userId, targetName, item.amount)
                                        }
                                    }
                                } else {
                                    val perMemberShare = item.amount / N
                                    if (item.paidBy.equals("You", ignoreCase = true)) {
                                        group.members.forEach { member ->
                                            if (!member.equals("You", ignoreCase = true)) {
                                                offsetFirestoreSettlement(userId, member, -perMemberShare)
                                            }
                                        }
                                    } else {
                                        offsetFirestoreSettlement(userId, item.paidBy, perMemberShare)
                                    }
                                }
                            }
                        }
                        recalculateFirestoreBalances(userId)
                        callback(true)
                    }
                }.addOnFailureListener { callback(false) }
            } else {
                callback(false)
            }
        }.addOnFailureListener { callback(false) }
    }

    override fun clearLedgerHistory(userId: String, groupName: String?, callback: (Boolean) -> Unit) {
        val userRef = db?.collection("users")?.document(userId) ?: return callback(false)
        userRef.collection("ledgerItems").get().addOnSuccessListener { ledgerSnap ->
            val batch = db?.batch() ?: return@addOnSuccessListener callback(false)
            ledgerSnap.documents.forEach { doc ->
                val item = doc.toObject(SharedLedgerItem::class.java)
                if (item != null) {
                    if (groupName == null || item.groupName.equals(groupName, ignoreCase = true)) {
                        batch.delete(doc.reference)
                    }
                }
            }
            
            userRef.collection("settlements").get().addOnSuccessListener { settleSnap ->
                settleSnap.documents.forEach { doc ->
                    val set = doc.toObject(QuickSettlement::class.java)
                    if (set != null) {
                        if (groupName == null) {
                            batch.delete(doc.reference)
                        } else {
                            // Delete settlements belonging to this group
                            userRef.collection("groups").get().addOnSuccessListener { groupsSnap ->
                                val group = groupsSnap.documents.mapNotNull { it.toObject(Group::class.java) }
                                    .firstOrNull { it.name.equals(groupName, ignoreCase = true) }
                                if (group != null && group.members.contains(set.name)) {
                                    db?.collection("users")?.document(userId)?.collection("settlements")?.document(doc.id)?.delete()
                                }
                            }
                        }
                    }
                }
                
                batch.commit().addOnSuccessListener {
                    recalculateFirestoreBalances(userId)
                    callback(true)
                }.addOnFailureListener { callback(false) }
            }
        }.addOnFailureListener { callback(false) }
    }

    override fun addSettlement(userId: String, settlement: QuickSettlement, callback: (Boolean) -> Unit) {
        val ref = db?.collection("users")?.document(userId)?.collection("settlements")
        val docRef = if (settlement.id.isEmpty()) ref?.document() else ref?.document(settlement.id)
        val finalSet = settlement.copy(id = docRef?.id ?: "")
        docRef?.set(finalSet)
            ?.addOnSuccessListener { callback(true) }
            ?.addOnFailureListener { callback(false) }
    }

    override fun deleteSettlement(userId: String, settlementId: String, callback: (Boolean) -> Unit) {
        db?.collection("users")?.document(userId)?.collection("settlements")?.document(settlementId)
            ?.delete()
            ?.addOnSuccessListener { callback(true) }
            ?.addOnFailureListener { callback(false) }
    }

    override fun updateBudgetSettings(userId: String, settings: BudgetSettings, callback: (Boolean) -> Unit) {
        db?.collection("users")?.document(userId)?.collection("budget")?.document("settings")
            ?.set(settings)
            ?.addOnSuccessListener { callback(true) }
            ?.addOnFailureListener { callback(false) }
    }

    override fun updateSubscription(userId: String, subscription: SubscriptionItem, callback: (Boolean) -> Unit) {
        if (subscription.id.isEmpty()) {
            callback(false)
            return
        }
        db?.collection("users")?.document(userId)?.collection("subscriptions")?.document(subscription.id)
            ?.set(subscription)
            ?.addOnSuccessListener { callback(true) }
            ?.addOnFailureListener { callback(false) }
    }

    override fun addSubscription(userId: String, subscription: SubscriptionItem, callback: (Boolean) -> Unit) {
        val id = if (subscription.id.isEmpty()) "sub_${System.currentTimeMillis()}" else subscription.id
        val newSub = subscription.copy(id = id)
        db?.collection("users")?.document(userId)?.collection("subscriptions")?.document(id)
            ?.set(newSub)
            ?.addOnSuccessListener { callback(true) }
            ?.addOnFailureListener { callback(false) }
    }

    private fun offsetFirestoreSettlement(userId: String, name: String, amount: Double) {
        val ref = db?.collection("users")?.document(userId)?.collection("settlements") ?: return
        ref.whereEqualTo("name", name).get().addOnSuccessListener { snap ->
            if (!snap.isEmpty) {
                val doc = snap.documents[0]
                val current = doc.toObject(QuickSettlement::class.java) ?: return@addOnSuccessListener
                var newNet = if (current.type == "owed") current.amount else -current.amount
                newNet += amount
                
                if (Math.abs(newNet) < 0.01) {
                    ref.document(doc.id).delete()
                } else if (newNet > 0) {
                    ref.document(doc.id).set(current.copy(amount = newNet, type = "owed"))
                } else {
                    ref.document(doc.id).set(current.copy(amount = -newNet, type = "owe"))
                }
            } else {
                val type = if (amount > 0) "owed" else "owe"
                val initials = name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
                val colorsList = listOf("#6366F1", "#10B981", "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6")
                val bg = colorsList[Math.abs(name.hashCode()) % colorsList.size]
                val newSet = QuickSettlement(
                    id = "",
                    name = name,
                    amount = Math.abs(amount),
                    type = type,
                    initials = initials,
                    bgClass = bg
                )
                val docRef = ref.document()
                docRef.set(newSet.copy(id = docRef.id))
            }
        }
    }

    private fun recalculateFirestoreBalances(userId: String) {
        val userRef = db?.collection("users")?.document(userId) ?: return
        val groupsRef = userRef.collection("groups")
        val ledgerRef = userRef.collection("ledgerItems")

        groupsRef.get().addOnSuccessListener { groupsSnap ->
            if (groupsSnap.isEmpty) return@addOnSuccessListener
            
            ledgerRef.get().addOnSuccessListener { ledgerSnap ->
                val items = ledgerSnap.documents.mapNotNull { it.toObject(SharedLedgerItem::class.java) }
                
                groupsSnap.documents.forEach { groupDoc ->
                    val group = groupDoc.toObject(Group::class.java)?.copy(id = groupDoc.id) ?: return@forEach
                    val N = group.members.size
                    if (N <= 1) return@forEach
                    
                    val groupItems = items.filter { it.groupName.equals(group.name, ignoreCase = true) }
                    var youPaidTotal = 0.0
                    var othersPaidTotal = 0.0
                    
                    groupItems.forEach { item ->
                        if (item.splitStatus == "none") {
                            if (item.paidBy.equals("You", ignoreCase = true)) {
                                youPaidTotal += item.amount
                            } else {
                                othersPaidTotal += item.amount
                            }
                        } else {
                            if (item.paidBy.equals("You", ignoreCase = true)) {
                                youPaidTotal += item.amount * (N - 1) / N
                            } else {
                                othersPaidTotal += item.amount / N
                            }
                        }
                    }
                    
                    val netBalance = youPaidTotal - othersPaidTotal
                    val updatedGroup = if (Math.abs(netBalance) < 0.01) {
                        group.copy(statusText = "All settled", statusType = "settled")
                    } else if (netBalance < 0.0) {
                        group.copy(statusText = "You owe ₹${String.format("%.2f", -netBalance)}", statusType = "owe")
                    } else {
                        group.copy(statusText = "You are owed ₹${String.format("%.2f", netBalance)}", statusType = "owed")
                    }
                    
                    groupsRef.document(group.id).set(updatedGroup)
                }
            }
        }
    }

    override fun seedInitialData(userId: String, callback: (Boolean) -> Unit) {
        val userRef = db?.collection("users")?.document(userId) ?: return callback(false)

        val batch = db?.batch() ?: return callback(false)

        // 1. Seed Subscriptions (base inactive templates)
        val sub1 = SubscriptionItem("sub_zomato", "Zomato Gold", 149.0, false, "#EF4444", "truck")
        val sub2 = SubscriptionItem("sub_swiggy", "Swiggy One", 129.0, false, "#F59E0B", "utensils")
        batch.set(userRef.collection("subscriptions").document(sub1.id), sub1)
        batch.set(userRef.collection("subscriptions").document(sub2.id), sub2)

        // 2. Seed default budget settings template
        val settings = BudgetSettings(0.0, 0.0, 0.0, false)
        batch.set(userRef.collection("budget").document("settings"), settings)

        // No mock groups, ledger items, settlements, or personal transactions are written!
        
        batch.commit()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    override fun getUserProfile(userId: String, callback: (UserProfile?) -> Unit) {
        val userRef = db?.collection("users")?.document(userId) ?: return callback(null)
        userRef.collection("profile").document("metadata")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    callback(profile)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { callback(null) }
    }

    override fun saveUserProfile(userId: String, profile: UserProfile, callback: (Boolean) -> Unit) {
        val userRef = db?.collection("users")?.document(userId) ?: return callback(false)
        userRef.collection("profile").document("metadata")
            .set(profile)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}
