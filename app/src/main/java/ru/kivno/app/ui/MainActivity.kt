package ru.kivno.app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.kivno.app.R
import ru.kivno.app.api.ApiService
import ru.kivno.app.databinding.ActivityMainBinding
import ru.kivno.app.model.Film
import ru.kivno.app.model.Person

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var searchJob: Job? = null

    private val adapter = MainAdapter(
        onFilmClick   = { film   -> openFilm(film) },
        onVoteClick   = { film, btn, amount -> voteFilm(film, btn, amount) },
        onPersonClick = { person -> openPerson(person) }   // ← клик на кивнодела
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerMain.layoutManager = LinearLayoutManager(this)
        binding.recyclerMain.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.terra)
        binding.swipeRefresh.setOnRefreshListener {
            when (binding.bottomNav.selectedItemId) {
                R.id.nav_archive -> loadArchive()
                R.id.nav_people  -> loadPeople()
                else             -> loadData()
            }
        }

        // Поиск
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                handleSearch(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Нижняя навигация — всё показывается В ГЛАВНОМ ЭКРАНЕ
        binding.bottomNav.setOnItemSelectedListener { item ->
            binding.edtSearch.text?.clear()
            when (item.itemId) {
                R.id.nav_main    -> { loadData();    true }
                R.id.nav_archive -> { loadArchive(); true }
                R.id.nav_people  -> { loadPeople();  true }  // ← больше не startActivity!
                else             -> false
            }
        }

        loadData()
    }

    // ── Загрузка данных ─────────────────────────────────────────

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val top   = ApiService.getTopFilms()
                val daily = ApiService.getDailyFilms()
                adapter.setData(top.films, daily.films, top.total)
            } catch (e: Exception) { showErr() }
            finally { binding.swipeRefresh.isRefreshing = false }
        }
    }

    private fun loadArchive() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                adapter.setArchive(ApiService.getArchive().films)
            } catch (e: Exception) { showErr() }
            finally { binding.swipeRefresh.isRefreshing = false }
        }
    }

    // ← Кивноделы загружаются в тот же RecyclerView — меню остаётся
    private fun loadPeople() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val resp = ApiService.getPeople()
                adapter.setPeople(resp.people)
            } catch (e: Exception) { showErr() }
            finally { binding.swipeRefresh.isRefreshing = false }
        }
    }

    private fun handleSearch(text: String) {
        searchJob?.cancel()
        val q = text.trim()
        if (q.length < 2) {
            // При пустом поиске возвращаемся к текущей вкладке
            when (binding.bottomNav.selectedItemId) {
                R.id.nav_archive -> loadArchive()
                R.id.nav_people  -> loadPeople()
                else             -> loadData()
            }
            return
        }
        searchJob = lifecycleScope.launch {
            delay(350)
            try {
                val result = ApiService.search(q)
                adapter.setSearchResults(q, result.films, result.people)
            } catch (e: Exception) { showErr() }
        }
    }

    // ── Навигация ───────────────────────────────────────────────

    private fun openFilm(film: Film) {
        startActivity(Intent(this, FilmActivity::class.java).putExtra("film_id", film.id))
    }

    // Кивнодел → PersonActivity (отдельный экран с голосованием)
    private fun openPerson(person: Person) {
        startActivity(Intent(this, PersonActivity::class.java).apply {
            putExtra("person_id",    person.id)
            putExtra("person_name",  person.name)
            putExtra("person_role",  person.profession ?: "")
            putExtra("person_photo", person.photo ?: "")
            putExtra("person_poop",  person.poopCount)
        })
    }

    private fun voteFilm(film: Film, btn: Button, amount: Int) {
        btn.isEnabled = false
        btn.text = "💩 Летит…"
        val deviceId = android.provider.Settings.Secure.getString(
            contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"
        lifecycleScope.launch {
            try {
                val res = ApiService.vote("film", film.id, amount, deviceId)
                if (res.ok) {
                    btn.text = "✓ +${res.added} КИВНО!"
                    adapter.updatePoop(film.id, res.poopCount)
                } else {
                    btn.text = res.message ?: "💩 Лимит"
                    btn.isEnabled = true
                }
            } catch (e: Exception) {
                btn.text = "💩 Закидать КИВНО"
                btn.isEnabled = true
                showErr()
            }
        }
    }

    private fun showErr() = Snackbar.make(binding.root,
        getString(R.string.error_network), Snackbar.LENGTH_SHORT).show()
}
