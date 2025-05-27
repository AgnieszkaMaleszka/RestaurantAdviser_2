package com.example.restaurantadviser.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
fun EmailAuthScreen(
    onLoggedIn: (String?) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "Logowanie e-mailem" else "Rejestracja",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail", color = MaterialTheme.colorScheme.onBackground) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło",color = MaterialTheme.colorScheme.onBackground) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                if (isLoginMode) {
                    viewModel.login(email, password) { success, message ->
                        isLoading = false
                        if (success) onLoggedIn(email)
                        else Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                } else {
                    viewModel.register(email, password) { success, message ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Zarejestrowano. Sprawdź maila.", Toast.LENGTH_LONG).show()
                            isLoginMode = true
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isLoginMode) "Zaloguj się" else "Zarejestruj się",
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(
                if (isLoginMode) "Nie masz konta? Zarejestruj się" else "Masz już konto? Zaloguj się",
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        TextButton(
            onClick = {
                viewModel.resetPassword(email) { success, message ->
                    if (success) Toast.makeText(context, "Wysłano link resetujący hasło", Toast.LENGTH_LONG).show()
                    else Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            enabled = email.isNotBlank()
        ) {
            Text(
                "Zapomniałeś hasła?",
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
