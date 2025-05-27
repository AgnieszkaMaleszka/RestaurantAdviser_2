import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.restaurantadviser.getPhotoUrl
import com.example.restaurantadviser.repository.Restaurant
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import com.example.restaurantadviser.R

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    backgroundColor: Color = Color.Transparent,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val photoRefs = restaurant.photoReferences ?: listOfNotNull(restaurant.photo_reference)
    var showDetails by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = showDetails,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "DetailsTransition"
        ) { isVisible ->
            if (!isVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
                ) {
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(photoRefs, key = { it }) { ref ->
                            val photoUrl = getPhotoUrl(context, ref)
                            if (photoUrl != null) {
                                Box(modifier = Modifier.width(280.dp)) {
                                    Image(
                                        painter = rememberAsyncImagePainter(photoUrl),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .align(Alignment.BottomStart)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = restaurant.name,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { showDetails = true },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color.White.copy(alpha = 0.15f), shape = CircleShape)
                                                    .clip(CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Szczegóły",
                                                    tint = MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val prevIndex = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                                listState.animateScrollToItem(prevIndex)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(2.dp)
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, "Poprzednie", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val nextIndex = (listState.firstVisibleItemIndex + 1).coerceAtMost(photoRefs.size - 1)
                                listState.animateScrollToItem(nextIndex)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(2.dp)
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, "Następne", tint = Color.White, modifier = Modifier.rotate(180f))
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        photoRefs.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (index == currentIndex) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (index == currentIndex) MaterialTheme.colorScheme.background else Color.Gray)
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GoogleMapView(location = LatLng(restaurant.latitude, restaurant.longitude))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { showDetails = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Zamknij",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        Text(
                            text = restaurant.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2
                        )

                        Text(
                            text = restaurant.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${restaurant.rating}",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            repeat(5) { i ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Ocena",
                                    tint = if (i < restaurant.rating.toInt()) Color(0xFFFF9800)
                                            else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        restaurant.priceLevel?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Cena:", fontWeight = FontWeight.Medium, color =  MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.width(4.dp))
                                repeat(it) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = "Cena",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
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
fun GoogleMapView(location: LatLng) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 15f)
    }

    val mapProperties = remember { mutableStateOf(MapProperties()) }

    LaunchedEffect(isDarkTheme) {
        if (isDarkTheme) {
            try {
                val style = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_dark_style)
                mapProperties.value = mapProperties.value.copy(mapStyleOptions = style)
            } catch (e: Exception) {
                Log.e("MapStyle", "Nie udało się załadować stylu mapy", e)
            }
        } else {
            mapProperties.value = mapProperties.value.copy(mapStyleOptions = null)
        }
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp)),
        cameraPositionState = cameraPositionState,
        properties = mapProperties.value
    ) {
        Marker(
            state = rememberMarkerState(position = location),
            title = "Lokalizacja",
            snippet = "${location.latitude}, ${location.longitude}"
        )
    }
}

