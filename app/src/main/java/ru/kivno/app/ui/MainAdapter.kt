package ru.kivno.app.ui

import android.animation.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ru.kivno.app.R
import ru.kivno.app.model.Film

class MainAdapter(
    private val onFilmClick: (Film) -> Unit,
    private val onVoteClick: (Film, Button) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_RANK   = 1
        const val TYPE_CARD   = 2
    }

    data class Header(val title: String, val sub: String = "")
    data class RankItem(val film: Film, val rank: Int)

    private val items = mutableListOf<Any>()

    // Фиксим ConcurrentModificationException — строим отдельный список, потом присваиваем
    fun setData(top: List<Film>, daily: List<Film>, total: Int) {
        val newItems = mutableListOf<Any>()
        newItems.add(Header("Рейтинг"))
        newItems.add(Header("💩💩 Полное КИВНО"))
        top.take(5).forEachIndexed  { i, f -> newItems.add(RankItem(f, i + 1)) }
        newItems.add(Header("💩 КИВНО"))
        top.drop(5).take(5).forEachIndexed { i, f -> newItems.add(RankItem(f, i + 6)) }

        if (daily.isNotEmpty()) {
            val dateLabel = daily.firstOrNull()?.premiereRu?.let { formatDate(it) } ?: ""
            newItems.add(Header("🎬 В кино", dateLabel))
            daily.forEach { newItems.add(it) }
        }
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setArchive(films: List<Film>) {
        val newItems = mutableListOf<Any>()
        newItems.add(Header("Архив фильмов"))
        films.forEach { newItems.add(it) }
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updatePoop(filmId: Int, count: Int, pct: Int) {
        val pos = items.indexOfFirst { it is Film && it.id == filmId }
        if (pos >= 0) {
            items[pos] = (items[pos] as Film).copy(poopCount = count, poopPct = pct)
            notifyItemChanged(pos)
        }
    }

    override fun getItemViewType(pos: Int) = when (items[pos]) {
        is Header   -> TYPE_HEADER
        is RankItem -> TYPE_RANK
        else        -> TYPE_CARD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(inf.inflate(R.layout.item_section_header, parent, false))
            TYPE_RANK   -> RankVH(inf.inflate(R.layout.item_rank, parent, false))
            else        -> CardVH(inf.inflate(R.layout.item_film_card, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        when (val item = items[pos]) {
            is Header   -> (holder as HeaderVH).bind(item)
            is RankItem -> (holder as RankVH).bind(item)
            is Film     -> (holder as CardVH).bind(item)
        }
    }

    override fun getItemCount() = items.size

    // ── ViewHolders ──────────────────────────────────────────

    inner class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        private val title = v.findViewById<TextView>(R.id.txtSectionTitle)
        private val sub   = v.findViewById<TextView>(R.id.txtSectionSub)
        fun bind(h: Header) {
            title.text = h.title
            sub.text = h.sub
            sub.visibility = if (h.sub.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    inner class RankVH(v: View) : RecyclerView.ViewHolder(v) {
        private val num   = v.findViewById<TextView>(R.id.txtRank)
        private val img   = v.findViewById<ImageView>(R.id.imgRank)
        private val title = v.findViewById<TextView>(R.id.txtRankTitle)
        private val poop  = v.findViewById<TextView>(R.id.txtRankPoop)
        fun bind(item: RankItem) {
            num.text   = "#${item.rank}"
            title.text = item.film.title
            poop.text  = "💩 ${item.film.poopCount}"
            Glide.with(img).load(item.film.poster)
                .placeholder(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade()).into(img)
            itemView.setOnClickListener { onFilmClick(item.film) }
        }
    }

    inner class CardVH(v: View) : RecyclerView.ViewHolder(v) {
        private val img      = v.findViewById<ImageView>(R.id.imgPoster)
        private val badge    = v.findViewById<TextView>(R.id.txtBadge)
        private val title    = v.findViewById<TextView>(R.id.txtTitle)
        private val meta     = v.findViewById<TextView>(R.id.txtMeta)
        private val progress = v.findViewById<ProgressBar>(R.id.progressPoop)
        private val count    = v.findViewById<TextView>(R.id.txtPoopCount)
        private val pct      = v.findViewById<TextView>(R.id.txtPoopPct)
        private val btn      = v.findViewById<Button>(R.id.btnVote)

        fun bind(film: Film) {
            Glide.with(img).load(film.poster)
                .placeholder(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade()).into(img)

            title.text = film.title
            meta.text  = listOfNotNull(film.year, film.genre).filter { it.isNotBlank() }.joinToString(" · ")
            progress.progress = film.poopPct
            count.text = "💩 ${film.poopCount}"
            pct.text   = "${film.poopPct}%"

            badge.text = film.age ?: ""
            badge.visibility = if (!film.age.isNullOrBlank()) View.VISIBLE else View.GONE

            btn.text = "💩 Закидать КИВНО"
            btn.isEnabled = true
            btn.setOnClickListener {
                animatePoopFromBtn(btn, img)
                onVoteClick(film, btn)
            }
            itemView.setOnClickListener { onFilmClick(film) }
        }

        private fun animatePoopFromBtn(anchor: View, target: View) {
            val root = anchor.rootView as? ViewGroup ?: return
            val anchorLoc = IntArray(2).also { anchor.getLocationInWindow(it) }
            val targetLoc = IntArray(2).also { target.getLocationInWindow(it) }

            repeat(7) { i ->
                anchor.postDelayed({
                    val poop = TextView(anchor.context).apply {
                        text = "💩"; textSize = 22f
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    root.addView(poop)
                    poop.x = anchorLoc[0] + anchor.width * (0.2f + Math.random().toFloat() * 0.6f)
                    poop.y = anchorLoc[1].toFloat()

                    val tx = targetLoc[0] + target.width  * (0.1f + Math.random().toFloat() * 0.8f)
                    val ty = targetLoc[1] + target.height * (0.1f + Math.random().toFloat() * 0.8f)

                    AnimatorSet().apply {
                        playTogether(
                            ObjectAnimator.ofFloat(poop, "x", poop.x, tx.toFloat()),
                            ObjectAnimator.ofFloat(poop, "y", poop.y, ty.toFloat()),
                            ObjectAnimator.ofFloat(poop, "alpha", 1f, 0f),
                            ObjectAnimator.ofFloat(poop, "scaleX", 0.4f, 1.1f, 0.2f),
                            ObjectAnimator.ofFloat(poop, "scaleY", 0.4f, 1.1f, 0.2f),
                            ObjectAnimator.ofFloat(poop, "rotation",
                                0f, (if (Math.random() > 0.5) 480f else -480f))
                        )
                        duration = 550 + i * 55L
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                    root.postDelayed({ root.removeView(poop) }, 900)
                }, i * 60L)
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
