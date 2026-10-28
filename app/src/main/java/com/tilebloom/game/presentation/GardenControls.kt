package com.tilebloom.game.presentation

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.widget.Button

/** Shared surfaces and hand-drawn vector controls; no icon font or external artwork. */
internal object GardenStyle {
    val forest = Color.rgb(16, 47, 39)
    val panel = Color.rgb(25, 60, 47)
    val ivory = Color.rgb(245, 237, 214)
    val gold = Color.rgb(199, 180, 126)
    val secondary = Color.rgb(185, 204, 181)
    fun dp(context: Context, value: Int) = (context.resources.displayMetrics.density * value).toInt()
    fun surface(context: Context, color: Int, radius: Int = 22, border: Boolean = true) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(context, radius).toFloat()
        if (border) setStroke(dp(context, 1).coerceAtLeast(1), Color.rgb(118, 129, 87))
    }
    fun button(button: Button, primary: Boolean = false) {
        val context = button.context
        button.isAllCaps = false
        button.minimumWidth = dp(context, 56); button.minimumHeight = dp(context, 56)
        button.gravity = Gravity.CENTER
        button.textSize = 17f
        button.setTextColor(if (primary) forest else ivory)
        button.backgroundTintList = null
        button.background = RippleDrawable(ColorStateList.valueOf(Color.argb(55, 245, 237, 214)),
            surface(context, if (primary) gold else panel), surface(context, Color.WHITE))
        button.setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 12))
        button.elevation = dp(context, if (primary) 3 else 1).toFloat()
    }
    fun primaryIcon(button: Button, resource: Int) {
        val icon = button.context.getDrawable(resource)!!.mutate()
        icon.setTint(forest)
        val size = dp(button.context, 24); icon.setBounds(0, 0, size, size)
        // An inline image belongs to the centered text run, rather than the button edge.
        val group = android.text.SpannableString("\uFFFC  " + button.text)
        group.setSpan(CenteredIconSpan(icon),
            0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        button.text = group
    }
}

/** Button remains a single accessible touch target, including its numeric credit badge. */
class GardenIconButton(context: Context, iconResource: Int, private val iconTint: Int = GardenStyle.ivory) : Button(context) {
    private val icon = context.getDrawable(iconResource)!!.mutate()
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    var badge: Int? = null
        set(value) { field = value; invalidate() }
    val showsAdIndicator: Boolean get() = badge == 0
    private val video = context.getDrawable(com.tilebloom.game.R.drawable.ic_play)!!.mutate()
    init {
        GardenStyle.button(this)
        minimumWidth = 0; minimumHeight = 0
        // The outer target reserves room for attached badges inside its own bounds.
        background = null
        text = ""
        setPadding(0, 0, 0, 0)
        isClickable = true
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val cx = width / 2f; val cy = height / 2f
        val radius = minOf(28 * d, width / 2f, height / 2f)
        ink.style = Paint.Style.FILL
        ink.color = if (isPressed) Color.rgb(49, 80, 59) else GardenStyle.forest
        canvas.drawCircle(cx, cy, radius - d, ink)
        ink.style = Paint.Style.STROKE; ink.strokeWidth = d
        ink.color = Color.rgb(118, 129, 87)
        canvas.drawCircle(cx, cy, radius - d, ink)
        ink.style = Paint.Style.FILL
        val size = GardenStyle.dp(context, 24)
        icon.setTint(if (isEnabled) iconTint else Color.rgb(137, 157, 137))
        icon.setBounds((cx - size / 2).toInt(), (cy - size / 2).toInt(),
            (cx + size / 2).toInt(), (cy + size / 2).toInt())
        icon.draw(canvas)
        badge?.let {
            ink.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            ink.textSize = 12 * resources.displayMetrics.scaledDensity
            val h = maxOf(20 * d, ink.fontSpacing + 2 * d)
            val w = maxOf(h, ink.measureText(it.toString()) + 6 * d)
            val right = width - d; val top = 0f
            ink.color = GardenStyle.gold
            canvas.drawRoundRect(right - w, top, right, h, h / 2, h / 2, ink)
            ink.style = Paint.Style.STROKE; ink.strokeWidth = 1.5f * d; ink.color = GardenStyle.forest
            canvas.drawRoundRect(right - w, top, right, h, h / 2, h / 2, ink)
            ink.style = Paint.Style.FILL
            canvas.drawText(it.toString(), right - w / 2 - ink.measureText(it.toString()) / 2,
                h / 2 - (ink.ascent() + ink.descent()) / 2, ink)
        }
        if (showsAdIndicator) {
            val label = context.getString(com.tilebloom.game.R.string.ad_label)
            ink.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            ink.textSize = 11 * resources.displayMetrics.scaledDensity
            val videoSize = 12 * d
            val w = videoSize + ink.measureText(label) + 10 * d
            val h = maxOf(20 * d, ink.fontSpacing + 2 * d)
            val left = cx - w / 2; val top = height - h
            ink.color = GardenStyle.gold
            canvas.drawRoundRect(left, top, left + w, height.toFloat(), h / 2, h / 2, ink)
            ink.style = Paint.Style.STROKE; ink.strokeWidth = 1.5f * d; ink.color = GardenStyle.forest
            canvas.drawRoundRect(left, top, left + w, height.toFloat(), h / 2, h / 2, ink)
            ink.style = Paint.Style.FILL
            video.setTint(GardenStyle.forest)
            video.setBounds((left + 3*d).toInt(), (top + (h-videoSize)/2).toInt(),
                (left + 3*d + videoSize).toInt(), (top + (h+videoSize)/2).toInt())
            video.draw(canvas)
            ink.color = GardenStyle.forest
            canvas.drawText(label, left + videoSize + 5*d,
                top + h/2 - (ink.ascent()+ink.descent())/2, ink)
        }
    }
}

/** Align fixed-size vector art to the label's typographic center at every font scale. */
private class CenteredIconSpan(drawable: android.graphics.drawable.Drawable) : android.text.style.ImageSpan(drawable) {
    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val fm = paint.fontMetricsInt
        canvas.save()
        canvas.translate(x, y + (fm.ascent + fm.descent - drawable.bounds.height()) / 2f)
        drawable.draw(canvas)
        canvas.restore()
    }
}
