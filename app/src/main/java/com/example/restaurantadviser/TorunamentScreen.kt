package com.example.restaurantadviser

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.restaurantadviser.repository.Restaurant
import com.example.restaurantadviser.repository.RestaurantRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.restaurantadviser.AnimatedTopWavesBackground
import com.example.restaurantadviser.AnimatedWavesBackground

fun getUserLocation(context: Context, callback: (LatLng) -> Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        callback(LatLng(52.2297, 21.0122))
        return
    }
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) callback(LatLng(loc.latitude, loc.longitude))
    }
}

fun determineAvailableCounts(restaurantCount: Int): List<Int> =
    when {
        restaurantCount >= 32 -> listOf(8, 16, 32)
        restaurantCount >= 16 -> listOf(8, 16)
        else -> listOf(8)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentScreen(navController: NavController, startTournament: (List<Restaurant>) -> Unit) {
    val context = LocalContext.current
    val apiKey = context.getString(R.string.google_maps_key)
    val restaurantRepository = remember { RestaurantRepository(apiKey) }

    var selectedDistance by remember { mutableStateOf(1000) }
    var selectedCount by remember { mutableStateOf(8) }
    var selectedPriority by remember { mutableStateOf(1) }
    var restaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var availableCounts by remember { mutableStateOf(listOf(8, 16, 32)) }
    var userLocation by remember { mutableStateOf(LatLng(52.2297, 21.0122)) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var timeoutReached by remember { mutableStateOf(false) }
    var restaurantsLoaded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    LaunchedEffect(context) {
        getUserLocation(context) { userLocation = it }
    }

    LaunchedEffect(selectedDistance, selectedCount, selectedPriority, userLocation, isSearching) {
        if (isSearching) {
            isLoading = true
            timeoutReached = false
            val job = CoroutineScope(Dispatchers.IO).launch {
                val fetched = restaurantRepository.fetchNearbyRestaurants(
                    selectedDistance, userLocation, selectedPriority
                )
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (fetched.size < 8) {
                        errorMessage = "Zbyt mało restauracji. Zwiększ dystans i spróbuj ponownie."
                        restaurants = emptyList()
                        isEditing = true
                        isSearching = false
                        restaurantsLoaded = false
                    } else {
                        errorMessage = ""
                        restaurants = fetched
                        availableCounts = determineAvailableCounts(fetched.size)
                        isSearching = false
                        restaurantsLoaded = true
                    }
                }

            }
            delay(10000)
            if (job.isActive) {
                job.cancel()
                isLoading = false
                timeoutReached = true
                isSearching = false
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedTopWavesBackgroundHigh()
        AnimatedWavesBackground()
        Column(
            modifier = Modifier.fillMaxSize()
        ) {                CenterAlignedTopAppBar(
                    title = { Text("Ustawienia turnieju", color = MaterialTheme.colorScheme.onSurface) },
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
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {

                    // KARTA 1 — Ustawienia przed wyszukiwaniem
                    if ((!isSearching && (restaurants.isEmpty() || isEditing)) && !restaurantsLoaded) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Wybierz dystans",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                DistanceSlider(selectedDistance, { selectedDistance = it }, true)

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "Wybierz priorytet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                PrioritySelector(selectedPriority, { selectedPriority = it }, true)

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { isSearching = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,    // kolor tła
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text("Szukaj restauracji")
                                }

                            }
                        }
                    }

                    // KARTA 2 — Ładowanie lub timeout
                    if (isLoading || timeoutReached || errorMessage.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.secondary,
                                        strokeWidth = 4.dp
                                    )
                                    Text(
                                        "Szukam restauracji...",
                                        modifier = Modifier.padding(top = 8.dp),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                if (timeoutReached) {
                                    Text("Przekroczono czas wyszukiwania.", color = MaterialTheme.colorScheme.error)
                                    Button(onClick = {
                                        timeoutReached = false
                                        isEditing = true
                                        isSearching = false
                                        restaurants = emptyList()
                                    }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                        Text("Wróć do ustawień", color = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                                if (errorMessage.isNotEmpty()) {
                                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // KARTA 3 — Ustawienia liczby restauracji
                    if (restaurantsLoaded) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Wybierz liczbę uczestników",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                RestaurantCountSelector(
                                    selectedCount,
                                    { selectedCount = it },
                                    availableCounts
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        isEditing = true
                                        isSearching = false
                                        restaurants = emptyList()
                                        restaurantsLoaded = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                ) {
                                    Text(
                                        "Zmień ustawienia",
                                        color = MaterialTheme.colorScheme.onSurface // kolor tekstu
                                    )
                                    Spacer(modifier = Modifier.width(8.dp)) // odstęp między tekstem a ikoną
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Start",
                                        tint = MaterialTheme.colorScheme.onSurface // kolor ikony
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { startTournament(restaurants.take(selectedCount)) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary // kolor tła przycisku
                                    )
                                ) {
                                    Text(
                                        text = "Rozpocznij turniej",
                                        color = MaterialTheme.colorScheme.onBackground  // kolor tekstu
                                    )
                                    Spacer(modifier = Modifier.width(8.dp)) // odstęp między tekstem a ikoną
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Start",
                                        tint = MaterialTheme.colorScheme.onBackground // kolor ikony
                                    )
                                }

                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun DistanceSlider(selected: Int, onValueChange: (Int) -> Unit, isEnabled: Boolean) {
    val options = listOf(500, 1000, 2000, 5000, 5001)
    val index = options.indexOf(selected)

    val green =MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Slider(
            value = index.toFloat(),
            onValueChange = { onValueChange(options[it.toInt()]) },
            valueRange = 0f..(options.size - 1).toFloat(),
            steps = options.size - 2,
            enabled = isEnabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.onPrimary ,
                activeTrackColor = MaterialTheme.colorScheme.onPrimary,
                inactiveTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth(0.75f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                selected == 5001 -> "5km+"
                selected >= 1000 -> "${selected / 1000} km"
                else -> "${selected} m"
            },
            color = Color.Gray,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun RestaurantCountSelector(
    selected: Int,
    onValueChange: (Int) -> Unit,
    availableCounts: List<Int>
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        availableCounts.forEach { count ->
            OutlinedButton(
                onClick = { onValueChange(count) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (selected == count) MaterialTheme.colorScheme.onBackground else Color.LightGray
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected == count) MaterialTheme.colorScheme.onBackground else Color.LightGray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text("$count")
            }
        }
    }
}

@Composable
fun PrioritySelector(selected: Int, onValueChange: (Int) -> Unit, isEnabled: Boolean) {
    val options = listOf("Ocena", "Dystans", "Cena", "Liczba ocen")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Podział na 2 kolumny w Gridzie
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            itemsIndexed(options) { index, label ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEnabled) { onValueChange(index + 1) }
                        .padding(4.dp)
                ) {
                    RadioButton(
                        selected = selected == index + 1,
                        onClick = { onValueChange(index + 1) },
                        enabled = isEnabled,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.secondary,
                            unselectedColor = MaterialTheme.colorScheme.onPrimary,
                            disabledSelectedColor = MaterialTheme.colorScheme.onPrimary,
                            disabledUnselectedColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Text(
                        text = label,
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

