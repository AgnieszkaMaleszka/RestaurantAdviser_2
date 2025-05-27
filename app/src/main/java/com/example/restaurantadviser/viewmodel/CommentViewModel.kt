package com.example.restaurantadviser.viewmodel
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.restaurantadviser.Comment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CommentViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _comments = mutableStateOf<List<Comment>>(emptyList())
    val comments: State<List<Comment>> get() = _comments
    private var currentPlaceId: String? = null
    private var commentsListener: ListenerRegistration? = null

    fun loadComments(placeId: String) {
        currentPlaceId = placeId

        // Usuń poprzedniego listenera jeśli istniał
        commentsListener?.remove()

        commentsListener = db.collection("comments")
            .whereEqualTo("placeId", placeId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _comments.value = snapshot.documents.mapNotNull { doc ->
                        val comment = doc.toObject(Comment::class.java)
                        comment?.copy(id = doc.id)
                    }
                }
            }
    }
    override fun onCleared() {
        super.onCleared()
        commentsListener?.remove()
    }

    suspend fun analyzeComment(comment: String): List<Pair<String, String>> {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)     // łączenie z serwerem
            .writeTimeout(20, TimeUnit.SECONDS)       // wysyłanie zapytania
            .readTimeout(600, TimeUnit.SECONDS)        // oczekiwanie na odpowiedź!
            .build()


        val json = JSONObject()
        json.put("text", comment)

        val body = json.toString().toRequestBody("application/json".toMediaType())


        val request = Request.Builder()
            .url("https://EfektMotyla-ABSA-REST-API.hf.space/analyze")
            .post(body)
            .build()
        Log.d("CommentViewModel", "Pytanie do API : ${request}")
        Log.d("CommentViewModel", "Pytanie do API : ${json}")

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("CommentViewModel", "Nieudana odpowiedź serwera: ${response.code}")
                        return@withContext emptyList()
                    }

                    val responseBody = response.body?.string() ?: return@withContext emptyList()
                    val jsonObject = JSONObject(responseBody)
                    val results = jsonObject.getJSONArray("results")
                    Log.e("CommentViewModel", "Udana odp z api : ${results}")
                    List(results.length()) { i ->
                        val item = results.getJSONObject(i)
                        item.getString("aspect") to item.getString("sentiment")
                    }
                }
            } catch (e: Exception) {
                Log.e("CommentViewModel", "Błąd połączenia z API: ${e.message}")
                return@withContext emptyList()
            }
        }
    }

    fun addComment(placeId: String, text: String, authorName: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("CommentViewModel", "Nie można dodać komentarza – użytkownik niezalogowany.")
            return
        }

        viewModelScope.launch {
            val comment = Comment(
                text = text,
                authorId = uid,
                authorName = authorName,
                timestamp = System.currentTimeMillis(),
                likes = emptyList(),
                placeId = placeId,
                aspects = emptyList() //  na start pusto
            )

            val data = hashMapOf(
                "text" to comment.text,
                "authorId" to comment.authorId,
                "authorName" to comment.authorName,
                "timestamp" to comment.timestamp,
                "likes" to comment.likes,
                "placeId" to comment.placeId,
                "aspects" to comment.aspects
            )

            // Dodaj komentarz raz
            db.collection("comments")
                .add(data)
                .addOnSuccessListener { documentRef ->
                    println("Komentarz został dodany: $text")
                    currentPlaceId?.let { loadComments(it) }

                    // Analiza komentarza w tle
                    viewModelScope.launch {
                        val aspects = analyzeComment(text)
                        if (aspects.isNotEmpty()) {
                            val aspectsList = aspects.map { (aspect, sentiment) ->
                                mapOf("aspect" to aspect, "sentiment" to sentiment)
                            }

                            Log.d("CommentViewModel", "Aspekty do zapisania: $aspectsList")

                            delay(2000)
                            documentRef.set(mapOf("aspects" to aspectsList), SetOptions.merge())
                                .addOnSuccessListener {
                                    Log.d("Aspekty zaktualizowane.", "${aspectsList}")
                                }

                                .addOnFailureListener { e ->
                                    println("Błąd aktualizacji aspektów: ${e.message}")
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    println("Błąd podczas dodawania komentarza: ${e.message}")
                }
        }
    }



    fun deleteComment(commentId: String) {
        db.collection("comments").document(commentId)
            .delete()
            .addOnSuccessListener {
                currentPlaceId?.let { loadComments(it) }
            }
    }

    fun toggleLike(comment: Comment) {
        val userId = auth.currentUser?.uid ?: return
        val newLikes = if (comment.likes.contains(userId)) {
            comment.likes - userId
        } else {
            comment.likes + userId
        }

        db.collection("comments").document(comment.id)
            .update("likes", newLikes)
            .addOnSuccessListener {
                currentPlaceId?.let { loadComments(it) }
            }
    }
    fun editComment(commentId: String, newText: String) {
        db.collection("comments").document(commentId)
            .update("text", newText)
            .addOnSuccessListener {
                currentPlaceId?.let { loadComments(it) }

                // Analiza aspektów po edycji
                viewModelScope.launch {
                    val aspects = analyzeComment(newText)
                    if (aspects.isNotEmpty()) {
                        val aspectsList = aspects.map { (aspect, sentiment) ->
                            mapOf("aspect" to aspect, "sentiment" to sentiment)
                        }

                        db.collection("comments").document(commentId)
                            .update("aspects", aspectsList)
                            .addOnSuccessListener {
                                println("Aspekty zaktualizowane po edycji.")
                                currentPlaceId?.let { loadComments(it) }
                            }
                            .addOnFailureListener { e ->
                                println("Błąd przy zapisie aspektów: ${e.message}")
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                println("Błąd edycji komentarza: ${e.message}")
            }
    }

}
