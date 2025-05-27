@file:JvmName("TournamentMatchScreenKt")

package com.example.restaurantadviser

import RestaurantCard
import WinnerScreen
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.restaurantadviser.repository.Restaurant
import com.example.restaurantadviser.repository.RestaurantRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.restaurantadviser.AnimatedTopWavesBackgroundHigh
import com.example.restaurantadviser.AnimatedWavesBackgroundHigh
fun getPhotoUrl(context: android.content.Context, photoReference: String?, maxWidth: Int = 400): String? {
    val apiKey = context.getString(R.string.google_maps_key)
    return photoReference?.let {
        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=$maxWidth&photoreference=$it&key=$apiKey"
    }
}

@Composable
fun TournamentLoaderScreen(
    navController: NavController,
    baseRestaurants: List<Restaurant>,
    onWinnerSelected: (Restaurant) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RestaurantRepository(context.getString(R.string.google_maps_key)) }
    var fullRestaurants by remember { mutableStateOf<List<Restaurant>?>(null) }

    LaunchedEffect(baseRestaurants) {
        fullRestaurants = baseRestaurants.map { restaurant ->
            async {
                repository.fetchRestaurantDetails(restaurant.placeId)
            }
        }.awaitAll().filterNotNull()
    }

    if (fullRestaurants == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        TournamentMatchScreen(
            navController = navController,
            restaurants = fullRestaurants!!,
            onWinnerSelected = onWinnerSelected
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentMatchScreen(
    navController: NavController,
    restaurants: List<Restaurant>,
    onWinnerSelected: (Restaurant) -> Unit
) {
    var remainingRestaurants by remember { mutableStateOf(restaurants) }
    var winners by remember { mutableStateOf(emptyList<Restaurant>()) }
    var currentRound by remember { mutableStateOf(1) }

    val totalMatchesPerRound = generateSequence(restaurants.size) { it / 2 }
        .takeWhile { it >= 1 }
        .toList()
    val totalRounds = totalMatchesPerRound.size

    LaunchedEffect(remainingRestaurants, winners) {
        if (remainingRestaurants.isEmpty() && winners.isNotEmpty()) {
            if (winners.size == 1) {
                onWinnerSelected(winners.first())
            } else {
                remainingRestaurants = winners
                winners = emptyList()
                currentRound++
            }
        }
    }

    val currentPair = remainingRestaurants.take(2)
    val roundColor = when (currentRound % 4) {
        1 -> Color(0xFF65B769)
        2 -> Color(0xFFFFC107)
        3 -> Color(0xFF4CAF50)
        else -> Color(0xFF2196F3)
    }

    val animatedColor by animateColorAsState(targetValue = roundColor, label = "RoundColor")

    if (remainingRestaurants.size >= 2) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )  {
            AnimatedTopWavesBackgroundHigh()
            AnimatedWavesBackgroundHigh()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("Turniej", color = MaterialTheme.colorScheme.onSurface) },
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

                // Pasek postępu – PRZENIESIONY DO WIDOCZNEJ KOLUMNY
                val allSegments = totalMatchesPerRound
                val totalSegments = allSegments.sum()
                val segmentColors = listOf(
                    Color(0xFF65B769),
                    Color(0xFFFFC107),
                    Color(0xFF4CAF50),
                    Color(0xFF2196F3)
                )
                val currentSegmentIndex = allSegments
                    .take(currentRound - 1)
                    .sum() + (allSegments.getOrNull(currentRound - 1) ?: 1 - remainingRestaurants.size / 2)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(totalSegments) { index ->
                        val roundIndex = allSegments
                            .runningFold(0) { acc, value -> acc + value }
                            .indexOfFirst { it > index }
                            .coerceAtLeast(0)

                        val targetColor =
                            if (index < currentSegmentIndex) segmentColors[roundIndex % segmentColors.size]
                            else MaterialTheme.colorScheme.onBackground

                        val animatedSegmentColor by animateColorAsState(
                            targetValue = targetColor,
                            label = "SegmentColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(animatedSegmentColor)
                        )
                    }
                }


                    Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Runda $currentRound z $totalRounds",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    RestaurantCard(
                        restaurant = currentPair[0],
                        onClick = {
                            winners += currentPair[0]
                            remainingRestaurants = remainingRestaurants.drop(2)
                        }
                    )

                    Text("VS", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)

                    RestaurantCard(
                        restaurant = currentPair[1],
                        onClick = {
                            winners += currentPair[1]
                            remainingRestaurants = remainingRestaurants.drop(2)
                        }
                    )
                }
            }
        }
    } else if (winners.size == 1) {
        WinnerScreen(navController, winners.first())
    }
}
