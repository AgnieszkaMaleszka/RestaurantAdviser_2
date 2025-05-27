package com.example.restaurantadviser.viewmodel


import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurantadviser.repository.Restaurant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FavoriteViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()

    private val _favorites = MutableStateFlow<List<Restaurant>>(emptyList())
    val favorites = _favorites.asStateFlow()

    fun checkIfFavorite(restaurantId: String) {
        val user = auth.currentUser ?: return
        firestore.collection("users")
            .document(user.uid)
            .collection("favorites")
            .document(restaurantId)
            .addSnapshotListener { snapshot, _ ->
                _isFavorite.value = snapshot?.exists() == true
            }
    }

    fun addFavorite(
        context: Context,
        restaurant: Restaurant
    ) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch

            try {
                val data = mapOf(
                    "name" to restaurant.name,
                    "address" to restaurant.address,
                    "location" to mapOf("lat" to restaurant.latitude, "lng" to restaurant.longitude),
                    "rating" to restaurant.rating,
                    "userRatingsTotal" to restaurant.userRatingsTotal,
                    "priceLevel" to restaurant.priceLevel,
                    "photo_reference" to restaurant.photo_reference,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                firestore.collection("users")
                    .document(user.uid)
                    .collection("favorites")
                    .document(restaurant.placeId)
                    .set(data)
                    .await()

                _isFavorite.value = true

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeFavorite(restaurantId: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            try {
                firestore.collection("users")
                    .document(user.uid)
                    .collection("favorites")
                    .document(restaurantId)
                    .delete()
                    .await()

                _isFavorite.value = false

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadFavorites() {
        val user = auth.currentUser ?: return
        firestore.collection("users")
            .document(user.uid)
            .collection("favorites")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val address = doc.getString("address") ?: ""
                    val location = doc.get("location") as? Map<*, *>
                    val lat = (location?.get("lat") as? Number)?.toDouble() ?: return@mapNotNull null
                    val lng = (location["lng"] as? Number)?.toDouble() ?: return@mapNotNull null
                    val photoUrl = doc.getString("photoUrl") ?: ""
                    val rating = (doc.get("rating") as? Number)?.toDouble() ?: 0.0
                    val userRatingsTotal = (doc.get("userRatingsTotal") as? Number)?.toInt()
                    val priceLevel = (doc.get("priceLevel") as? Number)?.toInt()
                    val photoReference = doc.getString("photo_reference")

                    Restaurant(
                        placeId = doc.id,
                        name = name,
                        rating = rating,
                        address = address,
                        latitude = lat,
                        longitude = lng,
                        photo_reference = photoReference,
                        priceLevel = priceLevel,
                        userRatingsTotal = userRatingsTotal,
                    )
                } ?: emptyList()

                _favorites.value = list
            }
    }
}
