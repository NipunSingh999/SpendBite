package com.example.spendbitepro

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NicknameBottomSheet : BottomSheetDialogFragment() {

    interface OnNicknameSavedListener {
        fun onNicknameSaved(nickname: String)
    }

    private var listener: OnNicknameSavedListener? = null

    fun setOnNicknameSavedListener(listener: OnNicknameSavedListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_nickname, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etNickname = view.findViewById<EditText>(R.id.et_nickname)
        val btnSave = view.findViewById<Button>(R.id.btn_nickname_save)
        val btnSkip = view.findViewById<Button>(R.id.btn_nickname_skip)

        btnSave.setOnClickListener {
            val name = etNickname.text.toString().trim()
            if (name.isNotEmpty()) {
                val sharedPref = requireContext().getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
                sharedPref.edit().putString("user_nickname", name).apply()
                
                Toast.makeText(context, "Welcome aboard, $name!", Toast.LENGTH_SHORT).show()
                listener?.onNicknameSaved(name)
                dismiss()
            } else {
                etNickname.error = "Please enter a nickname"
            }
        }

        btnSkip.setOnClickListener {
            dismiss()
        }
    }
}
