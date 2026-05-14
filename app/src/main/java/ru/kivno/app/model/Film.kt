package ru.kivno.app.model

import com.google.gson.annotations.SerializedName

// Ответ api_films.php (список)
data class Film(
    val id: Int = 0,
    val title: String = "",
    val original: String? = null,
    val year: String? = null,
    val country: String? = null,
    val genre: String? = null,
    val age: String? = null,
    val poster: String? = null,
    @SerializedName("poop_count") val poopCount: Int = 0,
    val percent: Int = 0,          // из api_films.php
    @SerializedName("short_desc")  val shortDesc: String? = null,
    @SerializedName("is_weekly_new") val isWeeklyNew: Int = 0,

    // Только в api_film.php (детали)
    val synopsis: String? = null,
    val slogan: String? = null,
    val duration: String? = null,
    @SerializedName("premiere_ru")    val premiereRu: String? = null,
    @SerializedName("premiere_world") val premiereWorld: String? = null,
    val budget: String? = null,
    @SerializedName("box_office_world") val boxOfficeWorld: String? = null,
    @SerializedName("box_office_ru")    val boxOfficeRu: String? = null,
    val language: String? = null,
    val studio: String? = null,
    val distributor: String? = null,
    @SerializedName("mpaa_rating")   val mpaaRating: String? = null,
    @SerializedName("kinopoisk_url") val kinopoiskUrl: String? = null,
    @SerializedName("trailer_url")   val trailerUrl: String? = null,
    val stats: FilmStats? = null,              // вложенный объект из api_film.php
    val crew: Map<String, List<Person>>? = null,
    val stills: List<String>? = null
)

// Статистика из api_film.php → "stats": {day, week, month, all}
data class FilmStats(
    val day: Int = 0,
    val week: Int = 0,
    val month: Int = 0,
    val all: Int = 0
)

data class FilmsResponse(
    val ok: Boolean = false,
    val type: String? = null,
    val films: List<Film> = emptyList()
)

data class FilmResponse(
    val ok: Boolean = false,
    val film: Film? = null
)
