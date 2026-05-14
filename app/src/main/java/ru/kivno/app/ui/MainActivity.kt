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
        onFilmClick = { film -> startActivity(Intent(this, FilmActivity::class.java).putExtra("film_id", film.id)) },
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
                    startActivity(Intent(this, PeopleActivity::class.java)); true
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
                adapter.setData(top.films, daily.films)
            } catch (e: Exception) {
                showErr()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadArchive() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                adapter.setArchive(ApiService.getArchive().films)
            } catch (e: Exception) {
                showErr()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun voteFilm(film: Film, btn: Button) {
        btn.isEnabled = false; btn.text = "💩 Летит…"
        val dev = android.provider.Settings.Secure.getString(
            contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"
        lifecycleScope.launch {
            try {
                val res = ApiService.vote("film", film.id, 5, dev)
                if (res.ok) {
                    btn.text = "✓ +${res.added}!"
                    adapter.updatePoop(film.id, res.poopCount, film.percent)
                } else {
                    btn.text = res.message ?: "💩 Лимит"; btn.isEnabled = true
                }
            } catch (e: Exception) {
                btn.text = "💩 Закидать КИВНО"; btn.isEnabled = true; showErr()
            }
        }
    }

    private fun showErr() = Snackbar.make(binding.root,
        getString(R.string.error_network), Snackbar.LENGTH_SHORT).show()
}
