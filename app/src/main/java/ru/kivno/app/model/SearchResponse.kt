package ru.kivno.app.model

data class SearchResponse(
    val ok: Boolean,
    val query: String?,
    val films: List<Film> = emptyList(),
    val people: List<Person> = emptyList()
)
