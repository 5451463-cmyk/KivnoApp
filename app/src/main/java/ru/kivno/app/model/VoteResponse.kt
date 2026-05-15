package ru.kivno.app.model

import com.google.gson.annotations.SerializedName

data class VoteResponse(
    val ok: Boolean = false,
    val message: String? = null,
    val added: Int = 0,
    val left: Int = 0,
    @SerializedName("poop_count") val poopCount: Int = 0
)
