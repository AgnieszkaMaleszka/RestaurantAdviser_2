import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.restaurantadviser.viewmodel.FavoriteViewModel
import com.example.restaurantadviser.repository.Restaurant
import com.example.restaurantadviser.AnimatedTopWavesBackgroundHigh
import com.example.restaurantadviser.AnimatedWavesBackgroundHigh
import com.example.restaurantadviser.RestaurantDetailsCard
import com.example.restaurantadviser.viewmodel.CommentViewModel
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WinnerScreen(
    navController: NavController,
    winner: Restaurant,
    commentViewModel: CommentViewModel = viewModel(),
    favoriteViewModel: FavoriteViewModel = viewModel()
) {
    val context = LocalContext.current
    val comments by commentViewModel.comments
    val isFavorite by favoriteViewModel.isFavorite.collectAsState()

    LaunchedEffect(winner.placeId) {
        commentViewModel.loadComments(winner.placeId)
        favoriteViewModel.checkIfFavorite(winner.placeId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedTopWavesBackgroundHigh()
        AnimatedWavesBackgroundHigh()

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            CenterAlignedTopAppBar(
                title = { Text("Zwycięzca turnieja", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Cofnij", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = winner.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            maxLines = 2
                        )

                        IconButton(
                            onClick = {
                                if (isFavorite) {
                                    favoriteViewModel.removeFavorite(winner.placeId)
                                } else {
                                    favoriteViewModel.addFavorite(context = context, restaurant = winner)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Gray.copy(alpha = 0.15f), shape = CircleShape)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Ulubione",
                                tint = if (isFavorite) Color(0xFFFFC107) else Color.LightGray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                RestaurantDetailsCard(
                    restaurant = winner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                if (comments.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {

                        comments.forEach { comment ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(comment.authorName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = SimpleDateFormat("HH:mm dd.MM", Locale.getDefault()).format(Date(comment.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color =  MaterialTheme.colorScheme.onBackground
                                        )
                                    }

                                    Text(
                                        comment.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )

                                    if (comment.aspects.isNotEmpty()) {
                                        FlowRow(
                                            modifier = Modifier.padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            comment.aspects.forEach { aspectMap ->
                                                val aspect = aspectMap["aspect"] ?: return@forEach
                                                val sentiment = aspectMap["sentiment"]?.lowercase() ?: "neutral"

                                                val (icon, color) = when (sentiment) {
                                                    "pozytywny" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                                                    "negatywny" -> Icons.Default.Remove to Color(0xFFF44336)
                                                    else -> Icons.Default.QuestionMark to Color(0xFF3F51B5)
                                                }

                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text(aspect) },
                                                    leadingIcon = {
                                                        Icon(imageVector = icon, contentDescription = null, tint = color)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Lubię",
                                            tint = if (comment.likes.isNotEmpty()) MaterialTheme.colorScheme.secondary else Color.LightGray
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "${comment.likes.size} polubień",
                                            style = MaterialTheme.typography.labelMedium
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
