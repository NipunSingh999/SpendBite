package com.example.spendbitepro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AllTransactionsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var rvAllList: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private var txListener: Any? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_all_transactions, container, false)
        rvAllList = view.findViewById(R.id.rv_all_transactions_list)
        rvAllList.layoutManager = LinearLayoutManager(context)

        adapter = TransactionAdapter(emptyList()) { transaction ->
            val bottomSheet = TransactionDetailBottomSheet.newInstance(transaction)
            bottomSheet.show(childFragmentManager, "TransactionDetailBottomSheet")
        }
        rvAllList.adapter = adapter

        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        txListener = repository.observeTransactions(userId) { list ->
            if (context != null) {
                adapter.updateList(list)
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (txListener as? com.google.firebase.firestore.ListenerRegistration)?.remove()
    }
}
