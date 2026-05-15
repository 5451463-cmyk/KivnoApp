package ru.kivno.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.kivno.app.api.ApiService
import ru.kivno.app.databinding.ActivityMainBinding
import ru.kivno.app.model.Film

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var searchJob: Job? = null

    private val adapter = MainAdapter(
        onFilmClick = { film -> openFilm(film) },
        onVoteClick = { film, btn -> voteFilm(film, btn) },
        onPersonClick = { openPeople() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerMain.layoutManager = LinearLayoutManager(this)
        binding.recyclerMain.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(ru.kivno.app.R.color.terra)
        binding.swipeRefresh.setOnRefreshListener { loadData() }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                handleSearchText(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                ru.kivno.app.R.id.nav_main    -> { loadData(); true }
                ru.kivno.app.R.id.nav_archive -> { loadArchive(); true }
                ru.kivno.app.R.id.nav_people  -> {
                    startActivity(Intent(this, PeopleActivity::class.java))
                    true
                }
                else -> false
            }
        }

        loadData()
    }

    private fun handleSearchText(text: String) {
        searchJob?.cancel()
        val query = text.trim()

        if (query.length < 2) {
            loadData()
            return
        }

        searchJob = lifecycleScope.launch {
            delay(350)
            try {
                val result = ApiService.search(query)
                adapter.setSearchResults(query, result.films, result.people)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val top   = ApiService.getTopFilms()
                val daily = ApiService.getDailyFilms()
                adapter.setData(top.films, daily.films, top.total)
            } catch (e: Exception) {
                showError()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadArchive() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val archive = ApiService.getArchive()
                adapter.setArchive(archive.films)
            } catch (e: Exception) {
                showError()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun openPeople() {
        startActivity(Intent(this, PeopleActivity::class.java))
    }

    private fun openFilm(film: Film) {
        startActivity(
            Intent(this, FilmActivity::class.java).putExtra("film_id", film.id)
        )
    }

    private fun voteFilm(film: Film, btn: android.widget.Button) {
        btn.isEnabled = false
        btn.text = "💩 Летит…"
        val deviceId = android.provider.Settings.Secure.getString(
            contentResolver, android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        lifecycleScope.launch {
            try {
                val result = ApiService.vote("film", film.id, 5, deviceId)
                if (result.ok) {
                    btn.text = "✓ +${result.added} КИВНО!"
                    adapter.updatePoop(film.id, result.poopCount)
                } else {
                    btn.text = result.message ?: "💩 Лимит"
                    btn.isEnabled = true
                }
            } catch (e: Exception) {
                btn.text = "💩 КИВНО"
                btn.isEnabled = true
                showError()
            }
        }
    }

    private fun showError() {
        Snackbar.make(binding.root,
            getString(ru.kivno.app.R.string.error_network),
            Snackbar.LENGTH_SHORT).show()
    }
}
