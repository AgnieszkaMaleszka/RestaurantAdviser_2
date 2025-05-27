package com.example.restaurantadviser.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthException

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun register(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    val exception = task.exception
                    val message = when (exception) {
                        is FirebaseAuthUserCollisionException ->
                            "Ten adres e-mail jest już zarejestrowany."

                        is FirebaseAuthWeakPasswordException ->
                            "Hasło musi mieć co najmniej 6 znaków."

                        is FirebaseAuthInvalidCredentialsException -> {
                            if (exception.message?.contains("mail") == true)
                                "Nieprawidłowy adres e-mail."
                            else
                                "Nieprawidłowe dane logowania."
                        }

                        is FirebaseAuthInvalidUserException ->
                            "Nie znaleziono konta z tym adresem e-mail."

                        is FirebaseAuthException ->
                            "Błąd: ${exception.message}"

                        else ->
                            "Wystąpił nieznany błąd. Spróbuj ponownie."
                    }

                    callback(false, message)
                }
            }
    }



    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    val exception = task.exception
                    val message = when (exception) {
                        is FirebaseAuthInvalidUserException ->
                            "Nie znaleziono konta z tym adresem e-mail."

                        is FirebaseAuthInvalidCredentialsException ->
                            "Nieprawidłowe hasło."

                        is FirebaseAuthException ->
                            "Błąd: ${exception.message}"

                        else -> "Wystąpił nieznany błąd. Spróbuj ponownie."
                    }

                    callback(false, message)
                }
            }
    }


    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }
}
