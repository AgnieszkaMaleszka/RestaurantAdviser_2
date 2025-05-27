package com.example.restaurantadviser

import GoogleMapView
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.restaurantadviser.repository.Restaurant
import com.example.restaurantadviser.repository.RestaurantRepository
import com.example.restaurantadviser.viewmodel.FavoriteViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun BouncingIcon(
    icon: ImageVector,
    size: Dp = 100.dp,
    rotation: Float = 0f
) {
    val density = LocalDensity.current
    var parentWidth by remember { mutableStateOf(0f) }
    var parentHeight by remember { mutableStateOf(0f) }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val iconSizePx = with(density) { size.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            //.background(MaterialTheme.colorScheme.background)
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size
                parentWidth = size.width.toFloat()
                parentHeight = size.height.toFloat()
            }
    ) {
        LaunchedEffect(parentWidth, parentHeight) {
            if (parentWidth == 0f || parentHeight == 0f) return@LaunchedEffect

            val maxX = parentWidth - iconSizePx
            val maxY = parentHeight - iconSizePx

            var velocityX = 6f
            var velocityY = 4f

            while (true) {
                offsetX.snapTo((offsetX.value + velocityX).coerceIn(0f, maxX))
                offsetY.snapTo((offsetY.value + velocityY).coerceIn(0f, maxY))

                if (offsetX.value <= 0f || offsetX.value >= maxX) velocityX *= -1
                if (offsetY.value <= 0f || offsetY.value >= maxY) velocityY *= -1

                delay(16L)
            }
        }

        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    translationX = offsetX.value
                    translationY = offsetY.value
                    rotationZ = rotation
                    alpha = 0.9f
                }
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(12.dp) // <- tylko tutaj, wewnątrz, działa jak margines
                    .fillMaxSize(),
                tint = MaterialTheme.colorScheme.background
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShakeomatScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val coroutineScope = rememberCoroutineScope()
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }

    val restaurants = remember { mutableStateListOf<Restaurant>() }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    val repository = remember { RestaurantRepository(context.getString(R.string.google_maps_key)) }

    val shakeThreshold = 11f
    var lastShakeTime by remember { mutableStateOf(0L) }
    var isShaking by remember { mutableStateOf(false) }
    var shakeProgress by remember { mutableStateOf(0f) }
    var canShake by remember { mutableStateOf(true) }
    val favoriteViewModel: FavoriteViewModel = viewModel()
    val isFavorite by favoriteViewModel.isFavorite.collectAsState()

    val rotation by animateFloatAsState(
        targetValue = if (isShaking) 360f else 0f,
        animationSpec = tween(700),
        label = "rotation"
    )

    // Pobierz lokalizację i restauracje
    LaunchedEffect(Unit) {
        getCurrentLocation(activity) { lat, lon ->
            coroutineScope.launch {
                val fetched = repository.fetchNearbyRestaurants(
                    distance = 3000,
                    userLocation = LatLng(lat, lon),
                    sortBy = RestaurantRepository.SortBy.RATING.value
                )
                restaurants.clear()
                restaurants.addAll(fetched)
            }
        }
    }

    // Nasłuchiwanie potrząśnięcia
    DisposableEffect(Unit) {
        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tiltX = event.values[0]
                tiltY = event.values[1]

                val acceleration = sqrt(
                    event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]
                )
                val currentTime = System.currentTimeMillis()
                if (acceleration > shakeThreshold && currentTime - lastShakeTime > 1000 && canShake) {
                    lastShakeTime = currentTime
                    isShaking = true
                    coroutineScope.launch {
                        delay(300)
                        isShaking = false
                        shakeProgress += 20f
                        if (shakeProgress >= 100f) {
                            shakeProgress = 0f
                            canShake = false
                            if (restaurants.isNotEmpty()) {
                                selectedRestaurant = restaurants.random()
                            }
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Shakeomat", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Cofnij",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, // zielony
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ){
            // Tło: fale
                AnimatedTopWavesBackgroundHigh()
                AnimatedWavesBackgroundHigh()

                if (selectedRestaurant != null) {
                    val photoUrl = getPhotoUrl(context, selectedRestaurant!!.photo_reference)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(500)) + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (photoUrl != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = photoUrl),
                                    contentDescription = "Zdjęcie restauracji",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = selectedRestaurant!!.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = selectedRestaurant!!.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "${selectedRestaurant!!.rating} (${selectedRestaurant!!.userRatingsTotal ?: 0} opinii)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )

                            GoogleMapView(
                                location = LatLng(
                                    selectedRestaurant!!.latitude,
                                    selectedRestaurant!!.longitude
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        selectedRestaurant = null
                                        shakeProgress = 0f
                                        canShake = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Losuj ponownie", color = MaterialTheme.colorScheme.onBackground)
                                }


                                LaunchedEffect(selectedRestaurant?.placeId) {
                                    selectedRestaurant?.placeId?.let {
                                        favoriteViewModel.checkIfFavorite(it)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        selectedRestaurant?.let {
                                            if (isFavorite) {
                                                favoriteViewModel.removeFavorite(it.placeId)
                                            } else {
                                                favoriteViewModel.addFavorite(
                                                    context = context,
                                                    restaurant = it
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                        .clip(CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Ulubione",
                                        tint = if (isFavorite) Color(0xFFFFC107) else Color.LightGray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedTopWavesBackgroundHigh()
                        AnimatedWavesBackgroundHigh()

                        BouncingIcon(
                            icon = Icons.Rounded.Place,
                            rotation = rotation
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp)
                                .zIndex(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val alpha by rememberInfiniteTransition(label = "pulse")
                                .animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        tween(1000),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "alpha"
                                )

                            Text(
                                text = "Potrząśnij telefonem,\naby wylosować restaurację!",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.alpha(alpha),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Gray.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(shakeProgress / 100f)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary,                                                )
                                            )
                                        )
                                )
                            }

                            Text(
                                text = "${shakeProgress.toInt()}% wstrząśnięcia",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

}

