package com.example.restaurantadviser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.VisualTransformation
import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.restaurantadviser.auth.AuthViewModel
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.foundation.verticalScroll


private fun handleFacebookAccessToken(
    context: Context,
    token: AccessToken,
    onSuccess: (String?) -> Unit,
    onError: (String) -> Unit
) {
    val credential = FacebookAuthProvider.getCredential(token.token)
    FirebaseAuth.getInstance().signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = FirebaseAuth.getInstance().currentUser
                onSuccess(user?.displayName)
            } else {
                onError(task.exception?.message ?: "Błąd logowania przez Firebase.")
            }
        }
}

@Composable
fun FacebookLoginButton(
    callbackManager: CallbackManager,
    onLoginSuccess: (String?) -> Unit,
    onLoginError: (String) -> Unit,
    onLoadingStateChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    // Rejestruj callback tylko raz!
    LaunchedEffect(Unit) {
        com.facebook.login.LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(
                        context = context,
                        token = result.accessToken,
                        onSuccess = {
                            onLoadingStateChange(false)
                            onLoginSuccess(it)
                        },
                        onError = {
                            onLoadingStateChange(false)
                            onLoginError(it)
                        }
                    )
                }

                override fun onCancel() {
                    onLoadingStateChange(false)
                    onLoginError("Anulowano logowanie.")
                }

                override fun onError(error: FacebookException) {
                    onLoadingStateChange(false)
                    onLoginError("Błąd: ${error.localizedMessage}")
                }
            }
        )
    }

    Button(
        onClick = {
            onLoadingStateChange(true)
            com.facebook.login.LoginManager.getInstance().logInWithReadPermissions(
                activity,
                listOf("email", "public_profile")
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)) // Facebook blue
    ) {
        Text("Zaloguj się przez Facebook", color = Color.White)
    }
}

@Composable
fun GoogleLoginButton(
    onLoginSuccess: (String?) -> Unit,
    onLoginError: (String) -> Unit,
    onLoadingStateChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // WYMAGANE!
            .requestEmail()
            .build()
    }

    val googleSignInClient: GoogleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onLoadingStateChange(true)
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { authResult ->
                    onLoadingStateChange(false)
                    if (authResult.isSuccessful) {
                        val user = FirebaseAuth.getInstance().currentUser
                        onLoginSuccess(user?.displayName ?: user?.email)
                    } else {
                        onLoginError("Błąd logowania przez Google: ${authResult.exception?.message}")
                    }
                }
        } catch (e: Exception) {
            onLoadingStateChange(false)
            onLoginError("Nieudane logowanie przez Google: ${e.localizedMessage}")
        }
    }

    Button(
        onClick = {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB4437))
    ) {
        Text("Zaloguj się przez Google", color = Color.White)
    }
}

@Composable
fun LoginScreen(
    navController: NavHostController,
    onLoggedIn: (String?) -> Unit,
    callbackManager: CallbackManager,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {

        AnimatedTopWavesBackground()
        AnimatedWavesBackground()

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .padding(8.dp)
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    Text(
                        text = if (isRegisterMode) "Rejestracja" else "Logowanie",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        label = { Text("E-mail", color = MaterialTheme.colorScheme.onBackground) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                            cursorColor = MaterialTheme.colorScheme.onBackground
                        ),
                        isError = emailError != null
                    )

                    emailError?.let {
                        Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp).align(Alignment.Start))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    var passwordVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = { Text("Hasło", color = MaterialTheme.colorScheme.onBackground) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                            cursorColor = MaterialTheme.colorScheme.onBackground
                        ),
                        isError = passwordError != null,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    )

                    passwordError?.let {
                        Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp).align(Alignment.Start))
                    }

                    if (isRegisterMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        var confirmPasswordVisible by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                confirmPasswordError = null
                            },
                            label = { Text("Potwierdź hasło", color = MaterialTheme.colorScheme.onBackground) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = MaterialTheme.colorScheme.onBackground
                            ),
                            isError = confirmPasswordError != null,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        )

                        confirmPasswordError?.let {
                            Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp).align(Alignment.Start))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            emailError = null
                            passwordError = null
                            confirmPasswordError = null
                            errorMessage = null

                            if (isRegisterMode) {
                                if (email.isBlank()) {
                                    emailError = "E-mail nie może być pusty"
                                    isLoading = false
                                    return@Button
                                }
                                if (password.isBlank()) {
                                    passwordError = "Hasło nie może być puste"
                                    isLoading = false
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    confirmPasswordError = "Hasła się nie zgadzają"
                                    isLoading = false
                                    return@Button
                                }

                                viewModel.register(email, password) { success, message ->
                                    if (success) {
                                        val user = FirebaseAuth.getInstance().currentUser
                                        user?.sendEmailVerification()?.addOnCompleteListener { task ->
                                            isLoading = false
                                            if (task.isSuccessful) {
                                                Toast.makeText(context, "Zarejestrowano. Sprawdź e-mail weryfikacyjny!", Toast.LENGTH_LONG).show()
                                                email = ""
                                                password = ""
                                                confirmPassword = ""
                                                isRegisterMode = false
                                            } else {
                                                Toast.makeText(context, "Nie udało się wysłać maila weryfikacyjnego: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        when {
                                            message?.contains("email", true) == true -> emailError = message
                                            message?.contains("hasło", true) == true -> passwordError = message
                                            else -> Toast.makeText(context, message ?: "Wystąpił nieznany błąd", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                var hasError = false

                                if (email.isBlank()) {
                                    emailError = "E-mail nie może być pusty"
                                    hasError = true
                                }
                                if (password.isBlank()) {
                                    passwordError = "Hasło nie może być puste"
                                    hasError = true
                                }
                                if (hasError) {
                                    isLoading = false
                                    return@Button
                                }

                                viewModel.login(email, password) { success, message ->
                                    isLoading = false
                                    if (success) {
                                        val user = FirebaseAuth.getInstance().currentUser
                                        if (user?.isEmailVerified == true) {
                                            onLoggedIn(email)
                                        } else {
                                            emailError = "Najpierw zweryfikuj swój adres e-mail!"
                                            FirebaseAuth.getInstance().signOut()
                                        }
                                    } else {
                                        when {
                                            message?.contains("hasło", true) == true -> passwordError = message
                                            message?.contains("e-mail", true) == true || message?.contains("email", true) == true -> emailError = message
                                            else -> errorMessage = message
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onBackground, strokeWidth = 2.dp)
                        } else {
                            Text(if (isRegisterMode) "Zarejestruj się" else "Zaloguj się", fontWeight = FontWeight.Medium, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("lub", color = MaterialTheme.colorScheme.onBackground)

                    Spacer(modifier = Modifier.height(6.dp))

                    FacebookLoginButton(
                        callbackManager = callbackManager,
                        onLoginSuccess = {
                            Toast.makeText(context, "Zalogowano jako: $it", Toast.LENGTH_SHORT).show()
                            onLoggedIn(it)
                        },
                        onLoginError = {
                            errorMessage = it
                            isLoading = false
                        },
                        onLoadingStateChange = { isLoading = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    GoogleLoginButton(
                        onLoginSuccess = {
                            Toast.makeText(context, "Zalogowano jako: $it", Toast.LENGTH_SHORT).show()
                            onLoggedIn(it)
                        },
                        onLoginError = {
                            errorMessage = it
                            isLoading = false
                        },
                        onLoadingStateChange = { isLoading = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                        Text(
                            if (isRegisterMode) "Masz już konto? Zaloguj się"
                            else "Nie masz konta? Zarejestruj się",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }


                    errorMessage?.let {
                        Text(text = it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}
