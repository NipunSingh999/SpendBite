package com.example.spendbitepro

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private lateinit var tvEmail: TextView
    private lateinit var tvUid: TextView
    private lateinit var tvNickname: TextView
    
    private lateinit var cvProfilePhoto: View
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var ivEditNickname: ImageView

    private lateinit var etTotal: EditText
    
    private lateinit var sbMeals: SeekBar
    private lateinit var tvMealsValue: TextView
    
    private lateinit var sbGroceries: SeekBar
    private lateinit var tvGroceriesValue: TextView
    
    private lateinit var switchAlerts: SwitchCompat
    private lateinit var btnSave: Button

    // Class level image picker registration
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val context = context ?: return@registerForActivityResult
        if (uri != null) {
            val path = saveImageToInternalStorage(uri)
            if (path != null) {
                val sharedPref = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
                sharedPref.edit().putString("user_profile_photo", path).apply()
                displayProfilePhoto()
                uploadProfileToFirestore()
                Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvEmail = view.findViewById(R.id.tv_profile_email)
        tvUid = view.findViewById(R.id.tv_profile_uid)
        tvNickname = view.findViewById(R.id.tv_profile_nickname)
        
        cvProfilePhoto = view.findViewById(R.id.cv_profile_photo)
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo)
        ivEditNickname = view.findViewById(R.id.iv_edit_nickname)

        etTotal = view.findViewById(R.id.et_total_limit)
        
        sbMeals = view.findViewById(R.id.sb_meals_slider)
        tvMealsValue = view.findViewById(R.id.tv_meals_value)
        
        sbGroceries = view.findViewById(R.id.sb_groceries_slider)
        tvGroceriesValue = view.findViewById(R.id.tv_groceries_value)
        
        switchAlerts = view.findViewById(R.id.switch_breach_alerts)
        btnSave = view.findViewById(R.id.btn_save_budget)

        setupSeekBars()
        loadProfileAndSettings()

        btnSave.setOnClickListener {
            saveBudgetSettings()
        }

        // Circular Photo Pick
        cvProfilePhoto.setOnClickListener {
            showPhotoOptionsDialog()
        }

        // Nickname Edit Click
        ivEditNickname.setOnClickListener {
            showEditNicknameDialog()
        }

        // History Archive Click
        val cvHistoryArchive = view.findViewById<View>(R.id.cv_history_archive)
        cvHistoryArchive?.setOnClickListener {
            val historySheet = SpendingHistoryBottomSheet()
            historySheet.show(childFragmentManager, "SpendingHistoryBottomSheet")
        }

        // Hamburger Menu click
        val btnMenu = view.findViewById<View>(R.id.btn_menu)
        btnMenu?.setOnClickListener {
            val menuSheet = NavigationDrawerBottomSheet()
            menuSheet.show(childFragmentManager, "NavigationDrawerBottomSheet")
        }

        return view
    }

    private fun setupSeekBars() {
        sbMeals.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvMealsValue.text = "₹$progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbGroceries.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvGroceriesValue.text = "₹$progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etTotal.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newTotal = s.toString().toIntOrNull() ?: 2000
                val safeMax = if (newTotal > 0) newTotal else 2000
                sbMeals.max = safeMax
                sbGroceries.max = safeMax
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadProfileAndSettings() {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        // Set user details
        val email = if (userId == "demo_user") "gastronome@spendbite.pro" else (FirebaseManager.auth?.currentUser?.email ?: "user@spendbite.pro")
        tvEmail.text = email
        tvUid.text = "Account UID: $userId"

        val sharedPref = requireContext().getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
        val nickname = sharedPref.getString("user_nickname", null)
        if (!nickname.isNullOrEmpty()) {
            tvNickname.text = nickname
            tvNickname.visibility = View.VISIBLE
        } else {
            tvNickname.text = "Alex"
            tvNickname.visibility = View.VISIBLE
        }

        displayProfilePhoto()

        if (userId != "demo_user") {
            repository.getUserProfile(userId) { profile ->
                if (isAdded && profile != null) {
                    val editor = sharedPref.edit()
                    if (!profile.nickname.isEmpty() && profile.nickname != nickname) {
                        editor.putString("user_nickname", profile.nickname)
                        tvNickname.text = profile.nickname
                    }
                    if (!profile.profilePhotoBase64.isEmpty()) {
                        try {
                            val file = File(requireContext().filesDir, "profile_photo.jpg")
                            val bytes = android.util.Base64.decode(profile.profilePhotoBase64, android.util.Base64.DEFAULT)
                            file.writeBytes(bytes)
                            editor.putString("user_profile_photo", file.absolutePath)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        // Attempt to fetch Google photo if available and local profile is blank
                        val googlePhotoUrl = FirebaseManager.auth?.currentUser?.photoUrl?.toString()
                        if (!googlePhotoUrl.isNullOrEmpty()) {
                            downloadGoogleProfilePhoto(googlePhotoUrl)
                        }
                    }
                    editor.apply()
                    displayProfilePhoto()
                }
            }
        }

        // Observe Budget settings
        repository.observeBudgetSettings(userId) { settings ->
            if (isAdded) {
                if (!etTotal.hasFocus()) {
                    etTotal.setText(String.format("%.0f", settings.totalMonthlyLimit))
                }
                
                val totalLimitVal = settings.totalMonthlyLimit.toInt()
                val safeMax = if (totalLimitVal > 0) totalLimitVal else 2000
                sbMeals.max = safeMax
                sbGroceries.max = safeMax

                val mealsVal = settings.mealsLimit.toInt()
                sbMeals.progress = mealsVal
                tvMealsValue.text = "₹$mealsVal"
                
                val grocVal = settings.groceriesLimit.toInt()
                sbGroceries.progress = grocVal
                tvGroceriesValue.text = "₹$grocVal"
                
                switchAlerts.isChecked = settings.breachAlerts
            }
        }
    }

    override fun onResume() {
        super.onResume()
        displayProfilePhoto()
    }

    fun refreshAvatar() {
        if (isAdded) {
            displayProfilePhoto()
        }
    }

    private fun displayProfilePhoto() {
        val context = context ?: return
        val sharedPref = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
        val photoPath = sharedPref.getString("user_profile_photo", null)
        val ivTopAvatar = view?.findViewById<ImageView>(R.id.iv_top_avatar)

        if (!photoPath.isNullOrEmpty()) {
            val file = File(photoPath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    ivProfilePhoto.setImageBitmap(bitmap)
                    ivProfilePhoto.imageTintList = null
                    
                    ivTopAvatar?.setImageBitmap(bitmap)
                    ivTopAvatar?.imageTintList = null
                }
            }
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_profile)
            ivProfilePhoto.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.brand_primary)
            )
            
            ivTopAvatar?.setImageResource(R.drawable.ic_profile)
            ivTopAvatar?.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.brand_primary)
            )
        }
    }

    private fun showEditNicknameDialog() {
        val context = context ?: return
        val sharedPref = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
        val currentNickname = sharedPref.getString("user_nickname", "Alex") ?: "Alex"

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_input, null)
        val etInput = dialogView.findViewById<EditText>(R.id.et_dialog_input)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_input_positive)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_input_negative)

        etInput.setText(currentNickname)
        etInput.setSelection(currentNickname.length)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnSave.setOnClickListener {
            val newName = etInput.text.toString().trim()
            if (newName.isNotEmpty()) {
                sharedPref.edit().putString("user_nickname", newName).apply()
                tvNickname.text = newName
                tvNickname.visibility = View.VISIBLE
                uploadProfileToFirestore()
                Toast.makeText(context, "Nickname updated!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Nickname cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        val context = context ?: return null
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val rawBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            
            // Resize the bitmap to a max dimension of 200px (standard avatar size)
            val maxDimension = 200
            val width = rawBitmap.width
            val height = rawBitmap.height
            val (newWidth, newHeight) = if (width > height) {
                maxDimension to (height * maxDimension / width)
            } else {
                (width * maxDimension / height) to maxDimension
            }
            
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(rawBitmap, newWidth, newHeight, true)
            
            // Fix orientation
            val rotatedBitmap = try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                val fd = pfd?.fileDescriptor
                val exifInterface = if (fd != null) android.media.ExifInterface(fd) else null
                val orientation = exifInterface?.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                ) ?: android.media.ExifInterface.ORIENTATION_NORMAL
                pfd?.close()
                
                val degrees = when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
                if (degrees != 0) {
                    val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
                    android.graphics.Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrix, true)
                } else {
                    scaledBitmap
                }
            } catch (ex: Exception) {
                scaledBitmap
            }
            
            val file = File(context.filesDir, "profile_photo.jpg")
            val outputStream = FileOutputStream(file)
            rotatedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBudgetSettings() {
        val total = etTotal.text.toString().toDoubleOrNull() ?: 15000.0
        val meals = sbMeals.progress.toDouble()
        val groceries = sbGroceries.progress.toDouble()
        val alerts = switchAlerts.isChecked

        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        val updated = BudgetSettings(total, meals, groceries, alerts)

        btnSave.isEnabled = false
        repository.updateBudgetSettings(userId, updated) { success ->
            btnSave.isEnabled = true
            if (success) {
                Toast.makeText(context, "Budget settings updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fileToBase64(filePath: String): String {
        return try {
            val bytes = File(filePath).readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun uploadProfileToFirestore() {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: return
        if (userId == "demo_user") return

        val context = context ?: return
        val sharedPref = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
        val nickname = sharedPref.getString("user_nickname", "Alex") ?: "Alex"
        val photoPath = sharedPref.getString("user_profile_photo", null)
        val base64Str = if (!photoPath.isNullOrEmpty()) fileToBase64(photoPath) else ""

        repository.saveUserProfile(userId, UserProfile(nickname, base64Str)) { success ->
            // Silent background sync
        }
    }

    private fun showPhotoOptionsDialog() {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_photo, null)
        val btnUpload = dialogView.findViewById<Button>(R.id.btn_photo_upload)
        val btnRemove = dialogView.findViewById<Button>(R.id.btn_photo_remove)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_photo_cancel)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnUpload.setOnClickListener {
            pickImageLauncher.launch("image/*")
            dialog.dismiss()
        }

        btnRemove.setOnClickListener {
            removeProfilePhoto()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun removeProfilePhoto() {
        val context = context ?: return
        val sharedPref = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("user_profile_photo").apply()

        val file = File(context.filesDir, "profile_photo.jpg")
        if (file.exists()) {
            file.delete()
        }

        displayProfilePhoto()
        uploadProfileToFirestore()
        (activity as? MainActivity)?.notifyProfileChanged()
        Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
    }

    private fun downloadGoogleProfilePhoto(photoUrl: String) {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: return
        if (userId == "demo_user") return

        val context = context ?: return
        val sharedPref = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)

        // Only download if no custom profile photo is already set locally
        val currentPhoto = sharedPref.getString("user_profile_photo", null)
        if (!currentPhoto.isNullOrEmpty()) return

        Thread {
            try {
                val url = java.net.URL(photoUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.doInput = true
                connection.connect()

                val input = connection.inputStream
                val rawBitmap = BitmapFactory.decodeStream(input)
                input.close()
                connection.disconnect()

                if (rawBitmap != null) {
                    // Compress and scale to our standard avatar size (200px max)
                    val maxDimension = 200
                    val width = rawBitmap.width
                    val height = rawBitmap.height
                    val (newWidth, newHeight) = if (width > height) {
                        maxDimension to (height * maxDimension / width)
                    } else {
                        (width * maxDimension / height) to maxDimension
                    }
                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(rawBitmap, newWidth, newHeight, true)

                    val file = java.io.File(context.filesDir, "profile_photo.jpg")
                    val outputStream = java.io.FileOutputStream(file)
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                    outputStream.flush()
                    outputStream.close()

                    activity?.runOnUiThread {
                        sharedPref.edit().putString("user_profile_photo", file.absolutePath).apply()
                        displayProfilePhoto()
                        uploadProfileToFirestore()
                        (activity as? MainActivity)?.notifyProfileChanged()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
