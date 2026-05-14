package ru.kivno.app.model

data class VoteResponse(
    val ok: Boolean,
    val message: String?,
    val count: Int = 0,
    val percent: Int = 0,
    val left: Int = 0
)
