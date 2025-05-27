package com.example.restaurantadviser.repository

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection
import java.net.URL

// Data class to represent a restaurant

data class Restaurant(
    val placeId: String,
    val name: String,
    val rating: Double,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val photo_reference: String? = null,
    val photoReferences: List<String>? = null, //wszystkie zdjęcia
    val priceLevel: Int? = null,
    val userRatingsTotal: Int? = null,
    val businessStatus: String? = null,
    val isOpenNow: Boolean? = null,
    val openingHours: List<String>? = null
)


class RestaurantRepository(private val apiKey: String) {
    private val client = OkHttpClient()

    // Enum for sort options with integer values
    enum class SortBy(val value: Int) {
        RATING(1),
        DISTANCE(2),
        PRICING(3),
        REVIEW_COUNT(4)
    }
/*
    // Fetch nearby restaurants using Google Places API
    suspend fun fetchNearbyRestaurants(
        distance: Int,
        userLocation: LatLng,
        sortBy: Int
    ): List<Restaurant> = withContext(Dispatchers.IO) {
        Log.d("RestaurantRepository", "Fetching restaurants")
        Log.d("RestaurantRepository", "${sortBy}")

        val allRestaurants = mutableListOf<Restaurant>()
        var nextPageToken: String? = null

        try {
            val effectiveRadius = if (distance == 5001) 100000 else distance
            val effectiveSortBy = if (distance == 5001) 2 else sortBy  // wymuszamy sortowanie po odległości

            // Pierwsze zapytanie
            var url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                    "?location=${userLocation.latitude},${userLocation.longitude}" +
                    "&radius=$effectiveRadius" +
                    "&type=restaurant" +
                    "&key=$apiKey"


            do {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@withContext emptyList()

                val jsonObject = JSONObject(responseBody)
                val results = jsonObject.getJSONArray("results")

                // Mapowanie wyników na obiekty Restaurant
                val restaurants = (0 until results.length()).map { i ->
                    val item = results.getJSONObject(i)
                    val placeId = item.optString("place_id", "unknown_id")
                    val name = item.optString("name", "Unknown Restaurant")
                    val rating = item.optDouble("rating", 0.0)
                    val address = item.optString("vicinity", "No address available")
                    val priceLevel = item.optInt("price_level", 10)  // -1 means no pricing info
                    val userRatingsTotal = item.optInt("user_ratings_total", 0)
                    val location = item.getJSONObject("geometry").getJSONObject("location")
                    val latitude = location.optDouble("lat", 0.0)
                    val longitude = location.optDouble("lng", 0.0)
                    val photoReference =
                        item.optJSONArray("photos")?.optJSONObject(0)?.optString("photo_reference")
                    Restaurant(
                        placeId = placeId,
                        name = name,
                        rating = rating,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        photo_reference = photoReference,
                        priceLevel = priceLevel,
                        userRatingsTotal = userRatingsTotal
                    )
                }

                allRestaurants.addAll(restaurants)

                // Sprawdzenie, czy są następne strony
                nextPageToken = jsonObject.optString("next_page_token", null)
                if (nextPageToken != null) {
                    url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                            "?location=${userLocation.latitude},${userLocation.longitude}" +
                            "&radius=$distance" +
                            "&type=restaurant" +
                            "&key=$apiKey" +
                            "&pagetoken=$nextPageToken"
                }

                // Czekanie przed kolejnym zapytaniem, aby API mogło przetworzyć token
                if (nextPageToken != null) {
                    Thread.sleep(2000)  // Czekaj 2 sekundy przed kolejnym zapytaniem
                }

            } while (nextPageToken != null && allRestaurants.size < 32)

            // Sortowanie wyników w zależności od wybranej opcji
            val sortedRestaurants = when (effectiveSortBy) {
                2 -> allRestaurants.sortedBy { restaurant ->
                    val latDiff = restaurant.latitude - userLocation.latitude
                    val lngDiff = restaurant.longitude - userLocation.longitude
                    Math.sqrt(latDiff * latDiff + lngDiff * lngDiff)
                }
                1 -> allRestaurants.sortedByDescending { it.rating }
                3 -> allRestaurants.sortedBy { it.priceLevel }
                4 -> allRestaurants.sortedByDescending { it.userRatingsTotal }
                else -> allRestaurants
            }


            // Zwracanie posortowanych wyników
            return@withContext sortedRestaurants.take(32)  // Zwróć maksymalnie 32 restauracje
        } catch (e: Exception) {
            Log.d("RestaurantRepository", "Error fetching restaurants")
            e.printStackTrace()
            return@withContext emptyList()
        }
    }*/

    suspend fun fetchNearbyRestaurants(
        distance: Int,
        userLocation: LatLng,
        sortBy: Int
    ): List<Restaurant> = withContext(Dispatchers.IO) {
        Log.d("RestaurantRepository", "Fetching restaurants")
        Log.d("RestaurantRepository", "SortBy: $sortBy")

        val allRestaurants = mutableListOf<Restaurant>()
        var nextPageToken: String? = null

        try {
            val effectiveRadius = if (distance == 5001) 100000 else distance
            val effectiveSortBy = if (distance == 5001) 2 else sortBy

            var url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                    "?location=${userLocation.latitude},${userLocation.longitude}" +
                    "&radius=$effectiveRadius" +
                    "&type=restaurant" +
                    "&key=$apiKey"

            do {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@withContext emptyList()

                val jsonObject = JSONObject(responseBody)
                val results = jsonObject.getJSONArray("results")

                val restaurants = (0 until results.length()).map { i ->
                    val item = results.getJSONObject(i)
                    val placeId = item.optString("place_id", "unknown_id")
                    val name = item.optString("name", "Unknown Restaurant")
                    val rating = item.optDouble("rating", 0.0)
                    val address = item.optString("vicinity", "No address available")
                    val priceLevel = item.optInt("price_level", 10)
                    val userRatingsTotal = item.optInt("user_ratings_total", 0)
                    val location = item.getJSONObject("geometry").getJSONObject("location")
                    val latitude = location.optDouble("lat", 0.0)
                    val longitude = location.optDouble("lng", 0.0)

                    val photosArray = item.optJSONArray("photos")
                    val photoReferences = mutableListOf<String>()

                    if (photosArray != null) {
                        for (j in 0 until photosArray.length()) {
                            val ref = photosArray.optJSONObject(j)?.optString("photo_reference")
                            if (!ref.isNullOrEmpty()) photoReferences.add(ref)
                        }
                    }

                    val photoReference = photosArray?.optJSONObject(0)?.optString("photo_reference")

                    Restaurant(
                        placeId = placeId,
                        name = name,
                        rating = rating,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        photo_reference = photoReference,
                        photoReferences = photoReferences,
                        priceLevel = priceLevel,
                        userRatingsTotal = userRatingsTotal
                    )
                }

                allRestaurants.addAll(restaurants)

                nextPageToken = jsonObject.optString("next_page_token", null)
                if (nextPageToken != null) {
                    url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                            "?location=${userLocation.latitude},${userLocation.longitude}" +
                            "&radius=$distance" +
                            "&type=restaurant" +
                            "&key=$apiKey" +
                            "&pagetoken=$nextPageToken"
                    Thread.sleep(2000) // delay for token activation
                }

            } while (nextPageToken != null && allRestaurants.size < 32)

            val sortedRestaurants = when (effectiveSortBy) {
                2 -> allRestaurants.sortedBy {
                    val latDiff = it.latitude - userLocation.latitude
                    val lngDiff = it.longitude - userLocation.longitude
                    Math.sqrt(latDiff * latDiff + lngDiff * lngDiff)
                }
                1 -> allRestaurants.sortedByDescending { it.rating }
                3 -> allRestaurants.sortedBy { it.priceLevel }
                4 -> allRestaurants.sortedByDescending { it.userRatingsTotal }
                else -> allRestaurants
            }

            return@withContext sortedRestaurants.take(32)
        } catch (e: Exception) {
            Log.d("RestaurantRepository", "Error fetching restaurants")
            e.printStackTrace()
            return@withContext emptyList()
        }
    }


    suspend fun fetchRestaurantDetails(placeId: String): Restaurant? {
        val url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=$placeId&fields=" +
                "name,rating,formatted_address,geometry,photos,price_level," +
                "user_ratings_total,opening_hours,business_status" +
                "&key=$apiKey"

        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == 200) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }

                    val json = JSONObject(response)
                    val result = json.optJSONObject("result") ?: return@withContext null

                    val name = result.optString("name")
                    val rating = result.optDouble("rating", 0.0)
                    val address = result.optString("formatted_address")
                    val userRatingsTotal = result.optInt("user_ratings_total")
                    val priceLevel = result.optInt("price_level", -1).takeIf { it >= 0 }

                    val location = result
                        .optJSONObject("geometry")
                        ?.optJSONObject("location")
                    val lat = location?.optDouble("lat") ?: return@withContext null
                    val lng = location.optDouble("lng")

                    val photosArray = result.optJSONArray("photos")
                    val photoReferences = photosArray?.let { array ->
                        List(array.length()) { i ->
                            array.optJSONObject(i)?.optString("photo_reference") ?: ""
                        }.filter { it.isNotBlank() }
                    }

                    val photo_reference = photoReferences?.firstOrNull() // główne zdjęcie

                    val businessStatus = result.optString("business_status", null)
                    val isOpenNow = result
                        .optJSONObject("opening_hours")
                        ?.optBoolean("open_now")

                    val openingHours = result
                        .optJSONObject("opening_hours")
                        ?.optJSONArray("weekday_text")
                        ?.let { array ->
                            List(array.length()) { i -> array.getString(i) }
                        }

                    return@withContext Restaurant(
                        placeId = placeId,
                        name = name,
                        rating = rating,
                        address = address,
                        latitude = lat,
                        longitude = lng,
                        photo_reference = photo_reference,
                        photoReferences = photoReferences,
                        priceLevel = priceLevel,
                        userRatingsTotal = userRatingsTotal,
                        businessStatus = businessStatus,
                        isOpenNow = isOpenNow,
                        openingHours = openingHours
                    )
                } else {
                    Log.e("RestaurantRepository", "HTTP ${connection.responseCode}: Niepowodzenie pobierania szczegółów")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("RestaurantRepository", "Wyjątek w fetchRestaurantDetails", e)
                return@withContext null
            }
        }
    }
}
