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
import java.net.URLEncoder

object ApiService {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
            }
        }
        .build()

    private val base = BuildConfig.BASE_URL

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(base + path).build()
        client.newCall(req).execute().body!!.string()
    }

    private suspend fun post(path: String, params: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder().apply {
                params.forEach { (k, v) -> add(k, v) }
            }.build()
            val req = Request.Builder().url(base + path).post(body).build()
            client.newCall(req).execute().body!!.string()
        }

    suspend fun getDailyFilms(): FilmsResponse =
        gson.fromJson(get("api_films.php?type=daily"), FilmsResponse::class.java)

    suspend fun getTopFilms(): FilmsResponse =
        gson.fromJson(get("api_films.php?type=top"), FilmsResponse::class.java)

    suspend fun getArchive(page: Int = 1): FilmsResponse =
        gson.fromJson(get("api_films.php?type=archive&page=$page"), FilmsResponse::class.java)

    suspend fun getFilm(id: Int): FilmResponse =
        gson.fromJson(get("api_film.php?id=$id"), FilmResponse::class.java)

    suspend fun getPeople(role: String = ""): PeopleResponse =
        gson.fromJson(get("api_people.php?role=$role"), PeopleResponse::class.java)

    suspend fun search(query: String): SearchResponse {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return gson.fromJson(get("api_search.php?q=$encoded"), SearchResponse::class.java)
    }

    suspend fun vote(type: String, id: Int, amount: Int, deviceId: String): VoteResponse =
        gson.fromJson(
            post("api_vote.php", mapOf(
                "type" to type,
                "id" to id.toString(),
                "amount" to amount.toString(),
                "device_id" to deviceId
            )),
            VoteResponse::class.java
        )
}
