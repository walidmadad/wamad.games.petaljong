package com.tilebloom.game.presentation

import android.animation.ValueAnimator
import android.os.SystemClock
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.tilebloom.game.R
import com.tilebloom.game.domain.*
import com.tilebloom.game.presentation.board.*
import kotlin.math.*

class MahjongBoardView(context: Context, private var game: Game, private val changed: () -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val effects = MatchEffects()
    private val hintEffect = HintPresentation()
    val hintedTileId get() = hintEffect.targetId
    private val hintBounds = RectF()
    private var hintIndicator: BoardPoint? = null
    private val expireHint = Runnable { clearHint(); invalidate() }
    fun showHint(id: Int) {
        if (!hintEffect.show(id, game, SystemClock.uptimeMillis())) return
        hintIndicator = null
        postDelayed(expireHint, HintPresentation.DURATION_MS)
        invalidate()
    }
    private fun clearHint() { removeCallbacks(expireHint); hintEffect.clear(); hintIndicator = null }

    val activeMatchEffectCount get() = effects.active.size
    private val gesture = TileTapGesture(ViewConfiguration.get(context).scaledTouchSlop.toFloat())
    private var blockedTileId: Int? = null
    private val clearBlocked = Runnable { blockedTileId = null; invalidate() }
    private fun geometry() = BoardGeometry(width.toFloat(), height.toFloat(),
        BoardPadding(paddingLeft.toFloat(), paddingTop.toFloat(), paddingRight.toFloat(), paddingBottom.toFloat()), game.tiles)
    // Read-only presentation queries also allow instrumentation to assert real touch results.
    val selectedTileId get() = game.selected
    val remainingTileCount get() = game.remaining.size
    fun tileCenter(id: Int): BoardPoint? = game.remaining.find { it.id == id }?.let { geometry().face(it).center }
    init {
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        refresh()
    }
    fun load(next: Game) { clearFeedback(); game = next; refresh() }
    fun refresh() {
        hintEffect.validate(game, SystemClock.uptimeMillis())
        if (hintedTileId == null) removeCallbacks(expireHint)
        contentDescription = context.getString(R.string.collection_board_description, remainingTileCount, game.tray.size)
        invalidate()
    }
    fun clearFeedback() {
        clearHint()
        removeCallbacks(clearBlocked)
        blockedTileId = null
        gesture.cancel()
        effects.clear()
        invalidate()
    }
    private fun BoardRect.android() = RectF(left, top, right, bottom)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = geometry()
        val scale = layout.scale
        if (scale <= 0f) return
        val now = SystemClock.uptimeMillis()
        if (!ValueAnimator.areAnimatorsEnabled()) effects.clear()
        effects.expire(now)
        // Merge ghosts into the original draw order; they never enter touch geometry.
        val ghosts = effects.active.associateBy { it.tile.id }
        for (tile in layout.drawOrder(game.remaining + effects.active.map { it.tile })) {
            val ghost = ghosts[tile.id]
            val progress = ghost?.let { effects.progress(it, now) } ?: 0f
            val r = layout.footprint(tile).android()
            val face = layout.face(tile)
            val available = ghost != null || game.isAvailable(tile.id)
            val saved = if (ghost != null) {
                canvas.saveLayerAlpha(RectF(r.left, r.top, r.right, r.bottom + scale * .1f),
                    ((1f - progress) * 255).toInt())
            } else canvas.save()
            if (ghost != null) {
                val shrink = 1f - .1f * progress
                canvas.scale(shrink, shrink, r.centerX(), r.centerY())
            }
            val selected = available && game.selected == tile.id
            paint.style = Paint.Style.FILL
            // Two offset shadow bands: soft penumbra and a stronger contact shadow.
            paint.color = Color.argb(if (tile.layer > 0) 48 else 28, 3, 20, 15)
            canvas.drawRoundRect(layout.shadow(tile).offset(scale * .045f).android(), layout.radius * 1.2f, layout.radius * 1.2f, paint)
            paint.color = Color.argb(if (tile.layer > 0) 115 else 75, 3, 20, 15)
            canvas.drawRoundRect(layout.shadow(tile).android(), layout.radius, layout.radius, paint)
            paint.color = Color.rgb(151, 139, 108)
            canvas.drawRoundRect(layout.body(tile).android(), layout.radius, layout.radius, paint)
            paint.color = Color.rgb(208, 197, 163)
            canvas.drawRoundRect(layout.edge(tile).android(), layout.radius, layout.radius, paint)
            paint.color = if (available) Color.rgb(250, 244, 224) else Color.rgb(153, 163, 139)
            canvas.drawRoundRect(face.android(), layout.radius, layout.radius, paint)
            // Inset ceramic rim and top highlight stay inside the established face.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = scale * .018f
            paint.color = if (available) Color.rgb(255, 253, 240) else Color.rgb(182, 190, 164)
            canvas.drawRoundRect(face.inset(scale * .035f).android(), layout.radius * .8f, layout.radius * .8f, paint)
            // Keep all border strokes inside the physical face so selection never changes hit bounds.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (selected) scale * .045f else scale * .014f
            paint.color = when {
                selected -> Color.rgb(193, 148, 48)
                blockedTileId == tile.id -> Color.rgb(192, 170, 111)
                else -> Color.rgb(151, 145, 112)
            }
            canvas.drawRoundRect(face.inset(paint.strokeWidth / 2).android(), layout.radius, layout.radius, paint)
            if (selected) {
                paint.strokeWidth = scale * .012f
                paint.color = Color.rgb(227, 201, 135)
                canvas.drawRoundRect(face.inset(scale * .075f).android(), scale * .06f, scale * .06f, paint)
            }
            paint.style = Paint.Style.FILL
            // Any overhead intersection conceals the artwork conservatively. Ceramic
            // edges remain visible, and opaque upper faces never disclose identity.
            if (TileVisibility.symbolVisible(tile, game.tiles)) {
                canvas.save()
                canvas.translate(r.centerX(), r.centerY())
                canvas.scale(scale * .34f, scale * .34f)
                BotanicalSymbols.draw(canvas, paint, tile.symbol)
                canvas.restore()
                if (!available) {
                    paint.color = Color.argb(65, 100, 115, 89)
                    canvas.drawRoundRect(face.android(), layout.radius, layout.radius, paint)
                }
            }
            if (!available) {
                // Engraved corner bars distinguish blocking without relying on color.
                paint.color = Color.rgb(103, 113, 91); paint.strokeWidth = scale * .025f
                canvas.drawLine(face.left + scale * .12f, face.top + scale * .12f, face.left + scale * .25f, face.top + scale * .12f, paint)
                canvas.drawLine(face.right - scale * .25f, face.top + scale * .12f, face.right - scale * .12f, face.top + scale * .12f, paint)
            }
            canvas.restoreToCount(saved)
        }
        hintEffect.validate(game, now)
        // Foreground decoration shares the authoritative face transform, never hit geometry.
        hintEffect.face(game, layout)?.let { face -> drawHint(canvas, layout, face, now) }
        if (effects.active.isNotEmpty() || (hintedTileId != null && ValueAnimator.areAnimatorsEnabled())) postInvalidateOnAnimation()
    }
    private fun drawHint(canvas: Canvas, layout: BoardGeometry, face: BoardRect, now: Long) {
        val density = resources.displayMetrics.density
        val pulse = hintEffect.brightness(now, ValueAnimator.areAnimatorsEnabled())
        val stroke = 3.5f * density
        hintBounds.set(face.left, face.top, face.right, face.bottom)
        // Stroke outside the ceramic rim keeps botanical artwork untouched.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke + 2.5f * density
        paint.color = Color.rgb(65, 48, 12)
        canvas.drawRoundRect(hintBounds, layout.radius, layout.radius, paint)
        paint.strokeWidth = stroke
        paint.color = Color.rgb(255, (191 + 38 * pulse).toInt(), (32 + 56 * pulse).toInt())
        canvas.drawRoundRect(hintBounds, layout.radius, layout.radius, paint)
        val radius = min(11f * density, layout.scale * .22f)
        val bulb = hintIndicator ?: hintEffect.indicator(face, width.toFloat(), height.toFloat(), radius, game, layout)
            .also { hintIndicator = it }
        paint.strokeWidth = 2f * density
        paint.color = Color.rgb(65, 48, 12)
        val anchorX = if (bulb.x > face.center.x) face.right - layout.radius else face.left + layout.radius
        val anchorY = if (bulb.y > face.center.y) face.bottom else face.top
        canvas.drawLine(bulb.x, bulb.y, anchorX, anchorY, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(bulb.x, bulb.y, radius, paint)
        paint.color = Color.rgb(255, 229, 110)
        canvas.drawCircle(bulb.x, bulb.y, radius * .82f, paint)
        // Original vector bulb: globe, neck, filament and base, no font/emoji dependency.
        paint.color = Color.rgb(65, 48, 12)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, radius * .13f)
        canvas.drawCircle(bulb.x, bulb.y - radius * .18f, radius * .37f, paint)
        canvas.drawLine(bulb.x - radius * .2f, bulb.y + radius * .14f, bulb.x - radius * .2f, bulb.y + radius * .43f, paint)
        canvas.drawLine(bulb.x + radius * .2f, bulb.y + radius * .14f, bulb.x + radius * .2f, bulb.y + radius * .43f, paint)
        canvas.drawLine(bulb.x - radius * .2f, bulb.y + radius * .43f, bulb.x + radius * .2f, bulb.y + radius * .43f, paint)
        canvas.drawLine(bulb.x - radius * .13f, bulb.y + radius * .6f, bulb.x + radius * .13f, bulb.y + radius * .6f, paint)
        paint.style = Paint.Style.FILL
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) { gesture.cancel(); return true }
        // Batched movement can accompany the final UP as well as MOVE.
        // Check every sample before completing a tap, including an out-and-back drag.
        if (event.actionMasked == MotionEvent.ACTION_MOVE || event.actionMasked == MotionEvent.ACTION_UP) {
            for (i in 0 until event.historySize) {
                gesture.move(event.getPointerId(0), event.getHistoricalX(0, i), event.getHistoricalY(0, i))
            }
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val tile = geometry().hitTest(game.tiles, event.x, event.y)
                gesture.down(event.getPointerId(0), tile?.id, event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                gesture.move(event.getPointerId(0), event.x, event.y)
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> gesture.cancel()
            MotionEvent.ACTION_UP -> {
                val tile = geometry().hitTest(game.tiles, event.x, event.y)
                val id = gesture.up(event.getPointerId(0), tile?.id, event.x, event.y)
                if (tile != null) { clearHint(); invalidate() }
                if (id != null) {
                    if (game.status == Status.PLAYING && game.isAvailable(id)) {
                        removeCallbacks(clearBlocked); blockedTileId = null
                        clearHint()
                        val before = game.tiles
                        game.select(id)
                        effects.record(before, game.tiles, SystemClock.uptimeMillis(), ValueAnimator.areAnimatorsEnabled())
                        changed()
                    } else {
                        // The touched upper tile still consumes the tap; never search below it.
                        removeCallbacks(clearBlocked); blockedTileId = id
                        postDelayed(clearBlocked, 160L)
                        invalidate()
                    }
                    performClick()
                }
            }
        }
        return true
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clearFeedback()
    }
    override fun onDetachedFromWindow() {
        clearFeedback()
        super.onDetachedFromWindow()
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}

internal object BotanicalSymbols {
    private val green = Color.rgb(43, 100, 69)
    private val leafPath = Path().apply {
        moveTo(-.7f, .85f)
        cubicTo(-1.2f, -.4f, -.2f, -1.1f, .8f, -.9f)
        cubicTo(1.1f, .2f, .3f, .9f, -.7f, .85f)
    }
    private val lotusAngles = floatArrayOf(-55f, -28f, 0f, 28f, 55f)
    private val berryCenters = listOf(-.43f to -.25f, .35f to -.1f, -.4f to .45f, .32f to .6f)
    fun label(context: Context, symbol: Symbol): String = context.getString(when (symbol) {
        Symbol.LEAF -> R.string.symbol_leaf
        Symbol.LOTUS -> R.string.symbol_lotus
        Symbol.FERN -> R.string.symbol_fern
        Symbol.FLOWER -> R.string.symbol_flower
        Symbol.BERRIES -> R.string.symbol_berries
        Symbol.SUNFLOWER -> R.string.symbol_sunflower
        Symbol.MUSHROOM -> R.string.symbol_mushroom
        Symbol.BUTTERFLY -> R.string.symbol_butterfly
        Symbol.BAMBOO -> R.string.symbol_bamboo
        Symbol.ACORN -> R.string.symbol_acorn
        Symbol.MAPLE_LEAF -> R.string.symbol_maple_leaf
        Symbol.PINECONE -> R.string.symbol_pinecone
        Symbol.IRIS -> R.string.symbol_iris
        Symbol.GINKGO_LEAF -> R.string.symbol_ginkgo_leaf
        Symbol.CHERRY_BLOSSOM -> R.string.symbol_cherry_blossom
        Symbol.DRAGONFLY -> R.string.symbol_dragonfly
        Symbol.CACTUS -> R.string.symbol_cactus
        Symbol.LAVENDER -> R.string.symbol_lavender
        Symbol.LADYBUG -> R.string.symbol_ladybug
        Symbol.SNAIL -> R.string.symbol_snail
    })
    fun draw(c: Canvas, p: Paint, symbol: Symbol) {
        p.color = green; p.style = Paint.Style.FILL
        when (symbol) {
            Symbol.LEAF -> {
                c.drawPath(leafPath, p)
                p.color = Color.rgb(210, 220, 166); p.strokeWidth = .07f
                c.drawLine(-.65f, .8f, .6f, -.65f, p)
            }
            Symbol.LOTUS -> {
                p.color = Color.rgb(161, 92, 119)
                for (angle in lotusAngles) {
                    c.save(); c.rotate(angle, 0f, .6f)
                    c.drawOval(-.27f, -.95f, .27f, .65f, p); c.restore()
                }
                p.color = green; c.drawOval(-.85f, .6f, .85f, .85f, p)
            }
            Symbol.FERN -> {
                p.strokeWidth = .09f; c.drawLine(0f, 1f, 0f, -1f, p)
                for (i in 0..4) {
                    val y = -.65f + i * .32f; val w = .25f + i * .12f
                    c.save(); c.rotate(-25f, 0f, y); c.drawOval(-w, y - .18f, 0f, y + .1f, p); c.restore()
                    c.save(); c.rotate(25f, 0f, y); c.drawOval(0f, y - .18f, w, y + .1f, p); c.restore()
                }
            }
            Symbol.FLOWER -> {
                p.color = Color.rgb(167, 101, 72)
                repeat(5) { c.save(); c.rotate(it * 72f); c.drawOval(-.33f, -.95f, .33f, .15f, p); c.restore() }
                p.color = Color.rgb(238, 203, 104); c.drawCircle(0f, 0f, .28f, p)
            }
            Symbol.BERRIES -> {
                p.strokeWidth = .09f; c.drawLine(0f, -.85f, 0f, .65f, p)
                c.drawOval(0f, -.95f, .75f, -.55f, p)
                p.color = Color.rgb(128, 71, 111)
                berryCenters.forEach { (x, y) -> c.drawCircle(x, y, .32f, p) }
            }
            Symbol.SUNFLOWER -> {
                p.color = Color.rgb(193, 147, 40)
                repeat(12) { c.save(); c.rotate(it * 30f); c.drawOval(-.15f, -1f, .15f, -.25f, p); c.restore() }
                p.color = Color.rgb(91, 76, 43); c.drawCircle(0f, 0f, .48f, p)
                p.color = Color.rgb(231, 204, 133)
                repeat(5) { val a = it * 2 * PI / 5; c.drawCircle((cos(a) * .23).toFloat(), (sin(a) * .23).toFloat(), .055f, p) }
            }
            Symbol.MUSHROOM -> {
                p.color = Color.rgb(156, 89, 65)
                c.drawRoundRect(-.22f, -.15f, .22f, .95f, .12f, .12f, p)
                c.drawArc(-.95f, -.9f, .95f, .65f, 180f, 180f, true, p)
                p.color = Color.rgb(238, 214, 169)
                c.drawCircle(-.4f, -.38f, .12f, p); c.drawCircle(.25f, -.55f, .14f, p)
            }
            Symbol.BUTTERFLY -> {
                p.color = Color.rgb(144, 112, 159)
                for (side in listOf(-1f, 1f)) {
                    c.save(); c.scale(side, 1f)
                    c.drawOval(.05f, -.9f, .95f, .15f, p)
                    c.drawOval(.08f, -.05f, .7f, .8f, p); c.restore()
                }
                p.color = green; p.strokeWidth = .12f; c.drawLine(0f, -.7f, 0f, .8f, p)
                p.strokeWidth = .05f; c.drawLine(0f, -.65f, -.22f, -1f, p); c.drawLine(0f, -.65f, .22f, -1f, p)
            }
            Symbol.BAMBOO -> {
                for (x in listOf(-.35f, .3f)) {
                    c.drawRoundRect(x, -.95f, x + .18f, .95f, .05f, .05f, p)
                    p.color = Color.rgb(175, 193, 126); p.strokeWidth = .07f
                    for (y in listOf(-.5f, 0f, .5f)) c.drawLine(x, y, x + .18f, y, p)
                    p.color = green
                }
                c.save(); c.rotate(-35f); c.drawOval(.05f, -.5f, .9f, -.22f, p); c.restore()
            }
            Symbol.ACORN -> {
                p.color = Color.rgb(163, 116, 63); c.drawOval(-.58f, -.35f, .58f, .92f, p)
                p.color = Color.rgb(98, 79, 49); c.drawOval(-.75f, -.65f, .75f, .02f, p)
                p.strokeWidth = .15f; c.drawLine(0f, -.5f, .18f, -.95f, p)
            }
            Symbol.MAPLE_LEAF -> {
                p.color = Color.rgb(167, 91, 57)
                val path = Path().apply {
                    moveTo(0f, -1f); lineTo(.25f, -.4f); lineTo(.6f, -.7f); lineTo(.55f, -.12f)
                    lineTo(1f, -.2f); lineTo(.72f, .3f); lineTo(.85f, .5f); lineTo(.1f, .65f)
                    lineTo(0f, 1f); lineTo(-.1f, .65f); lineTo(-.85f, .5f); lineTo(-.72f, .3f)
                    lineTo(-1f, -.2f); lineTo(-.55f, -.12f); lineTo(-.6f, -.7f); lineTo(-.25f, -.4f); close()
                }; c.drawPath(path, p)
            }
            Symbol.PINECONE -> {
                p.color = Color.rgb(120, 87, 57); c.drawOval(-.6f, -.95f, .6f, .95f, p)
                p.color = Color.rgb(190, 151, 98)
                for (row in 0..3) for (col in 0..2) {
                    val x = (col - 1) * .32f; val y = -.65f + row * .4f
                    c.drawArc(x - .2f, y - .15f, x + .2f, y + .2f, 0f, 180f, true, p)
                }
            }
            Symbol.IRIS -> {
                p.strokeWidth = .1f; c.drawLine(0f, -.1f, 0f, 1f, p)
                c.drawOval(.05f, .25f, .35f, 1f, p)
                p.color = Color.rgb(103, 101, 157)
                c.drawOval(-.24f, -1f, .24f, .2f, p)
                c.save(); c.rotate(-45f); c.drawOval(-.24f, -.9f, .24f, .3f, p); c.restore()
                c.save(); c.rotate(45f); c.drawOval(-.24f, -.9f, .24f, .3f, p); c.restore()
                p.color = Color.rgb(211, 179, 78); c.drawOval(-.1f, -.35f, .1f, .2f, p)
            }
            Symbol.GINKGO_LEAF -> {
                p.color = Color.rgb(153, 157, 65)
                val path = Path().apply { moveTo(0f, .65f); lineTo(-.95f, -.4f)
                    cubicTo(-.9f, -.95f, -.25f, -1f, 0f, -.65f)
                    cubicTo(.25f, -1f, .9f, -.95f, .95f, -.4f); close() }
                c.drawPath(path, p); p.strokeWidth = .08f; c.drawLine(0f, .5f, .15f, 1f, p)
                p.color = Color.rgb(216, 211, 135); p.strokeWidth = .04f
                for (x in listOf(-.65f, -.3f, .3f, .65f)) c.drawLine(0f, .55f, x, -.5f, p)
            }
            Symbol.CHERRY_BLOSSOM -> {
                p.color = Color.rgb(185, 114, 142)
                val petal = Path().apply { moveTo(0f, .05f); cubicTo(-.8f, -.35f, -.45f, -1.1f, 0f, -.75f)
                    cubicTo(.45f, -1.1f, .8f, -.35f, 0f, .05f); close() }
                repeat(5) { c.save(); c.rotate(it * 72f); c.drawPath(petal, p); c.restore() }
                p.color = Color.rgb(246, 213, 151); c.drawCircle(0f, 0f, .15f, p)
            }
            Symbol.CACTUS -> {
                c.drawRoundRect(-.25f, -.95f, .25f, .9f, .24f, .24f, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = .29f; p.strokeCap = Paint.Cap.ROUND
                val arms = Path().apply { moveTo(-.15f, .2f); lineTo(-.66f, .2f); lineTo(-.66f, -.5f)
                    moveTo(.15f, -.1f); lineTo(.65f, -.1f); lineTo(.65f, -.7f) }
                c.drawPath(arms, p)
                p.strokeCap = Paint.Cap.BUTT; p.strokeWidth = .06f; p.color = Color.rgb(179, 195, 128)
                c.drawLine(0f, -.65f, 0f, .65f, p)
                p.style = Paint.Style.FILL; p.color = Color.rgb(160, 110, 74)
                c.drawRoundRect(-.45f, .65f, .45f, 1f, .08f, .08f, p)
            }
            Symbol.LAVENDER -> {
                p.strokeWidth = .1f; c.drawLine(0f, -.9f, 0f, 1f, p)
                c.drawOval(-.55f, .45f, 0f, .7f, p); c.drawOval(0f, .2f, .5f, .45f, p)
                p.color = Color.rgb(119, 89, 150)
                for (i in 0..3) {
                    val y = -.75f + i * .3f
                    c.drawOval(-.4f, y - .19f, -.02f, y + .12f, p)
                    c.drawOval(.02f, y - .19f, .4f, y + .12f, p)
                }
                c.drawOval(-.16f, -1.05f, .16f, -.65f, p)
            }
            Symbol.LADYBUG -> {
                p.color = Color.rgb(49, 58, 43); p.strokeWidth = .1f
                for (y in listOf(-.3f, .15f, .55f)) c.drawLine(-.9f, y, .9f, y + .12f, p)
                c.drawCircle(0f, -.7f, .32f, p)
                c.drawLine(-.13f, -.8f, -.35f, -1f, p); c.drawLine(.13f, -.8f, .35f, -1f, p)
                p.color = Color.rgb(174, 65, 53); c.drawOval(-.7f, -.65f, .7f, .95f, p)
                p.color = Color.rgb(49, 58, 43); p.strokeWidth = .09f; c.drawLine(0f, -.62f, 0f, .9f, p)
                for (x in listOf(-.33f, .33f)) for (y in listOf(-.2f, .4f)) c.drawCircle(x, y, .15f, p)
            }
            Symbol.SNAIL -> {
                p.color = Color.rgb(95, 123, 68); c.drawRoundRect(-.95f, .45f, .95f, .85f, .2f, .2f, p)
                c.drawOval(.5f, -.05f, .88f, .75f, p); p.strokeWidth = .08f
                c.drawLine(.63f, .12f, .49f, -.3f, p); c.drawLine(.8f, .1f, .95f, -.3f, p)
                c.drawCircle(.49f, -.3f, .1f, p); c.drawCircle(.95f, -.3f, .1f, p)
                p.color = Color.rgb(181, 133, 77); c.drawCircle(-.22f, -.08f, .68f, p)
                p.color = Color.rgb(102, 73, 47); p.style = Paint.Style.STROKE; p.strokeWidth = .11f
                val spiral = Path()
                for (i in 0..48) {
                    val angle = i * .22; val r = .55 * (1 - i / 56.0)
                    val x = (-.22 + cos(angle) * r).toFloat(); val y = (-.08 + sin(angle) * r).toFloat()
                    if (i == 0) spiral.moveTo(x, y) else spiral.lineTo(x, y)
                }
                c.drawPath(spiral, p); p.style = Paint.Style.FILL
            }
            Symbol.DRAGONFLY -> {
                p.color = Color.rgb(111, 154, 154)
                for (angle in listOf(-65f, 65f, -110f, 110f)) {
                    c.save(); c.rotate(angle, 0f, -.25f); c.drawOval(-.18f, -1.05f, .18f, -.2f, p); c.restore()
                }
                p.color = green; p.strokeWidth = .13f; c.drawLine(0f, -.55f, 0f, 1f, p)
                c.drawCircle(0f, -.65f, .2f, p)
            }

        }
    }
}
