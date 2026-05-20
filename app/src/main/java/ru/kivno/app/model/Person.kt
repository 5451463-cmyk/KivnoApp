package ru.kivno.app.model

import com.google.gson.annotations.SerializedName

data class Person(
    val id: Int = 0,
    val name: String = "",
    val alias: String? = null,
    val profession: String? = null,
    val photo: String? = null,
    val biography: String? = null,
    val filmography: String? = null,
    @SerializedName("poop_count") val poopCount: Int = 0
)

data class PeopleResponse(
    val ok: Boolean = false,
    val people: List<Person> = emptyList()
)
