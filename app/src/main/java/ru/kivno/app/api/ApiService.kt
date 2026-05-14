package ru.kivno.app.api

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import ru.kivno.app.BuildConfig
import ru.kivno.app.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object ApiService {

    private val gson = Gson()

    // BASE_URL = "https://kivno.ru/" (с трейлинг слешем)
    private val base = BuildConfig.BASE_URL.trimEnd('/') + "/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val url = if (path.startsWith("http")) path else base + path
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { it.body!!.string() }
    }

    private suspend fun post(path: String, params: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            val url = if (path.startsWith("http")) path else base + path
            val body = FormBody.Builder().apply {
                params.forEach { (k, v) -> add(k, v) }
            }.build()
            val req = Request.Builder().url(url).post(body).build()
            client.newCall(req).execute().use { it.body!!.string() }
        }

    // Фильмы активного дня (is_weekly_new=1, последняя дата)
    suspend fun getDailyFilms(): FilmsResponse =
        gson.fromJson(get("api_films.php?type=daily"), FilmsResponse::class.java)

    // Топ-10 по poop_count
    suspend fun getTopFilms(): FilmsResponse =
        gson.fromJson(get("api_films.php?type=top"), FilmsResponse::class.java)

    // Архив (все фильмы, постранично)
    suspend fun getArchive(page: Int = 1): FilmsResponse =
        gson.fromJson(get("api_films.php?type=archive&page=$page"), FilmsResponse::class.java)

    // Детали одного фильма (включая crew, stills, stats)
    suspend fun getFilm(id: Int): FilmResponse =
        gson.fromJson(get("api_film.php?id=$id"), FilmResponse::class.java)

    // Список людей
    suspend fun getPeople(role: String = ""): PeopleResponse {
        val path = if (role.isBlank()) "api_people.php" else "api_people.php?role=$role"
        return gson.fromJson(get(path), PeopleResponse::class.java)
    }

    // Голосование
    suspend fun vote(type: String, id: Int, amount: Int, deviceId: String): VoteResponse =
        gson.fromJson(
            post("api_vote.php", mapOf(
                "type"      to type,
                "id"        to id.toString(),
                "amount"    to amount.toString(),
                "device_id" to deviceId
            )),
            VoteResponse::class.java
        )
}
