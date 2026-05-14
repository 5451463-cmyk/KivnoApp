package ru.kivno.app.model

import com.google.gson.annotations.SerializedName

data class Film(
    val id: Int,
    val title: String,
    val original: String?,
    val year: String?,
    val genre: String?,
    val age: String?,
    val poster: String?,
    @SerializedName("poop_count") val poopCount: Int = 0,
    @SerializedName("poop_pct")   val poopPct: Int = 0,
    @SerializedName("premiere_ru") val premiereRu: String?,
    @SerializedName("is_weekly")  val isWeekly: Boolean = false,
    val synopsis: String? = null,
    @SerializedName("short_desc") val shortDesc: String? = null,
    val slogan: String? = null,
    val duration: String? = null,
    val country: String? = null,
    @SerializedName("poop_day")   val poopDay: Int = 0,
    @SerializedName("poop_week")  val poopWeek: Int = 0,
    @SerializedName("poop_month") val poopMonth: Int = 0,
    @SerializedName("trailer_url") val trailerUrl: String? = null,
    @SerializedName("kinopoisk_url") val kinopoiskUrl: String? = null,
    val crew: Map<String, List<Person>>? = null,
    val stills: List<String>? = null
)

data class FilmsResponse(
    val ok: Boolean,
    val films: List<Film>,
    val total: Int
)

data class FilmResponse(
    val ok: Boolean,
    val film: Film?
)
