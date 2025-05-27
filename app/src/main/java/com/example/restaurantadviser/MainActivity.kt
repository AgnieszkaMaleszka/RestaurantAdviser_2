package com.example.restaurantadviser
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import UserScreen
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.restaurantadviser.repository.Restaurant
import com.example.restaurantadviser.ui.theme.RestaurantAdviserTheme
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.restaurantadviser.auth.EmailAuthScreen
import com.example.restaurantadviser.viewmodel.CommentViewModel
import com.example.restaurantadviser.viewmodel.FavoriteViewModel
import com.facebook.CallbackManager


class MainActivity : ComponentActivity() {
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(application)
        callbackManager = CallbackManager.Factory.create()

        // Inicjalizacja Google Places API
        if (!Places.isInitialized()) {
            try {
                Places.initialize(applicationContext, getString(R.string.google_maps_key))
                Log.d("PlacesInitialization", "Google Places API initialized successfully.")
            } catch (e: Exception) {
                Log.e("PlacesInitialization", "Error initializing Google Places API", e)
            }
        }

        setContent {
            RestaurantAdviserTheme(dynamicColor = false) {
                AppNavigator(callbackManager)
            }
        }

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}

@Composable
fun AnimatedWavesBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSecondaryColor = MaterialTheme.colorScheme.onSecondary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val waveHeight = 200f

        // Dolna fala (ciemniejsza)
        val backgroundPath = Path().apply {
            moveTo(0f, size.height * 0.8f)
            quadraticBezierTo(
                size.width * 0.5f,
                size.height,
                size.width,
                size.height * 0.8f
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(backgroundPath, color = onSecondaryColor) // ciemnoniebieski

        // Górna animowana fala (jaśniejsza)
        val wavePath = Path().apply {
            moveTo(0f, size.height - waveHeight + offsetY)
            quadraticBezierTo(
                size.width / 4f, size.height - waveHeight - 30f + offsetY,
                size.width / 2f, size.height - waveHeight + offsetY
            )
            quadraticBezierTo(
                3 * size.width / 4f, size.height - waveHeight + 30f + offsetY,
                size.width, size.height - waveHeight + offsetY
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(wavePath, color = secondaryColor) // jaśniejszy niebieski
    }
}

@Composable
fun AnimatedTopWavesBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "topWave")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val waveHeight = 200f

        // Tło górne (ciemniejszy czerwony)
        val backgroundPath = Path().apply {
            moveTo(0f, waveHeight)
            quadraticBezierTo(
                size.width / 2f,
                0.5f,
                size.width,
                waveHeight*2.5f
            )
            lineTo(size.width, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(backgroundPath, color = onPrimaryColor) // ciemnoczerwony

        // Górna animowana fala (jaśniejszy czerwony)
        val wavePath = Path().apply {
            moveTo(0f, waveHeight - offsetY)
            quadraticBezierTo(
                size.width / 4f, waveHeight + 30f - offsetY,
                size.width / 2f, waveHeight - offsetY
            )
            quadraticBezierTo(
                3 * size.width / 4f, waveHeight - 30f - offsetY,
                size.width, waveHeight - offsetY
            )
            lineTo(size.width, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(wavePath, color = primaryColor) // jaśniejszy czerwony
    }
}

@Composable
fun AnimatedWavesBackgroundHigh() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 70f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSecondaryColor = MaterialTheme.colorScheme.onSecondary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val waveHeight = 300f

        val backgroundPath = Path().apply {
            moveTo(0f, size.height * 0.75f)
            quadraticBezierTo(
                size.width * 0.5f,
                size.height,
                size.width,
                size.height * 0.75f
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(backgroundPath, color = onSecondaryColor)

        val wavePath = Path().apply {
            moveTo(0f, size.height - waveHeight + offsetY)
            quadraticBezierTo(
                size.width / 4f, size.height - waveHeight - 40f + offsetY,
                size.width / 2f, size.height - waveHeight + offsetY
            )
            quadraticBezierTo(
                3 * size.width / 4f, size.height - waveHeight + 40f + offsetY,
                size.width, size.height - waveHeight + offsetY
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(wavePath, color = secondaryColor)
    }
}

@Composable
fun AnimatedTopWavesBackgroundHigh() {
    val infiniteTransition = rememberInfiniteTransition(label = "topWave")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 70f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val waveHeight = 300f

        val backgroundPath = Path().apply {
            moveTo(0f, waveHeight)
            quadraticBezierTo(
                size.width / 2f,
                0f,
                size.width,
                waveHeight * 2.5f
            )
            lineTo(size.width, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(backgroundPath, color = onPrimaryColor)

        val wavePath = Path().apply {
            moveTo(0f, waveHeight - offsetY)
            quadraticBezierTo(
                size.width / 4f, waveHeight + 40f - offsetY,
                size.width / 2f, waveHeight - offsetY
            )
            quadraticBezierTo(
                3 * size.width / 4f, waveHeight - 40f - offsetY,
                size.width, waveHeight - offsetY
            )
            lineTo(size.width, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(wavePath, color = primaryColor)
    }
}

@Composable
fun AppNavigator(callbackManager: CallbackManager) {
    val navController = rememberNavController()
    var selectedRestaurants by rememberSaveable { mutableStateOf<List<Restaurant>>(emptyList()) }

    var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoggedIn = {
                    isLoggedIn = true
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                callbackManager = callbackManager // <- przekazane poprawnie
            )
        }

        composable("email_register") {
            EmailAuthScreen(
                onLoggedIn = {
                    isLoggedIn = true
                    navController.navigate("home") {
                        popUpTo("email_register") { inclusive = true }
                    }
                }
            )
        }

        composable("home") { TrapezoidScreen(navController) }

        composable("tournament") {
            TournamentScreen(
                navController = navController,
                startTournament = { restaurants ->
                    selectedRestaurants = restaurants
                    navController.navigate("tournamentMatchScreen")
                }
            )
        }

        composable("tournamentMatchScreen") {
            TournamentLoaderScreen(
                navController = navController,
                baseRestaurants = selectedRestaurants,
                onWinnerSelected = { winner ->
                }
            )
        }


        composable("shakeomat") { ShakeomatScreen(navController) }

        composable("user") { UserScreen(navController) }

        composable("favorites") { FavoritesScreen(navController) }

        composable("details/{placeId}") { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId") ?: return@composable
            val commentViewModel: CommentViewModel = viewModel()
            val favoriteViewModel: FavoriteViewModel = viewModel()

            RestaurantDetailScreen(
                placeId = placeId,
                navController = navController,
                favoriteViewModel = favoriteViewModel,
                commentViewModel = commentViewModel
            )
        }
    }
}

@Composable
fun RequestLocationPermission(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            Toast.makeText(context, "Brak uprawnień do lokalizacji!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}


@Composable
fun TrapezoidScreen(navController: NavHostController) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }

    var animateColorFill by remember { mutableStateOf(false) }
    var animationDirection by remember { mutableStateOf("none") }

    val transition = updateTransition(targetState = animateColorFill, label = "ColorFill")
    val animatedHeight by transition.animateDp(
        transitionSpec = { tween(durationMillis = 600) },
        label = "HeightAnimation"
    ) { filled ->
        if (filled) 1000.dp else 0.dp
    }

    RequestLocationPermission {
        hasLocationPermission = true
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Główne trapezowe przyciski
        TrapezoidButtons(
            onTournamentClick = {
                if (hasLocationPermission) {
                    animationDirection = "top"
                    animateColorFill = true
                } else {
                    Toast.makeText(context, "Potrzebujesz dostępu do lokalizacji!", Toast.LENGTH_SHORT).show()
                }
            },
            onShakeClick = {
                animationDirection = "bottom"
                animateColorFill = true
            }
        )
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .shadow(16.dp, CircleShape)
        )
        // Animowana nakładka (rozszerzający się kolor)
        if (animateColorFill) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedHeight)
                    .align(
                        if (animationDirection == "top") Alignment.TopCenter else Alignment.BottomCenter
                    )
                    .background(
                        if (animationDirection == "top")  MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
            )

            LaunchedEffect(animatedHeight) {
                if (animatedHeight >= 1000.dp) {
                    animateColorFill = false
                    if (animationDirection == "top") {
                        navController.navigate("tournament")
                    } else {
                        navController.navigate("shakeomat")
                    }
                }
            }
        }
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = "Profil użytkownika",
            tint = Color.White,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(10f)
                .clickable {
                    navController.navigate("user")
                }
        )
    }
}
@Composable
fun TrapezoidButtons(
    onTournamentClick: () -> Unit,
    onShakeClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Górny czerwony trapez (Turniej)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clip(TrapezoidShape(isTop = true))
                .background(MaterialTheme.colorScheme.primary)
                .border(5.dp, Color.Black)
                .clickable { onTournamentClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Turniej",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color =  MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 100.dp)
            )
        }

        // Dolny niebieski trapez (Shakeomat)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clip(TrapezoidShape(isTop = false))
                .background(MaterialTheme.colorScheme.secondary)
                .border(5.dp, Color.Black)
                .align(Alignment.BottomCenter)
                .clickable { onShakeClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Shakeomat",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color =  MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 100.dp)
            )
        }
    }
}

class TrapezoidShape(private val isTop: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()

        if (isTop) {
            path.moveTo(0f, 0f)
            path.lineTo(size.width, 0f)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height * 0.75f)
        } else {
            path.moveTo(0f, size.height * 0.25f)
            path.lineTo(size.width, size.height * 0.5f)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
        }
        path.close()

        return Outline.Generic(path)
    }
}

// Pobieranie lokalizacji
fun getCurrentLocation(context: ComponentActivity, onLocationReceived: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
        return
    }

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            val lat = location.latitude
            val lon = location.longitude
            Log.d("Location", "Lat: $lat, Lon: $lon")
            onLocationReceived(lat, lon)
        }
    }.addOnFailureListener {
        Log.e("LocationError", "Błąd pobierania lokalizacji", it)
    }
}
