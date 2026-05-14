package ru.kivno.app.ui

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class StillsAdapter(private val stills: List<String>) :
    RecyclerView.Adapter<StillsAdapter.VH>() {

    inner class VH(val img: ImageView) : RecyclerView.ViewHolder(img)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val density = parent.context.resources.displayMetrics.density
        val w = (280 * density).toInt()
        val h = (158 * density).toInt()
        val img = ImageView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(w, h).apply {
                marginEnd = (10 * density).toInt()
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
        }
        return VH(img)
    }

    override fun getItemCount() = stills.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        Glide.with(holder.img)
            .load(stills[position])
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.img)
    }
}
