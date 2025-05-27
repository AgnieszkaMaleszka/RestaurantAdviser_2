import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import androidx.compose.ui.text.style.TextAlign
import com.example.restaurantadviser.AnimatedTopWavesBackground
import com.example.restaurantadviser.AnimatedWavesBackground
import com.facebook.login.LoginManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.layout.Row


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(navController: NavHostController) {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val name = user?.displayName ?: "Nieznany użytkownik"

    val facebookUid = user?.providerData?.firstOrNull { it.providerId == "facebook.com" }?.uid
    val photoUrl = facebookUid?.let {
        "https://graph.facebook.com/$it/picture?type=large"
    }
    var newName by remember { mutableStateOf(name) }
    var showSuccessToast by remember { mutableStateOf(false) }
    Log.d("PROFILE_PHOTO_URL", "URL: $photoUrl")
    var isEditing by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        AnimatedTopWavesBackground()
        AnimatedWavesBackground()

        Column(modifier = Modifier.fillMaxSize()
            .verticalScroll(scrollState)) {
            // TOP APP BAR
            CenterAlignedTopAppBar(
                title = { Text("Panel użytkownika", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Cofnij",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )

            // ZAWARTOŚĆ POD TOOLBAREM
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (photoUrl != null) {
                            SubcomposeAsyncImage(
                                model = photoUrl,
                                contentDescription = "Zdjęcie profilowe",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            ) {
                                val state = painter.state
                                if (state is AsyncImagePainter.State.Success) {
                                    SubcomposeAsyncImageContent()
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Domyślny avatar",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                            .background(Color.LightGray),
                                        tint = Color.DarkGray
                                    )
                                }
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Brak zdjęcia",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                tint = Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Twój profil", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Zalogowano jako:", fontSize = 14.sp, color = Color.Gray)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = newName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = {
                                if (isEditing) {
                                    newName = name
                                }
                                isEditing = !isEditing
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edytuj nazwę",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        if (isEditing) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Zmień imię i nazwisko:", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        "Nowe imię i nazwisko",
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Zapisz zmiany",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                            .setDisplayName(newName)
                                            .build()

                                        FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdates)
                                            ?.addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    showSuccessToast = true
                                                    isEditing = false // zamykamy edycję po zapisie
                                                    Log.d("PROFILE", "Nazwa użytkownika zaktualizowana")
                                                } else {
                                                    Toast.makeText(context, "Błąd podczas aktualizacji", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    }
                                    .padding(12.dp)
                            )

                            if (showSuccessToast) {
                                LaunchedEffect(Unit) {
                                    Toast.makeText(context, "Zapisano nowe imię i nazwisko", Toast.LENGTH_SHORT).show()
                                    showSuccessToast = false
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Wyloguj się",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    FirebaseAuth.getInstance().signOut()
                                    LoginManager.getInstance().logOut()
                                    Toast.makeText(context, "Wylogowano", Toast.LENGTH_SHORT).show()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                                .padding(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Ulubione restauracje",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    navController.navigate("favorites")
                                }
                                .padding(12.dp)
                        )

                    }
                }
            }
        }
    }
}

