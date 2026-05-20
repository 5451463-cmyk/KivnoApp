package ru.kivno.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ru.kivno.app.R
import ru.kivno.app.model.Film
import ru.kivno.app.model.Person

class MainAdapter(
    private val onFilmClick: (Film) -> Unit,
    private val onVoteClick: (Film, Button, Int) -> Unit,
    private val onPersonClick: (Person) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HERO   = 0
        const val TYPE_HEADER = 1
        const val TYPE_RANK   = 2
        const val TYPE_FILM   = 3
        const val TYPE_PERSON = 4
    }

    object HeroBlock
    data class SectionHeader(val title: String, val sub: String = "")
    data class RankItem(val film: Film, val rank: Int)

    private val items = mutableListOf<Any>()

    fun setData(top: List<Film>, daily: List<Film>, total: Int = 0) {
        val newItems = mutableListOf<Any>()

        newItems.add(HeroBlock)
        newItems.add(SectionHeader("Рейтинг", "Топ 10"))
        newItems.add(SectionHeader("💩💩 Полное КИВНО", "Топ 1–5"))
        top.take(5).forEachIndexed { i, f -> newItems.add(RankItem(f, i + 1)) }
        newItems.add(SectionHeader("💩 КИВНО", "Топ 6–10"))
        top.drop(5).take(5).forEachIndexed { i, f -> newItems.add(RankItem(f, i + 6)) }

        if (daily.isNotEmpty()) {
            val dateLabel = daily.firstOrNull()?.premiereRu?.let { formatDate(it) } ?: ""
            newItems.add(SectionHeader("Новинки недели", dateLabel))
            daily.forEach { newItems.add(it) }
        }

        items.clear(); items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setArchive(films: List<Film>) {
        val newItems = mutableListOf<Any>()
        newItems.add(SectionHeader("Архив", "${films.size} фильмов"))
        films.forEach { newItems.add(it) }
        items.clear(); items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setPeople(people: List<Person>) {
        val label = when {
            people.size % 100 in 11..19 -> "${people.size} кивноделов"
            people.size % 10 == 1       -> "${people.size} кивнодел"
            people.size % 10 in 2..4    -> "${people.size} кивнодела"
            else                         -> "${people.size} кивноделов"
        }
        val newItems = mutableListOf<Any>()
        newItems.add(SectionHeader("Кивноделы", label))
        people.forEach { newItems.add(it) }
        items.clear(); items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setSearchResults(query: String, films: List<Film>, people: List<Person>) {
        val newItems = mutableListOf<Any>()
        newItems.add(SectionHeader("Поиск", "«$query»"))

        if (films.isNotEmpty()) {
            newItems.add(SectionHeader("Фильмы"))
            films.forEach { newItems.add(it) }
        }
        if (people.isNotEmpty()) {
            newItems.add(SectionHeader("Кивноделы"))
            people.forEach { newItems.add(it) }
        }
        if (films.isEmpty() && people.isEmpty()) {
            newItems.add(SectionHeader("Ничего не найдено", "Попробуйте другой запрос"))
        }

        items.clear(); items.addAll(newItems)
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

    override fun getItemViewType(pos: Int) = when (items[pos]) {
        is HeroBlock     -> TYPE_HERO
        is SectionHeader -> TYPE_HEADER
        is RankItem      -> TYPE_RANK
        is Person        -> TYPE_PERSON
        else             -> TYPE_FILM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HERO   -> HeroVH(inf.inflate(R.layout.item_hero_kivno, parent, false))
            TYPE_HEADER -> HeaderVH(inf.inflate(R.layout.item_section_header, parent, false))
            TYPE_RANK   -> RankVH(inf.inflate(R.layout.item_rank, parent, false))
            TYPE_PERSON -> PersonVH(inf.inflate(R.layout.item_person_card, parent, false))
            else        -> FilmVH(inf.inflate(R.layout.item_film_card, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        when (val item = items[pos]) {
            is HeroBlock     -> Unit
            is SectionHeader -> (holder as HeaderVH).bind(item)
            is RankItem      -> (holder as RankVH).bind(item)
            is Film          -> (holder as FilmVH).bind(item)
            is Person        -> (holder as PersonVH).bind(item)
        }
    }

    override fun getItemCount() = items.size

    inner class HeroVH(v: View) : RecyclerView.ViewHolder(v)

    inner class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        private val title = v.findViewById<TextView>(R.id.txtSectionTitle)
        private val sub   = v.findViewById<TextView>(R.id.txtSectionSub)
        fun bind(h: SectionHeader) {
            title.text = h.title
            sub.text = h.sub
            sub.visibility = if (h.sub.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    inner class RankVH(v: View) : RecyclerView.ViewHolder(v) {
        private val rank  = v.findViewById<TextView>(R.id.txtRank)
        private val img   = v.findViewById<ImageView>(R.id.imgRank)
        private val title = v.findViewById<TextView>(R.id.txtRankTitle)
        private val poop  = v.findViewById<TextView>(R.id.txtRankPoop)
        fun bind(item: RankItem) {
            rank.text  = "#${item.rank}"
            title.text = item.film.title
            poop.text  = "💩 ${item.film.poopCount}"
            Glide.with(img).load(item.film.poster)
                .placeholder(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade()).into(img)
            itemView.setOnClickListener { onFilmClick(item.film) }
        }
    }

    inner class PersonVH(v: View) : RecyclerView.ViewHolder(v) {
        private val img  = v.findViewById<ImageView>(R.id.imgPerson)
        private val name = v.findViewById<TextView>(R.id.txtPersonName)
        private val role = v.findViewById<TextView>(R.id.txtPersonRole)
        private val poop = v.findViewById<TextView>(R.id.txtPersonPoop)
        fun bind(p: Person) {
            name.text = p.name
            role.text = listOfNotNull(
                p.alias?.takeIf { it.isNotBlank() },
                p.profession?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            poop.text = "💩 ${p.poopCount}"
            Glide.with(img).load(p.photo)
                .transform(CircleCrop())
                .placeholder(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade()).into(img)
            itemView.setOnClickListener { onPersonClick(p) }
        }
    }

    inner class FilmVH(v: View) : RecyclerView.ViewHolder(v) {
        private val img      = v.findViewById<ImageView>(R.id.imgPoster)
        private val badge    = v.findViewById<TextView>(R.id.txtBadge)
        private val title    = v.findViewById<TextView>(R.id.txtTitle)
        private val meta     = v.findViewById<TextView>(R.id.txtMeta)
        private val amountTx = v.findViewById<TextView>(R.id.txtAmount)
        private val seek     = v.findViewById<SeekBar>(R.id.seekAmount)
        private val progress = v.findViewById<ProgressBar>(R.id.progressPoop)
        private val count    = v.findViewById<TextView>(R.id.txtPoopCount)
        private val pct      = v.findViewById<TextView>(R.id.txtPoopPct)
        private val btnVote  = v.findViewById<Button>(R.id.btnVote)

        fun bind(film: Film) {
            Glide.with(img).load(film.poster)
                .placeholder(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade()).into(img)

            title.text    = film.title
            meta.text     = listOfNotNull(film.year, film.genre).filter { it.isNotBlank() }.joinToString(" · ")
            progress.progress = film.percent
            count.text    = "${film.poopCount} 💩"
            pct.text      = "${film.percent}% от общего"
            badge.text    = film.age ?: ""
            badge.visibility = if (!film.age.isNullOrBlank()) View.VISIBLE else View.GONE

            seek.progress = 4
            amountTx.text = "5/10"
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    amountTx.text = "${progress + 1}/10"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            btnVote.text = "💩 Закидать КИВНО"
            btnVote.isEnabled = true
            btnVote.setOnClickListener {
                val amount = seek.progress + 1
                animatePoop(btnVote, img, amount)
                onVoteClick(film, btnVote, amount)
            }
            itemView.setOnClickListener { onFilmClick(film) }
        }

        private fun animatePoop(anchor: View, target: View, amount: Int) {
            val root = anchor.rootView as? ViewGroup ?: return
            val aLoc = IntArray(2).also { anchor.getLocationInWindow(it) }
            val tLoc = IntArray(2).also { target.getLocationInWindow(it) }

            repeat(amount.coerceIn(1, 10)) { i ->
                anchor.postDelayed({
                    val poop = TextView(anchor.context).apply {
                        text = "💩"; textSize = 22f
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT)
                    }
                    root.addView(poop)
                    poop.x = aLoc[0] + anchor.width * (0.2f + Math.random().toFloat() * 0.6f)
                    poop.y = aLoc[1].toFloat()
                    val tx = tLoc[0] + target.width  * (0.1f + Math.random().toFloat() * 0.8f)
                    val ty = tLoc[1] + target.height * (0.1f + Math.random().toFloat() * 0.8f)

                    AnimatorSet().apply {
                        playTogether(
                            ObjectAnimator.ofFloat(poop, "x", poop.x, tx.toFloat()),
                            ObjectAnimator.ofFloat(poop, "y", poop.y, ty.toFloat()),
                            ObjectAnimator.ofFloat(poop, "alpha", 1f, 0f),
                            ObjectAnimator.ofFloat(poop, "scaleX", 0.5f, 1.3f, 0f),
                            ObjectAnimator.ofFloat(poop, "scaleY", 0.5f, 1.3f, 0f),
                            ObjectAnimator.ofFloat(poop, "rotation", 0f,
                                (360 * (2 + Math.random() * 2)).toFloat())
                        )
                        duration = (500 + i * 60).toLong()
                        startDelay = (i * 50).toLong()
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                    root.postDelayed({ root.removeView(poop) }, 1000L)
                }, i * 55L)
            }
        }
    }

    private fun formatDate(iso: String): String {
        if (iso.isBlank()) return ""
        return try {
            val months = arrayOf("","января","февраля","марта","апреля","мая","июня",
                "июля","августа","сентября","октября","ноября","декабря")
            val parts = iso.split("-")
            if (parts.size >= 3) "${parts[2].trimStart('0')} ${months[parts[1].toInt()]} ${parts[0]}"
            else iso
        } catch (e: Exception) { iso }
    }
}
