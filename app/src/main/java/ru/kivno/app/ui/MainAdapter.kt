package ru.kivno.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ru.kivno.app.R
import ru.kivno.app.model.Film
import ru.kivno.app.model.Person

class MainAdapter(
    private val onFilmClick: (Film) -> Unit,
    private val onVoteClick: (Film, Button) -> Unit,
    private val onPersonClick: (Person) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER    = 0
        const val TYPE_RANK      = 1
        const val TYPE_FILM_CARD = 2
        const val TYPE_PERSON    = 3
    }

    private val items = mutableListOf<Any>()

    data class SectionHeader(val title: String, val sub: String = "")
    data class RankItem(val film: Film, val rank: Int)

    fun setData(top: List<Film>, daily: List<Film>, total: Int) {
        items.clear()
        // Рейтинг
        items.add(SectionHeader("Рейтинг"))
        items.add(SectionHeader("💩💩 Полное КИВНО", "Топ 1–5"))
        top.take(5).forEachIndexed { i, f -> items.add(RankItem(f, i + 1)) }
        items.add(SectionHeader("💩 КИВНО", "Топ 6–10"))
        top.drop(5).take(5).forEachIndexed { i, f -> items.add(RankItem(f, i + 6)) }
        // Голосование
        if (daily.isNotEmpty()) {
            val dateLabel = daily.firstOrNull()?.premiereRu?.let { formatDate(it) } ?: ""
            items.add(SectionHeader("🎬 В кино", dateLabel))
            daily.forEach { items.add(it) }
        }
        notifyDataSetChanged()
    }

    fun setArchive(films: List<Film>) {
        items.clear()
        items.add(SectionHeader("Архив фильмов"))
        films.forEach { items.add(it) }
        notifyDataSetChanged()
    }

    fun setSearchResults(query: String, films: List<Film>, people: List<Person>) {
        items.clear()
        items.add(SectionHeader("Поиск", "Найдено по запросу: $query"))

        if (films.isNotEmpty()) {
            items.add(SectionHeader("🎬 Фильмы", "${films.size}"))
            films.forEach { items.add(it) }
        }

        if (people.isNotEmpty()) {
            items.add(SectionHeader("👤 Кивноделы", "${people.size}"))
            people.forEach { items.add(it) }
        }

        if (films.isEmpty() && people.isEmpty()) {
            items.add(SectionHeader("Ничего не найдено", "Попробуйте другой запрос"))
        }

        notifyDataSetChanged()
    }

fun updatePoop(filmId: Int, newCount: Int) {
    val pos = items.indexOfFirst { it is Film && it.id == filmId }
    if (pos >= 0) {
        val old = items[pos] as Film
        items[pos] = old.copy(poopCount = newCount)
        notifyItemChanged(pos)
    }
}

    override fun getItemViewType(position: Int) = when (items[position]) {
        is SectionHeader -> TYPE_HEADER
        is RankItem      -> TYPE_RANK
        is Person        -> TYPE_PERSON
        else             -> TYPE_FILM_CARD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER    -> HeaderVH(inflater.inflate(R.layout.item_section_header, parent, false))
            TYPE_RANK      -> RankVH(inflater.inflate(R.layout.item_rank, parent, false))
            TYPE_PERSON    -> PersonVH(inflater.inflate(R.layout.item_person_card, parent, false))
            else           -> FilmVH(inflater.inflate(R.layout.item_film_card, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SectionHeader -> (holder as HeaderVH).bind(item)
            is RankItem      -> (holder as RankVH).bind(item)
            is Film          -> (holder as FilmVH).bind(item)
            is Person        -> (holder as PersonVH).bind(item)
        }
    }

    override fun getItemCount() = items.size

    // ── ViewHolders ──────────────────────────────────────────

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.txtSectionTitle)
        private val sub   = view.findViewById<TextView>(R.id.txtSectionSub)
        fun bind(h: SectionHeader) {
            title.text = h.title
            sub.text = h.sub
            sub.visibility = if (h.sub.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    inner class RankVH(view: View) : RecyclerView.ViewHolder(view) {
        private val rank  = view.findViewById<TextView>(R.id.txtRank)
        private val img   = view.findViewById<android.widget.ImageView>(R.id.imgRank)
        private val title = view.findViewById<TextView>(R.id.txtRankTitle)
        private val poop  = view.findViewById<TextView>(R.id.txtRankPoop)

        fun bind(item: RankItem) {
            rank.text  = "#${item.rank}"
            title.text = item.film.title
            poop.text  = "💩 ${item.film.poopCount}"
            Glide.with(img).load(item.film.poster)
                .placeholder(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(img)
            itemView.setOnClickListener { onFilmClick(item.film) }
        }
    }


    inner class PersonVH(view: View) : RecyclerView.ViewHolder(view) {
        private val img   = view.findViewById<android.widget.ImageView>(R.id.imgPerson)
        private val name  = view.findViewById<TextView>(R.id.txtPersonName)
        private val role  = view.findViewById<TextView>(R.id.txtPersonRole)
        private val poop  = view.findViewById<TextView>(R.id.txtPersonPoop)

        fun bind(person: Person) {
            name.text = person.name
            role.text = listOfNotNull(person.alias?.takeIf { it.isNotBlank() }, person.profession).joinToString(" · ")
            poop.text = "💩 ${person.poopCount}"

            Glide.with(img)
                .load(person.photo)
                .placeholder(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(img)

            itemView.setOnClickListener { onPersonClick(person) }
        }
    }

    inner class FilmVH(view: View) : RecyclerView.ViewHolder(view) {
        private val img       = view.findViewById<android.widget.ImageView>(R.id.imgPoster)
        private val badge     = view.findViewById<TextView>(R.id.txtBadge)
        private val title     = view.findViewById<TextView>(R.id.txtTitle)
        private val meta      = view.findViewById<TextView>(R.id.txtMeta)
        private val progress  = view.findViewById<android.widget.ProgressBar>(R.id.progressPoop)
        private val poopCount = view.findViewById<TextView>(R.id.txtPoopCount)
        private val poopPct   = view.findViewById<TextView>(R.id.txtPoopPct)
        private val btnVote   = view.findViewById<Button>(R.id.btnVote)

        fun bind(film: Film) {
            Glide.with(img).load(film.poster)
                .placeholder(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(img)

            val metaParts = listOfNotNull(film.year, film.genre).filter { it.isNotBlank() }
            meta.text      = metaParts.joinToString(" · ")
            title.text     = film.title
            progress.progress = film.percent
            poopCount.text = "💩 ${film.poopCount}"
            poopPct.text   = "${film.percent}%"

            if (!film.age.isNullOrBlank()) {
                badge.text = film.age; badge.visibility = View.VISIBLE
            } else badge.visibility = View.GONE

            btnVote.text = "💩 Закидать КИВНО"
            btnVote.isEnabled = true
            btnVote.setOnClickListener { btn ->
                animatePoop(btn)
                onVoteClick(film, btnVote)
            }
            itemView.setOnClickListener { onFilmClick(film) }
        }

        private fun animatePoop(anchor: View) {
            val ctx = anchor.context
            repeat(8) { i ->
                val poop = TextView(ctx).apply {
                    text = "💩"
                    textSize = 22f
                }
                val rootView = (anchor.rootView as ViewGroup)
                val loc = IntArray(2).also { anchor.getLocationInWindow(it) }
                poop.x = loc[0].toFloat() + anchor.width * (0.2f + Math.random().toFloat() * 0.6f)
                poop.y = loc[1].toFloat()
                rootView.addView(poop)

                val targetY = loc[1].toFloat() - 300 - (Math.random() * 200).toFloat()
                val targetX = poop.x + (-100 + Math.random().toFloat() * 200)

                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(poop, "translationX", 0f, targetX - poop.x),
                        ObjectAnimator.ofFloat(poop, "translationY", 0f, targetY - poop.y),
                        ObjectAnimator.ofFloat(poop, "alpha", 1f, 0f),
                        ObjectAnimator.ofFloat(poop, "scaleX", 0.5f, 1.3f, 0f),
                        ObjectAnimator.ofFloat(poop, "scaleY", 0.5f, 1.3f, 0f),
                        ObjectAnimator.ofFloat(poop, "rotation", 0f, (360 * (2 + Math.random() * 2)).toFloat())
                    )
                    duration = (500 + i * 60).toLong()
                    startDelay = (i * 50).toLong()
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }.also {
                    anchor.postDelayed({ rootView.removeView(poop) }, 1000L)
                }
            }
        }
    }

    private fun formatDate(iso: String): String {
        if (iso.isBlank()) return ""
        return try {
            val months = arrayOf("","января","февраля","марта","апреля","мая","июня",
                                 "июля","августа","сентября","октября","ноября","декабря")
            val parts = iso.split("-")
            "${parts[2].trimStart('0')} ${months[parts[1].toInt()]} ${parts[0]}"
        } catch (e: Exception) { iso }
    }
}
