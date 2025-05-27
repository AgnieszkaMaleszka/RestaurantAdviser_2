package com.example.restaurantadviser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.restaurantadviser.ui.theme.RestaurantAdviserTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Easing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon

fun OvershootInterpolator.asEasing(): Easing = Easing { input -> getInterpolation(input) }

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RestaurantAdviserTheme(dynamicColor = false) {
                SplashScreen(
                    onFinish = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}


@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }

    val placeOffsetY = remember { Animatable(20f) }
    val placeAlpha = remember { Animatable(0f) }

    val textAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Logo pojawia się
        logoAlpha.animateTo(1f, tween(600))
        logoScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing))

        // 2. Ikona Place "wyrasta" z logo
        delay(300)
        placeAlpha.animateTo(1f, tween(300))
        placeOffsetY.animateTo(
            targetValue = -60f,
            animationSpec = tween(700, easing = OvershootInterpolator(6f).asEasing())
        )

        // 3. Tekst pojawia się
        delay(400)
        textAlpha.animateTo(1f, tween(600))
        subtitleAlpha.animateTo(1f, tween(600))

        // 4. Po chwili przejście
        delay(2000)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopCenter) {
                // LOGO
                Image(
                    painter = painterResource(id = R.drawable.logo2),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(140.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                        .clip(RoundedCornerShape(24.dp))
                )

                // IKONA PLACE "wyrasta" z logo
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Lokalizacja",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            translationY = placeOffsetY.value
                            alpha = placeAlpha.value
                            scaleX = 1.1f
                            scaleY = 1.1f
                        }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // NAZWA
            Text(
                text = "GdzieJemy?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // OPIS
            Text(
                text = "Odkrywaj restauracje, które pokochasz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}