package ru.kivno.app.model

import com.google.gson.annotations.SerializedName

data class Person(
    val id: Int,
    val name: String,
    val alias: String?,
    val profession: String?,
    val photo: String?,
    @SerializedName("poop_count") val poopCount: Int = 0
)

data class PeopleResponse(
    val ok: Boolean,
    val people: List<Person>
)
