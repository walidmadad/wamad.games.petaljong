package com.wamad.petaljong.presentation

import android.content.Context
import android.graphics.*
import android.view.*
import android.widget.*
import com.wamad.petaljong.R

/** A touch-modal spotlight: only the highlighted real control receives a gesture. */
class TutorialOverlay(
    context: Context, private val target: View?, private val targetBounds: (() -> RectF)?,
    title: Int, detail: Int, action: Int?, skipText: Int, proceed: () -> Unit, skip: () -> Unit
) : FrameLayout(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hole = RectF()
    private var forwarding = false
    private val bubble = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(12), dp(18), dp(10))
        background = GardenStyle.surface(context, GardenStyle.panel, 22)
        addView(TextView(context).apply {
            id = R.id.tutorial_title; setText(title); textSize = 20f; setTextColor(GardenStyle.ivory)
            setTypeface(typeface, Typeface.BOLD)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        })
        addView(TextView(context).apply {
            setText(detail); textSize = 16f; setTextColor(GardenStyle.ivory)
            setPadding(0, dp(6), 0, dp(4))
        })
        val buttons = LinearLayout(context).apply {
            gravity = Gravity.END
            if (resources.configuration.screenWidthDp < 360 || resources.configuration.fontScale > 1.2f) orientation = LinearLayout.VERTICAL
        }
        buttons.addView(Button(context).apply {
            id = R.id.tutorial_skip; setText(skipText); GardenStyle.button(this, false)
            minimumHeight = dp(48); setOnClickListener { skip() }
        })
        if (action != null) buttons.addView(Button(context).apply {
            id = R.id.tutorial_action; setText(action); GardenStyle.button(this, true)
            minimumHeight = dp(48); setOnClickListener { proceed() }
        })
        addView(buttons)
    }
    private val scroll = ScrollView(context).apply { isFillViewport = false; addView(bubble) }
    init {
        id = R.id.tutorial_overlay
        tag = "tutorial_overlay"
        setWillNotDraw(false)
        isClickable = true; isFocusable = true
        addView(scroll)
        alpha = 0f
        animate().alpha(1f).setDuration(180).start()
        post { requestLayout() }
    }
    private fun dp(n: Int) = GardenStyle.dp(context, n)
    private fun locate() {
        hole.setEmpty()
        if (target == null) return
        val origin = IntArray(2); val local = IntArray(2)
        getLocationOnScreen(origin); target.getLocationOnScreen(local)
        hole.set(targetBounds?.invoke() ?: RectF(0f, 0f, target.width.toFloat(), target.height.toFloat()))
        hole.offset((local[0] - origin[0]).toFloat(), (local[1] - origin[1]).toFloat())
    }
    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w); val height = MeasureSpec.getSize(h)
        setMeasuredDimension(width, height)
        locate()
        val available = if (hole.isEmpty) height - dp(32) else maxOf(hole.top.toInt() - dp(24), height - hole.bottom.toInt() - dp(24))
        scroll.measure(MeasureSpec.makeMeasureSpec(minOf(width - dp(24), dp(380)), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(maxOf(dp(48), available), MeasureSpec.AT_MOST))
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        locate()
        val x = (width - scroll.measuredWidth) / 2
        val y = if (hole.isEmpty) (height - scroll.measuredHeight) / 2
        else if (hole.top > height - hole.bottom) maxOf(dp(12), hole.top.toInt() - dp(12) - scroll.measuredHeight)
        else minOf(height - scroll.measuredHeight - dp(12), hole.bottom.toInt() + dp(12))
        scroll.layout(x, y, x + scroll.measuredWidth, y + scroll.measuredHeight)
    }
    override fun onDraw(canvas: Canvas) {
        locate()
        val path = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            if (!hole.isEmpty) addRoundRect(hole, dp(10).toFloat(), dp(10).toFloat(), Path.Direction.CW)
        }
        paint.alpha = 255; paint.style = Paint.Style.FILL; paint.color = 0xB0173F35.toInt()
        canvas.drawPath(path, paint)
        if (!hole.isEmpty) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(2).toFloat(); paint.color = GardenStyle.gold
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
                paint.alpha = (190 + 65 * kotlin.math.sin(android.os.SystemClock.uptimeMillis() / 550.0)).toInt()
                postInvalidateOnAnimation()
            }
            canvas.drawRoundRect(hole, dp(10).toFloat(), dp(10).toFloat(), paint)
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) forwarding = target != null && hole.contains(event.x, event.y)
        if (forwarding && target != null) {
            val origin = IntArray(2); val local = IntArray(2)
            getLocationOnScreen(origin); target.getLocationOnScreen(local)
            val copy = MotionEvent.obtain(event)
            copy.offsetLocation((origin[0] - local[0]).toFloat(), (origin[1] - local[1]).toFloat())
            target.dispatchTouchEvent(copy); copy.recycle()
        }
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) forwarding = false
        return true
    }
}
