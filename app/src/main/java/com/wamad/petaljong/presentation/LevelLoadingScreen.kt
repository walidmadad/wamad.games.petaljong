package com.wamad.petaljong.presentation

import com.wamad.petaljong.domain.GardenTheme
import com.wamad.petaljong.R
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*

/** Lightweight animation of existing botanical artwork; owned by the visible Activity. */
class LevelLoadingScreen(context: Context, retry: () -> Unit) : ScrollView(context) {
    private val art = GardenWelcomeArt(context)
    private val message = TextView(context)
    private val indicator = ProgressBar(context)
    private val retryButton = Button(context).apply { setText(R.string.loading_retry); setOnClickListener { retry() }; GardenStyle.button(this, true) }
    private var animator: ValueAnimator? = null
    private val density = resources.displayMetrics.density
    init {
        indicator.indeterminateTintList = android.content.res.ColorStateList.valueOf(GardenStyle.gold)
        background = WelcomeGardenDrawable(resources)
        isFillViewport = true; isClickable = true; visibility = View.GONE
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding((24*density).toInt(), (32*density).toInt(), (24*density).toInt(), (32*density).toInt())
        }
        content.addView(TextView(context).apply {
            setText(R.string.app_name); textSize = 34f; gravity = Gravity.CENTER
            setTextColor(GardenTheme.ivory); typeface = Typeface.create("serif", Typeface.NORMAL)
        }, LinearLayout.LayoutParams(-1,-2))
        content.addView(art, LinearLayout.LayoutParams(-1,(220*density).toInt()))
        message.apply { textSize = 20f; gravity = Gravity.CENTER; setTextColor(GardenTheme.ivory); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        content.addView(message, LinearLayout.LayoutParams(-1,-2))
        content.addView(indicator, LinearLayout.LayoutParams((20*density).toInt(),(20*density).toInt()).apply { topMargin = (24*density).toInt() })
        content.addView(retryButton, LinearLayout.LayoutParams(-2,-2))
        addView(content)
    }
    fun present(level: Long, error: Boolean, active: Boolean) {
        // Background spans the window; ScrollView padding protects only its content.
        val label = if (error) context.getString(R.string.loading_error) else context.getString(R.string.loading_level, level)
        if (message.text != label) message.text = label
        retryButton.visibility = if (error) View.VISIBLE else View.GONE
        indicator.visibility = if (error) View.GONE else View.VISIBLE
        stopAnimation()
        if (active && !error && ValueAnimator.areAnimatorsEnabled()) {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1200; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
                addUpdateListener { art.translationY = -8*density*(it.animatedValue as Float); art.alpha = .85f + .15f*(it.animatedValue as Float) }
                start()
            }
        } else if (!ValueAnimator.areAnimatorsEnabled()) indicator.visibility = View.GONE
    }
    fun stopAnimation() { animator?.cancel(); animator = null; art.translationY = 0f; art.alpha = 1f }
    override fun onDetachedFromWindow() { stopAnimation(); super.onDetachedFromWindow() }
}
