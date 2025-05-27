package com.example.restaurantadviser

data class Comment(
    val id: String = "",
    val text: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: List<String> = emptyList(),
    val placeId: String = "",
    val aspects: List<Map<String, String>> = emptyList()
)
