package com.example.spendbitepro

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider

class LoginFragment : Fragment() {

    private var isRegisterMode = false
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnAuthenticate: Button
    private lateinit var btnGoogleAuth: View
    private lateinit var btnForgotPassword: View
    private lateinit var tvFooterPrompt: TextView
    private lateinit var tvFooterAction: TextView
    private lateinit var btnTogglePassword: ImageView

    private var isPasswordVisible = false
    private var googleSignInClient: GoogleSignInClient? = null
    private val RC_SIGN_IN = 9001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        tvTitle = view.findViewById(R.id.tv_login_title)
        tvSubtitle = view.findViewById(R.id.tv_login_subtitle)
        etEmail = view.findViewById(R.id.et_email)
        etPassword = view.findViewById(R.id.et_password)
        tvErrorMessage = view.findViewById(R.id.tv_error_message)
        btnAuthenticate = view.findViewById(R.id.btn_authenticate)
        btnGoogleAuth = view.findViewById(R.id.btn_google_auth)
        btnForgotPassword = view.findViewById(R.id.btn_forgot_password)
        tvFooterPrompt = view.findViewById(R.id.tv_footer_prompt)
        tvFooterAction = view.findViewById(R.id.tv_footer_action)
        btnTogglePassword = view.findViewById(R.id.btn_toggle_password)

        // Set initial state
        updateUiState()

        // Initialize Google Sign In client dynamically
        setupGoogleSignIn()

        // Toggle Register / Login State
        tvFooterAction.setOnClickListener {
            isRegisterMode = !isRegisterMode
            updateUiState()
        }

        // Password visibility toggle
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(android.R.drawable.ic_secure)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        // Firebase Auth Click
        btnAuthenticate.setOnClickListener {
            performAuthentication()
        }



        // Google Sign In Action
        btnGoogleAuth.setOnClickListener {
            val client = googleSignInClient
            if (client != null) {
                btnGoogleAuth.isEnabled = false
                client.signOut().addOnCompleteListener {
                    client.revokeAccess().addOnCompleteListener {
                        btnGoogleAuth.isEnabled = true
                        val signInIntent = client.signInIntent
                        startActivityForResult(signInIntent, RC_SIGN_IN)
                    }
                }
            } else {
                Toast.makeText(context, "Google Sign-In is unavailable without config files.", Toast.LENGTH_LONG).show()
            }
        }

        // Forgot Password Action
        btnForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(context, "Please enter your email above to reset password.", Toast.LENGTH_LONG).show()
            } else {
                val auth = FirebaseManager.auth
                if (auth != null) {
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to send reset link: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Firebase not connected. Cannot reset password.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun setupGoogleSignIn() {
        val context = context ?: return
        val webClientIdResId = resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (webClientIdResId != 0) {
            val webClientId = getString(webClientIdResId)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignInClient = client
            
            // Clear cached account session so Google always shows the account chooser
            try {
                client.signOut()
                client.revokeAccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUiState() {
        tvErrorMessage.visibility = View.GONE
        if (isRegisterMode) {
            tvTitle.text = "Create Account"
            tvSubtitle.text = "Register to isolate your isolated ledger"
            btnAuthenticate.text = "Register & Onboard"
            
            tvFooterPrompt.text = "Already have an account? "
            tvFooterAction.text = "Sign In"
            btnForgotPassword.visibility = View.GONE
        } else {
            tvTitle.text = "Welcome Back"
            tvSubtitle.text = "Enter your credentials to access your dashboard"
            btnAuthenticate.text = "Access Account"
            
            tvFooterPrompt.text = "Don't have an account? "
            tvFooterAction.text = "Create Account"
            btnForgotPassword.visibility = View.VISIBLE
        }
    }

    private fun performAuthentication() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Fields cannot be empty")
            return
        }
        if (password.length < 6) {
            showError("Password must be at least 6 characters")
            return
        }

        tvErrorMessage.visibility = View.GONE
        btnAuthenticate.isEnabled = false

        // Check if Firebase is initialized
        if (!FirebaseManager.isInitialized) {
            showError("Firebase is not initialized. Using Demo Mode is recommended.")
            btnAuthenticate.isEnabled = true
            return
        }

        val auth = FirebaseManager.auth ?: run {
            showError("Firebase Auth unavailable. Please use Demo Mode.")
            btnAuthenticate.isEnabled = true
            return
        }

        RepositoryProvider.setDemoMode(false)

        if (isRegisterMode) {
            // Firebase Auth Register + Enforce Email Verification
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.sendEmailVerification()
                            ?.addOnCompleteListener { verifyTask ->
                                btnAuthenticate.isEnabled = true
                                if (verifyTask.isSuccessful) {
                                    Toast.makeText(context, "Verification email sent to $email. Please check your inbox and verify before logging in.", Toast.LENGTH_LONG).show()
                                    auth.signOut() // Sign out until verified
                                    isRegisterMode = false
                                    updateUiState()
                                } else {
                                    showError(verifyTask.exception?.message ?: "Failed to send verification email.")
                                }
                            }
                    } else {
                        btnAuthenticate.isEnabled = true
                        val exception = task.exception
                        val msg = exception?.message ?: "Registration failed"
                        showError(msg)
                    }
                }
        } else {
            // Firebase Auth Login + Enforce Verification check
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            if (user.isEmailVerified) {
                                btnAuthenticate.isEnabled = true
                                val userId = user.uid
                                checkBudgetAndNavigate(userId)
                            } else {
                                Toast.makeText(context, "Please verify your email address. A verification link was resent to ${user.email}.", Toast.LENGTH_LONG).show()
                                user.sendEmailVerification()
                                auth.signOut()
                                btnAuthenticate.isEnabled = true
                            }
                        }
                    } else {
                        btnAuthenticate.isEnabled = true
                        val exception = task.exception
                        val msg = exception?.message ?: "Sign in failed"
                        showError(msg)
                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                showError("Google sign in failed: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val auth = FirebaseManager.auth ?: return
        btnAuthenticate.isEnabled = false
        
        // Disable Demo Mode and switch to live Firebase Firestore repository
        RepositoryProvider.setDemoMode(false)

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                btnAuthenticate.isEnabled = true
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    checkBudgetAndNavigate(userId)
                } else {
                    showError(task.exception?.message ?: "Firebase authentication with Google failed.")
                }
            }
    }

    private fun checkBudgetAndNavigate(userId: String) {
        val repository = RepositoryProvider.getRepository()
        repository.observeBudgetSettings(userId) { budget ->
            val mainActivity = activity as? MainActivity
            mainActivity?.navigateTo("dashboard")
        }
    }

    private fun showError(message: String) {
        tvErrorMessage.text = message
        tvErrorMessage.visibility = View.VISIBLE
    }
}
