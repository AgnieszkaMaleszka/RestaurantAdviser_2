package com.example.restaurantadviser
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Save
import GoogleMapView
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.restaurantadviser.repository.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.restaurantadviser.repository.RestaurantRepository
import com.example.restaurantadviser.viewmodel.CommentViewModel
import com.example.restaurantadviser.viewmodel.FavoriteViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.AssistChip
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RestaurantDetailScreen(
    placeId: String,
    navController: NavHostController,
    favoriteViewModel: FavoriteViewModel = viewModel(),
    commentViewModel: CommentViewModel = viewModel()
){
    val context = LocalContext.current
    val currentUserId = Firebase.auth.currentUser?.uid
    val currentUserName = Firebase.auth.currentUser?.displayName ?: "Anonim"

    val comments by commentViewModel.comments

    val repository = remember { RestaurantRepository(context.getString(R.string.google_maps_key)) }
    var restaurant by remember { mutableStateOf<Restaurant?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var editingCommentId by remember { mutableStateOf<String?>(null) }

    // Komentarze
    var commentText by remember { mutableStateOf("") }
    var expandedCommentId by remember { mutableStateOf<String?>(null) }

    // Ulubione
    val isFavorite by favoriteViewModel.isFavorite.collectAsState()

    LaunchedEffect(placeId) {
        restaurant = repository.fetchRestaurantDetails(placeId)
        isLoading = false
        favoriteViewModel.checkIfFavorite(placeId)
        commentViewModel.loadComments(placeId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedTopWavesBackgroundHigh()
        AnimatedWavesBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = restaurant?.name ?: "Szczegóły",
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Cofnij",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    restaurant?.let {
                        IconButton(onClick = {
                            if (isFavorite) {
                                favoriteViewModel.removeFavorite(it.placeId)
                            } else {
                                favoriteViewModel.addFavorite(context, it)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Ulubione",
                                tint = if (isFavorite) Color(0xFFFFC107) else Color.White,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(), // zajmij cały ekran
                    contentAlignment = Alignment.Center // wyśrodkuj zawartość
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onBackground,
                        strokeWidth = 4.dp
                    )
                }
            } else if (restaurant == null) {
                Text("Nie znaleziono restauracji.", color = Color.Red, modifier = Modifier.padding(16.dp))
            } else {
                RestaurantDetailsCard(
                    restaurant = restaurant!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                //  KOMENTARZE
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {

                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                label = {
                                    Text(
                                        if (editingCommentId != null) "Edytuj opinie" else "Dodaj opinie",
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                    cursorColor = MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.weight(1f)
                            )




                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        val trimmedText = commentText.trim()
                                        if (editingCommentId != null) {
                                            commentViewModel.editComment(editingCommentId!!, trimmedText)
                                            editingCommentId = null
                                        } else {
                                            commentViewModel.addComment(placeId, trimmedText, currentUserName)
                                        }
                                        commentText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary),
                                modifier = Modifier.size(56.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = if (editingCommentId != null) Icons.Default.Save else Icons.Default.Send,
                                    contentDescription = if (editingCommentId != null) "Zapisz komentarz" else "Wyślij komentarz",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        comments.forEach { comment ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(comment.authorName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = SimpleDateFormat("HH:mm dd.MM", Locale.getDefault()).format(Date(comment.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Text(comment.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                                Log.d("restaurantDetailsScreen", comment.aspects.toString())
                                if (comment.aspects.isNotEmpty()) {
                                    FlowRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        comment.aspects.forEach { aspectMap ->
                                            val aspect = aspectMap["aspect"] ?: return@forEach
                                            val sentiment = aspectMap["sentiment"]?.lowercase() ?: "neutral"

                                            val icon = when (sentiment) {
                                                "pozytywny" -> Icons.Default.Add
                                                "negatywny" -> Icons.Default.Remove
                                                else -> Icons.Default.QuestionMark
                                            }

                                            val color = when (sentiment) {
                                                "pozytywny" -> Color(0xFF4CAF50)
                                                "negatywny" -> Color(0xFFF44336)
                                                else -> Color(0xFF3F51B5)
                                            }

                                            AssistChip(
                                                onClick = {},
                                                label = { Text(aspect, color = MaterialTheme.colorScheme.onBackground) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = color
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }


                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                    IconButton(onClick = { commentViewModel.toggleLike(comment) }) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Lubię",
                                            tint = if (comment.likes.contains(currentUserId)) Color(0xFFFFC107)else Color.LightGray
                                        )
                                    }
                                    Text("${comment.likes.size} polubień", style = MaterialTheme.typography.labelMedium)

                                   if (comment.authorId == currentUserId) {
                                        Spacer(modifier = Modifier.weight(1f))

                                        Box {
                                            IconButton(onClick = {
                                                expandedCommentId = if (expandedCommentId == comment.id) null else comment.id
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "Opcje",
                                                    tint = MaterialTheme.colorScheme.onBackground
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = expandedCommentId == comment.id,
                                                onDismissRequest = { expandedCommentId = null },
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.background)
                                                    .clip(RoundedCornerShape(12.dp))
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Edytuj", color = MaterialTheme.colorScheme.onBackground) },
                                                    onClick = {
                                                        commentText = comment.text
                                                        editingCommentId = comment.id
                                                        expandedCommentId = null
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Usuń", color = Color.Red) },
                                                    onClick = {
                                                        commentViewModel.deleteComment(comment.id)
                                                        expandedCommentId = null
                                                    }
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
        }
    }
}
@Composable
fun RestaurantDetailsCard(restaurant: Restaurant, modifier: Modifier = Modifier) {
    val dniTygodniaMap = mapOf(
        "Monday" to "Poniedziałek",
        "Tuesday" to "Wtorek",
        "Wednesday" to "Środa",
        "Thursday" to "Czwartek",
        "Friday" to "Piątek",
        "Saturday" to "Sobota",
        "Sunday" to "Niedziela"
    )

    var showMap by remember { mutableStateOf(false) }

    fun convertSingleTimeTo24(time: String): String {
        val trimmed = time.trim()
        val match = Regex("(\\d{1,2}):(\\d{2}) ?(AM|PM)", RegexOption.IGNORE_CASE).find(trimmed)
        return if (match != null) {
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            val amPm = match.groupValues[3].uppercase()
            val hour24 = when {
                amPm == "AM" && hour == 12 -> 0
                amPm == "AM" -> hour
                amPm == "PM" && hour != 12 -> hour + 12
                else -> hour
            }
            "%02d:%02d".format(hour24, minute)
        } else {
            time
        }
    }

    fun convertTo24HourFormat(timeRange: String): String {
        val rangeParts = timeRange.split(Regex("( – | - )"))
        return if (rangeParts.size == 2) {
            val start = convertSingleTimeTo24(rangeParts[0])
            val end = convertSingleTimeTo24(rangeParts[1])
            "$start – $end"
        } else {
            convertSingleTimeTo24(timeRange)
        }
    }
    fun mapOpenStatus(status: Boolean?): String = when (status) {
        true -> "1"
        false -> "0"
        null -> "2" // Za 2 godziny lub nieznane
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!restaurant.photoReferences.isNullOrEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(restaurant.photoReferences) { photoRef ->
                        val context = LocalContext.current
                        val photoUrl = getPhotoUrl(context, photoRef)
                        if (photoUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(photoUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .width(280.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Adres, ocena i przycisk pokaż na mapie
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = restaurant.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = { showMap = !showMap }) {
                        Icon(Icons.Default.Place, contentDescription = null, tint =  MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(50.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                if (restaurant.rating > 0) {
                    Text(
                        text = "Ocena: ${restaurant.rating} (${restaurant.userRatingsTotal ?: 0} opinii)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (showMap) {
                    Spacer(modifier = Modifier.height(16.dp))
                    GoogleMapView(location = LatLng(restaurant.latitude, restaurant.longitude))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (restaurant.businessStatus) {
                            "OPERATIONAL" -> "Działa"
                            "CLOSED_TEMPORARILY" -> "Tymczasowo zamknięta"
                            "CLOSED_PERMANENTLY" -> "Na stałe zamknięta"
                            else -> "Status nieznany"
                        },
                        color = when (restaurant.businessStatus) {
                            "OPERATIONAL" -> Color(0xFF4CAF50)
                            "CLOSED_TEMPORARILY" -> Color(0xFFFF9800)
                            "CLOSED_PERMANENTLY" -> Color(0xFFF44336)
                            else ->MaterialTheme.colorScheme.onBackground
                        },
                        fontWeight = FontWeight.Bold
                    )

                    if (restaurant.isOpenNow != null) {
                        Text(
                            text = when (mapOpenStatus(restaurant.isOpenNow)) {
                                "1" -> "Otwarte teraz"
                                "0" -> "Zamknięte"
                                else -> "Otwarte za 2 godziny"
                            },
                            color = if (restaurant.isOpenNow) MaterialTheme.colorScheme.onBackground else Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                restaurant.openingHours?.let { hours ->
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text("Godziny otwarcia:", fontWeight = FontWeight.SemiBold)
                        hours.forEach { line ->
                            val translated = dniTygodniaMap.entries.fold(line) { acc, (en, pl) ->
                                acc.replace(en, pl)
                            }.replace("Closed", "Zamknięte")

                            val converted = convertTo24HourFormat(translated)
                            Text(
                                text = converted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
