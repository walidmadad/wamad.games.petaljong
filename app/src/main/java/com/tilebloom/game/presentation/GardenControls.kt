package com.tilebloom.game.presentation

import com.tilebloom.game.presentation.board.HomeTheme
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.widget.Button

/** Shared surfaces and hand-drawn vector controls; no icon font or external artwork. */
internal object GardenStyle {
    var theme = com.tilebloom.game.domain.GardenTheme.forLevel(1)
    val forest get() = theme.palette.backgroundTop
    val panel get() = theme.palette.headerSurface
    val ivory get() = theme.palette.textPrimary
    val gold get() = theme.palette.primaryAccent
    val secondary get() = theme.palette.textSecondary
    fun dp(context: Context, value: Int) = (context.resources.displayMetrics.density * value).toInt()
    fun surface(context: Context, color: Int, radius: Int = 22, border: Boolean = true) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(context, radius).toFloat()
        if (border) setStroke(dp(context, 1).coerceAtLeast(1), theme.palette.border)
    }
    fun button(button: Button, primary: Boolean = false) {
        val context = button.context
        button.isSoundEffectsEnabled = false
        button.isAllCaps = false
        button.minimumWidth = dp(context, 56); button.minimumHeight = dp(context, 56)
        button.gravity = Gravity.CENTER
        button.textSize = 17f
        button.setTextColor(if (primary) forest else ivory)
        button.backgroundTintList = null
        button.background = RippleDrawable(ColorStateList.valueOf((ivory and 0x00ffffff) or (55 shl 24)),
            surface(context, if (primary) gold else panel), surface(context, Color.WHITE))
        button.setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 12))
        button.elevation = dp(context, if (primary) 3 else 1).toFloat()
    }
    fun primaryIcon(button: Button, resource: Int, tint: Int = forest) {
        val icon = button.context.getDrawable(resource)!!.mutate()
        icon.layoutDirection = button.context.resources.configuration.layoutDirection
        icon.setTint(tint)
        val size = dp(button.context, 24); icon.setBounds(0, 0, size, size)
        // An inline image belongs to the centered text run, rather than the button edge.
        val group = android.text.SpannableString("\uFFFC  " + button.text)
        group.setSpan(CenteredIconSpan(icon),
            0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        button.text = group
    }
}

/** Button remains a single accessible touch target, including its numeric credit badge. */
class GardenIconButton(context: Context, iconResource: Int, private val iconTint: Int? = null) : Button(context) {
    private val icon = context.getDrawable(iconResource)!!.mutate().apply { layoutDirection = context.resources.configuration.layoutDirection }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    var badge: Int? = null
        set(value) { field = value; invalidate() }
    val showsAdIndicator: Boolean get() = badge == 0
    init {
        GardenStyle.button(this)
        minimumWidth = 0; minimumHeight = 0
        // The outer target reserves room for attached badges inside its own bounds.
        background = null
        text = ""
        setPadding(0, 0, 0, 0)
        isClickable = true
    }
    private fun adLayout(availableWidth: Int): android.text.StaticLayout {
        val paint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12 * resources.displayMetrics.scaledDensity
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            color = GardenStyle.forest
        }
        val label = context.getString(com.tilebloom.game.R.string.ad_label)
        return android.text.StaticLayout.Builder.obtain(label, 0, label.length, paint,
            availableWidth.coerceAtLeast(1)).setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false).build()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = resources.displayMetrics.density
        val sp = resources.displayMetrics.scaledDensity
        val desired = (60 * d + maxOf(24 * d, 20 * sp + 8 * d) / 2 +
            maxOf(24 * d, adLayout(android.view.View.MeasureSpec.getSize(widthMeasureSpec) - (10 * d).toInt()).height + 8 * d) + 6 * d).toInt()
        super.onMeasure(widthMeasureSpec, android.view.View.MeasureSpec.makeMeasureSpec(
            resolveSize(desired, heightMeasureSpec), android.view.View.MeasureSpec.EXACTLY))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val badgeHeight = maxOf(24 * d, 20 * resources.displayMetrics.scaledDensity + 8 * d)
        val cy = if (height < 80 * d) height / 2f
            else badgeHeight / 2 + 30 * d
        val radius = minOf(30 * d, width / 2f - d, height / 2f)
        ink.style = Paint.Style.FILL
        ink.color = if (isPressed) GardenStyle.theme.palette.traySurface else GardenStyle.theme.palette.buttonSurface
        canvas.drawCircle(cx, cy, radius - d, ink)
        ink.style = Paint.Style.STROKE; ink.strokeWidth = d
        ink.color = GardenStyle.theme.palette.border
        canvas.drawCircle(cx, cy, radius - d, ink)
        ink.style = Paint.Style.FILL
        val size = GardenStyle.dp(context, 28)
        icon.setTint(if (isEnabled) iconTint ?: GardenStyle.ivory else GardenStyle.secondary)
        icon.setBounds((cx - size / 2).toInt(), (cy - size / 2).toInt(),
            (cx + size / 2).toInt(), (cy + size / 2).toInt())
        icon.draw(canvas)
        badge?.let {
            ink.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            ink.textSize = 14 * resources.displayMetrics.scaledDensity
            val h = maxOf(24 * d, ink.fontSpacing + 6 * d)
            val w = maxOf(h, ink.measureText(it.toString()) + 10 * d)
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
            // Long localized labels wrap at the user's font size inside this column.
            val label = adLayout(width - (10 * d).toInt())
            val h = maxOf(24 * d, label.height + 8 * d)
            val left = d; val top = height - h
            ink.color = GardenStyle.gold
            canvas.drawRoundRect(left, top, width - d, height.toFloat(), 10 * d, 10 * d, ink)
            canvas.save()
            canvas.translate(5 * d, top + (h - label.height) / 2)
            label.draw(canvas)
            canvas.restore()
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

/** One focusable target for the entire saved-level card, including its decoration. */
class ContinueLevelCard(context: Context) : android.widget.LinearLayout(context) {
    val levelLabel = android.widget.TextView(context).apply {
        textSize = 26f; gravity = Gravity.CENTER; maxLines = 2
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(HomeTheme.palette.primaryAccent)
    }
    val continuationLabel = android.widget.TextView(context).apply {
        textSize = 16f; gravity = Gravity.CENTER
        setTextColor(HomeTheme.palette.textPrimary)
        setPadding(0, GardenStyle.dp(context, 8), 0, 0)
        setText(com.tilebloom.game.R.string.continue_garden)
    }
    fun present(level: Long) {
        levelLabel.text = context.getString(com.tilebloom.game.R.string.start_level, level)
        contentDescription = context.getString(com.tilebloom.game.R.string.continue_level_description, level)
    }
    init {
        orientation = VERTICAL; gravity = Gravity.CENTER
        setPadding(GardenStyle.dp(context, 18), GardenStyle.dp(context, 20),
            GardenStyle.dp(context, 18), GardenStyle.dp(context, 20))
        addView(levelLabel, LayoutParams(-1, -2))
        addView(continuationLabel, LayoutParams(-1, -2))
        isClickable = true
        isFocusable = true
        isSoundEffectsEnabled = false
        minimumHeight = GardenStyle.dp(context, 112)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        background = RippleDrawable(ColorStateList.valueOf(0x22E8CF91),
            GradientDrawable().apply {
                setColor(HomeTheme.palette.headerSurface)
                cornerRadius = GardenStyle.dp(context, 22).toFloat()
                setStroke(GardenStyle.dp(context, 1).coerceAtLeast(1), HomeTheme.palette.border)
            }, GardenStyle.surface(context, Color.WHITE, border = false))
    }

    override fun onViewAdded(child: android.view.View) {
        super.onViewAdded(child)
        child.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    override fun getAccessibilityClassName(): CharSequence = Button::class.java.name

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        animate().scaleX(if (pressed) .985f else 1f)
            .scaleY(if (pressed) .985f else 1f).setDuration(100).start()
    }
}
