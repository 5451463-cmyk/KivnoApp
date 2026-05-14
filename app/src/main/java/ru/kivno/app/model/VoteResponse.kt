package ru.kivno.app.model

import com.google.gson.annotations.SerializedName

// api_vote.php возвращает: ok, message, added, left, poop_count
data class VoteResponse(
    val ok: Boolean = false,
    val message: String? = null,
    val added: Int = 0,
    val left: Int = 0,
    @SerializedName("poop_count") val poopCount: Int = 0
)
