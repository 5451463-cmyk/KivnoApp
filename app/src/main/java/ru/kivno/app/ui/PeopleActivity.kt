package ru.kivno.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.launch
import ru.kivno.app.R
import ru.kivno.app.api.ApiService
import ru.kivno.app.model.Person

class PeopleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(context)
            val pad = (80 * resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, pad)
            clipToPadding = false
        }
        setContentView(recycler)

        supportActionBar?.title = "👤 Кивноделы"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch {
            try {
                val resp = ApiService.getPeople()
                recycler.adapter = PeopleAdapter(resp.people)
            } catch (e: Exception) { /* показываем пустой список */ }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

class PeopleAdapter(private val people: List<Person>) :
    RecyclerView.Adapter<PeopleAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img  = view.findViewById<ImageView>(R.id.imgPerson)
        val name = view.findViewById<TextView>(R.id.txtPersonName)
        val role = view.findViewById<TextView>(R.id.txtPersonRole)
        val poop = view.findViewById<TextView>(R.id.txtPersonPoop)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_person_card, parent, false))

    override fun getItemCount() = people.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = people[position]
        holder.name.text = p.name
        holder.role.text = listOfNotNull(
            p.alias?.takeIf { it.isNotBlank() },
            p.profession?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        holder.poop.text = "💩 ${p.poopCount}"

        if (!p.photo.isNullOrBlank()) {
            Glide.with(holder.img)
                .load(p.photo)
                .transform(CircleCrop())
                .placeholder(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.img)
        }
    }
}
