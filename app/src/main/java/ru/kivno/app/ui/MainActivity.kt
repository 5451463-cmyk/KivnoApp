package ru.kivno.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.kivno.app.R
import ru.kivno.app.api.ApiService
import ru.kivno.app.databinding.ActivityMainBinding
import ru.kivno.app.model.Film

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = MainAdapter(
        onFilmClick = { film -> openFilm(film) },
        onVoteClick = { film, btn -> voteFilm(film, btn) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerMain.layoutManager = LinearLayoutManager(this)
        binding.recyclerMain.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.terra)
        binding.swipeRefresh.setOnRefreshListener { loadData() }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_main    -> { loadData(); true }
                R.id.nav_archive -> { loadArchive(); true }
                R.id.nav_people  -> {
                    startActivity(Intent(this, PeopleActivity::class.java))
                    true
                }
                else -> false
            }
        }

        loadData()
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val top   = ApiService.getTopFilms()
                val daily = ApiService.getDailyFilms()
                adapter.setData(top.films, daily.films, top.total)
            } catch (e: Exception) {
                Snackbar.make(binding.root,
                    getString(R.string.error_network), Snackbar.LENGTH_SHORT).show()
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
                Snackbar.make(binding.root,
                    getString(R.string.error_network), Snackbar.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun openFilm(film: Film) {
        startActivity(Intent(this, FilmActivity::class.java).putExtra("film_id", film.id))
    }

    private fun voteFilm(film: Film, btn: Button) {
        btn.isEnabled = false
        btn.text = "💩 Летит…"
        val deviceId = android.provider.Settings.Secure.getString(
            contentResolver, android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        lifecycleScope.launch {
            try {
                val result = ApiService.vote("film", film.id, 5, deviceId)
                if (result.ok) {
                    btn.text = getString(R.string.voted_ok)
                    adapter.updatePoop(film.id, result.count, result.percent)
                } else {
                    btn.text = result.message ?: "💩 Кивно"
                    btn.isEnabled = true
                }
            } catch (e: Exception) {
                btn.text = "💩 Закидать КИВНО"
                btn.isEnabled = true
                Snackbar.make(binding.root,
                    getString(R.string.error_network), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
