package ru.kivno.app.ui

import android.animation.*
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.kivno.app.R
import ru.kivno.app.api.ApiService
import ru.kivno.app.databinding.ActivityFilmBinding
import ru.kivno.app.model.Film

class FilmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilmBinding
    private var currentFilm: Film? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.seekAmount.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, user: Boolean) {
                binding.txtSliderValue.text = "${p + 1}/10"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnVoteFilm.setOnClickListener {
            currentFilm?.let { castVote(it, binding.seekAmount.progress + 1) }
        }

        val filmId = intent.getIntExtra("film_id", 0)
        if (filmId == 0) { finish(); return }
        loadFilm(filmId)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }

    private fun loadFilm(id: Int) {
        lifecycleScope.launch {
            try {
                val resp = ApiService.getFilm(id)
                resp.film?.let { bindFilm(it) } ?: showError(id)
            } catch (e: Exception) {
                showError(id)
            }
        }
    }

    private fun bindFilm(f: Film) {
        currentFilm = f
        binding.collapsingToolbar.title = f.title

        Glide.with(this).load(f.poster)
            .placeholder(android.R.color.darker_gray)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imgPosterHero)

        val meta = listOfNotNull(f.year, f.country, f.genre).filter { it.isNotBlank() }
        binding.txtGenre.text = meta.joinToString(" · ")

        // stats — вложенный объект из api_film.php
        binding.statDayValue.text   = (f.stats?.day   ?: 0).toString()
        binding.statWeekValue.text  = (f.stats?.week  ?: 0).toString()
        binding.statMonthValue.text = (f.stats?.month ?: 0).toString()
        binding.statAllValue.text   = (f.stats?.all   ?: f.poopCount).toString()

        // Синопсис
        val synopsis = listOfNotNull(f.synopsis, f.shortDesc).firstOrNull { !it.isNullOrBlank() }
        if (synopsis != null) {
            binding.txtSynopsis.text = synopsis
            binding.cardSynopsis.visibility = View.VISIBLE
        }

        // Кадры
        val stills = f.stills?.filter { it.isNotBlank() } ?: emptyList()
        if (stills.isNotEmpty()) {
            binding.recyclerStills.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerStills.adapter = StillsAdapter(stills)
            binding.cardStills.visibility = View.VISIBLE
        }

        // Создатели (crew из api_film.php)
        val roleNames = mapOf(
            "director" to "Режиссёр", "writer" to "Сценарист",
            "actor"    to "Актёры",   "producer" to "Продюсер",
            "composer" to "Композитор"
        )
        val crew = f.crew?.filter { it.value.isNotEmpty() }
        if (!crew.isNullOrEmpty()) {
            crew.forEach { (role, people) ->
                val label = TextView(this).apply {
                    text = roleNames[role] ?: role
                    textSize = 12f; setTextColor(getColor(R.color.terra))
                    setPadding(0, 12, 0, 2)
                }
                val value = TextView(this).apply {
                    text = people.joinToString(", ") { it.name }
                    textSize = 14f; setTextColor(getColor(R.color.text))
                    setPadding(0, 0, 0, 8)
                }
                binding.crewContainer.addView(label)
                binding.crewContainer.addView(value)
            }
            binding.cardCrew.visibility = View.VISIBLE
        }
    }

    private fun castVote(film: Film, amount: Int) {
        binding.btnVoteFilm.isEnabled = false
        binding.btnVoteFilm.text = "💩 Летит…"
        launchPoops(amount)

        val deviceId = android.provider.Settings.Secure.getString(
            contentResolver, android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        lifecycleScope.launch {
            try {
                val res = ApiService.vote("film", film.id, amount, deviceId)
                if (res.ok) {
                    binding.btnVoteFilm.text = "✓ +${res.added} засчитано!"
                    binding.statAllValue.text = res.poopCount.toString()
                    // left — сколько осталось голосов сегодня
                    if (res.left <= 0) binding.btnVoteFilm.text = "✓ Лимит 24ч"
                } else {
                    binding.btnVoteFilm.text = res.message ?: "💩 Лимит 24ч"
                }
            } catch (e: Exception) {
                binding.btnVoteFilm.text = "💩 Закидать КИВНО"
                binding.btnVoteFilm.isEnabled = true
                Snackbar.make(binding.coordLayout,
                    getString(R.string.error_network), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchPoops(count: Int) {
        val root = binding.coordLayout
        val btnLoc = IntArray(2).also { binding.btnVoteFilm.getLocationInWindow(it) }
        val imgLoc = IntArray(2).also { binding.imgPosterHero.getLocationInWindow(it) }

        repeat(count.coerceAtMost(10)) { i ->
            root.postDelayed({
                val poop = TextView(this).apply {
                    text = "💩"; textSize = 28f
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                root.addView(poop)
                poop.x = btnLoc[0] + binding.btnVoteFilm.width * (0.3f + Math.random().toFloat() * 0.4f)
                poop.y = btnLoc[1].toFloat()

                val tx = imgLoc[0] + binding.imgPosterHero.width  * (0.1f + Math.random().toFloat() * 0.8f)
                val ty = imgLoc[1] + binding.imgPosterHero.height * (0.2f + Math.random().toFloat() * 0.6f)

                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(poop, "x", poop.x, tx.toFloat()),
                        ObjectAnimator.ofFloat(poop, "y", poop.y, ty.toFloat()),
                        ObjectAnimator.ofFloat(poop, "alpha", 1f, 0.8f, 0f),
                        ObjectAnimator.ofFloat(poop, "scaleX", 0.4f, 1.2f, 0.5f),
                        ObjectAnimator.ofFloat(poop, "scaleY", 0.4f, 1.2f, 0.5f),
                        ObjectAnimator.ofFloat(poop, "rotation", 0f,
                            if (Math.random() > 0.5) 540f else -540f)
                    )
                    duration = 650 + i * 40L
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
                root.postDelayed({ root.removeView(poop) }, 1000)
            }, i * 65L)
        }
    }

    private fun showError(id: Int) {
        Snackbar.make(binding.coordLayout,
            getString(R.string.error_network), Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) { loadFilm(id) }.show()
    }
}
