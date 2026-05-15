package ru.kivno.app.ui

import android.animation.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.kivno.app.R
import ru.kivno.app.api.ApiService
import ru.kivno.app.model.Person

class PersonActivity : AppCompatActivity() {

    private lateinit var rootLayout: LinearLayout
    private var currentPerson: Person? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        scroll.addView(rootLayout)
        setContentView(scroll)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val personId = intent.getIntExtra("person_id", 0)
        val personName = intent.getStringExtra("person_name") ?: "Кивнодел"
        supportActionBar?.title = personName

        if (personId == 0) { buildFromIntent(); return }
        loadPerson(personId)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }

    private fun buildFromIntent() {
        // Показываем базовую инфо из Intent пока грузится
        val name   = intent.getStringExtra("person_name") ?: ""
        val role   = intent.getStringExtra("person_role") ?: ""
        val photo  = intent.getStringExtra("person_photo") ?: ""
        val poop   = intent.getIntExtra("person_poop", 0)
        buildUI(null, name, role, photo, poop, "", "")
    }

    private fun loadPerson(id: Int) {
        // Показываем загрузку через getPeople по id (используем список)
        lifecycleScope.launch {
            try {
                val resp = ApiService.getPeople()
                val person = resp.people.firstOrNull { it.id == id }
                if (person != null) {
                    currentPerson = person
                    buildUI(person, person.name, person.profession ?: "",
                        person.photo ?: "", person.poopCount, "", "")
                }
            } catch (e: Exception) {
                buildFromIntent()
            }
        }
    }

    private fun buildUI(person: Person?, name: String, role: String,
                        photo: String, poopCount: Int, bio: String, filmography: String) {
        rootLayout.removeAllViews()
        val pad = dp(16)

        // ── Фото + имя ───────────────────────────────────────────────
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad * 2, pad, pad)
        }

        val imgView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(120), dp(120)).apply {
                bottomMargin = dp(14)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
        }
        header.addView(imgView)

        if (photo.isNotBlank()) {
            Glide.with(this).load(photo)
                .transform(CircleCrop())
                .placeholder(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgView)
        }

        header.addView(TextView(this).apply {
            text = name; textSize = 22f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.dark)); gravity = Gravity.CENTER
        })
        if (role.isNotBlank()) {
            header.addView(TextView(this).apply {
                text = role; textSize = 14f; setTextColor(getColor(R.color.muted))
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })
        }
        header.addView(TextView(this).apply {
            text = "💩 $poopCount в антирейтинге"; textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.poop_brown))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })

        rootLayout.addView(header)

        // ── Кнопка голосования ───────────────────────────────────────
        val btnCard = makeCard()
        val voteLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val sliderLabel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val labelTxt = TextView(this).apply {
            text = "Степень разочарования:"; textSize = 13f
            setTextColor(getColor(R.color.poop_brown))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val sliderVal = TextView(this).apply {
            text = "5/10"; textSize = 14f
            setTextColor(getColor(R.color.terra))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        sliderLabel.addView(labelTxt); sliderLabel.addView(sliderVal)
        voteLayout.addView(sliderLabel)

        val seekBar = SeekBar(this).apply {
            max = 9; progress = 4
            progressTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.terra))
            thumbTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.terra))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8); bottomMargin = dp(14) }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                    sliderVal.text = "${p + 1}/10"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        voteLayout.addView(seekBar)

        val voteBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle).apply {
            text = "💩 Закидать КИВНО"; textSize = 16f
            setBackgroundColor(getColor(R.color.terra))
            setTextColor(getColor(R.color.white))
            cornerRadius = dp(28)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        voteLayout.addView(voteBtn)
        btnCard.addView(voteLayout)
        rootLayout.addView(btnCard)

        val poop0 = intArrayOf(poopCount) // mutable ref

        voteBtn.setOnClickListener {
            val amount = seekBar.progress + 1
            castVote(person?.id ?: intent.getIntExtra("person_id", 0),
                amount, voteBtn, poop0, header)
        }

        // ── Пространство снизу ───────────────────────────────────────
        rootLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(80))
        })
    }

    private fun castVote(personId: Int, amount: Int, btn: MaterialButton,
                         poopRef: IntArray, header: LinearLayout) {
        if (personId == 0) return
        btn.isEnabled = false; btn.text = "💩 Летит…"
        launchPoops(btn)

        val deviceId = android.provider.Settings.Secure.getString(
            contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"

        lifecycleScope.launch {
            try {
                val res = ApiService.vote("person", personId, amount, deviceId)
                if (res.ok) {
                    btn.text = "✓ +${res.added} КИВНО!"
                    poopRef[0] = res.poopCount
                    // Обновляем счётчик в хедере
                    val poopTv = header.getChildAt(3) as? TextView
                    poopTv?.text = "💩 ${res.poopCount} в антирейтинге"
                } else {
                    btn.text = res.message ?: "💩 Лимит 24ч"
                }
            } catch (e: Exception) {
                btn.text = "💩 Закидать КИВНО"; btn.isEnabled = true
                Snackbar.make(rootLayout,
                    getString(R.string.error_network), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchPoops(anchor: View) {
        val root = window.decorView as ViewGroup
        val loc = IntArray(2).also { anchor.getLocationInWindow(it) }
        repeat(8) { i ->
            anchor.postDelayed({
                val poop = TextView(this).apply {
                    text = "💩"; textSize = 24f
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                root.addView(poop)
                poop.x = loc[0] + anchor.width * (0.2f + Math.random().toFloat() * 0.6f)
                poop.y = loc[1].toFloat()
                val ty = poop.y - dp(200) - (Math.random() * dp(100)).toFloat()
                val tx = poop.x + ((-1 + Math.random() * 2) * dp(80)).toFloat()
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(poop, "x", poop.x, tx),
                        ObjectAnimator.ofFloat(poop, "y", poop.y, ty),
                        ObjectAnimator.ofFloat(poop, "alpha", 1f, 0f),
                        ObjectAnimator.ofFloat(poop, "scaleX", 0.5f, 1.3f, 0.3f),
                        ObjectAnimator.ofFloat(poop, "scaleY", 0.5f, 1.3f, 0.3f),
                        ObjectAnimator.ofFloat(poop, "rotation", 0f,
                            if (Math.random() > 0.5) 360f else -360f)
                    )
                    duration = 600 + i * 50L
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
                root.postDelayed({ root.removeView(poop) }, 950)
            }, i * 60L)
        }
    }

    private fun makeCard(): MaterialCardView = MaterialCardView(this).apply {
        radius = dp(20).toFloat(); elevation = dp(4).toFloat()
        setCardBackgroundColor(getColor(R.color.card))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dp(16); rightMargin = dp(16)
            topMargin = dp(8); bottomMargin = dp(8)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
