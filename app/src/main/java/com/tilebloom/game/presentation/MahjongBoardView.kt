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
    init { layoutDirection = View.LAYOUT_DIRECTION_LTR; BotanicalSymbols.prepare(context) }
    var theme = GardenTheme.forLevel(1)
        set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Unit-space shaders are shared across every tile; only their local matrix changes.
    private val ceramicLight = LinearGradient(0f, 0f, 0f, 1f,
        intArrayOf(0xfffffef9.toInt(), 0xfffaf8f0.toInt(), 0xfff2ede2.toInt(), 0xffe9dfcc.toInt()),
        floatArrayOf(0f, .24f, .84f, 1f), Shader.TileMode.CLAMP)
    private val ceramicMuted = LinearGradient(0f, 0f, 0f, 1f,
        0xffe8e2d4.toInt(), 0xffd6cbb7.toInt(), Shader.TileMode.CLAMP)
    private val gardenGreen = LinearGradient(0f, 0f, 1f, 1f,
        intArrayOf(0xff409a65.toInt(), 0xff2f8b57.toInt(), 0xff2f8b57.toInt(), 0xff267347.toInt()),
        floatArrayOf(0f, .18f, .78f, 1f), Shader.TileMode.CLAMP)
    private val gardenBlocked = LinearGradient(0f, 0f, 1f, 1f,
        intArrayOf(0xff378456.toInt(), 0xff2b7d4f.toInt(), 0xff2b7d4f.toInt(), 0xff266e45.toInt()),
        floatArrayOf(0f, .18f, .78f, 1f), Shader.TileMode.CLAMP)
    private val flowerGold = LinearGradient(0f, -1f, .4f, .2f,
        0xfffff3d5.toInt(), 0xffe6c875.toInt(), Shader.TileMode.CLAMP)
    private val leafIvory = LinearGradient(0f, -.2f, 0f, .8f,
        0xfffff3d5.toInt(), 0xffa8e6ad.toInt(), Shader.TileMode.CLAMP)
    private val glaze = RadialGradient(.2f, .08f, .85f,
        0x66ffffff, 0x00ffffff, Shader.TileMode.CLAMP)
    // A restrained green glaze preserves the medium-green face instead of whitening it.
    private val mysteryGlaze = RadialGradient(.2f, .08f, .65f,
        0x10409a65, 0x00409a65, Shader.TileMode.CLAMP)
    private val goldTrim = LinearGradient(0f, 0f, 1f, 1f,
        intArrayOf(0xffd8c596.toInt(), 0xffb79b66.toInt(), 0xffdecca0.toInt(), 0xffa18a5e.toInt()),
        floatArrayOf(0f, .4f, .65f, 1f), Shader.TileMode.CLAMP)
    private val mysteryGoldTrim = LinearGradient(0f, 0f, 1f, 1f,
        intArrayOf(0xfffff3d5.toInt(), 0xffe6c875.toInt(), 0xfff5dfa2.toInt(), 0xffc6a252.toInt()),
        floatArrayOf(0f, .4f, .65f, 1f), Shader.TileMode.CLAMP)
    private val wall = LinearGradient(0f, 0f, 1f, 1f,
        intArrayOf(0xffeee6d7.toInt(), 0xffc8baa0.toInt(), 0xffac9979.toInt()),
        floatArrayOf(0f, .65f, 1f), Shader.TileMode.CLAMP)
    private val surfaceMatrix = Matrix()
    // Cached unit-space artwork is independent of the concealed symbol.
    private val gardenLeaf = Path().apply {
        moveTo(.04f, .65f)
        cubicTo(.12f, .12f, .64f, .08f, .88f, .16f)
        cubicTo(.86f, .62f, .48f, .9f, .04f, .65f)
        close()
    }
    private val emblemSides = floatArrayOf(-1f, 1f)
    private fun surface(shader: Shader, face: BoardRect) {
        surfaceMatrix.setScale(face.right - face.left, face.bottom - face.top)
        surfaceMatrix.postTranslate(face.left, face.top)
        shader.setLocalMatrix(surfaceMatrix)
        paint.shader = shader
    }
    private val effects = MatchEffects()
    private val reveals = MysteryRevealFeedback()
    private var tapStarted = 0L
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
    private var cachedGeometry: BoardGeometry? = null
    private fun geometry() = cachedGeometry ?: BoardGeometry(width.toFloat(), height.toFloat(),
        BoardPadding(paddingLeft.toFloat(), paddingTop.toFloat(), paddingRight.toFloat(), paddingBottom.toFloat()), game.tiles).also { cachedGeometry = it }
    // Read-only presentation queries also allow instrumentation to assert real touch results.
    var allowTile: (Int) -> Boolean = { true }
    fun tutorialBounds(id: Int): RectF = game.remaining.firstOrNull { it.id == id }?.let { geometry().face(it).android() } ?: RectF()
    var audioCue: (com.tilebloom.game.domain.AudioCue) -> Unit = {}
    val selectedTileId get() = game.selected
    val remainingTileCount get() = game.remaining.size
    fun tileCenter(id: Int): BoardPoint? = game.remaining.find { it.id == id }?.let { geometry().face(it).center }
    init {
        isSoundEffectsEnabled = false
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        refresh()
    }
    fun load(next: Game) { clearFeedback(); game = next; cachedGeometry = null; refresh() }
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
        reveals.clear()
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
            val flip = reveals.progress(tile.id, now, ValueAnimator.areAnimatorsEnabled())
            val back = tile.concealed || (flip != null && flip < .5f)
            if (flip != null) canvas.scale(maxOf(.06f, abs(cos(flip * PI).toFloat())), 1f, r.centerX(), r.centerY())
            val selected = available && game.selected == tile.id
            paint.style = Paint.Style.FILL
            // Two offset shadow bands: soft penumbra and a stronger contact shadow.
            paint.color = (theme.palette.tileShadow and 0x00ffffff) or ((if (tile.layer > 0) 34 else 24) shl 24)
            canvas.drawRoundRect(layout.shadow(tile).offset(scale * .045f).android().apply { offset(scale * .035f, 0f) }, layout.radius * 1.2f, layout.radius * 1.2f, paint)
            paint.color = (theme.palette.tileShadow and 0x00ffffff) or ((if (tile.layer > 0) 80 else 60) shl 24)
            canvas.drawRoundRect(layout.shadow(tile).android().apply { offset(scale * .015f, 0f) }, layout.radius, layout.radius, paint)
            paint.color = if (back) 0xff267347.toInt() else 0xffa39478.toInt()
            if (!back) surface(wall, face)
            canvas.drawRoundRect(layout.body(tile).android(), layout.radius, layout.radius, paint)
            paint.shader = null
            paint.color = if (back) 0xff409a65.toInt() else 0xffe2d7c2.toInt()
            canvas.drawRoundRect(layout.edge(tile).android(), layout.radius, layout.radius, paint)
            surface(if (available) ceramicLight else ceramicMuted, face)
            canvas.drawRoundRect(face.android(), layout.radius, layout.radius, paint)
            if (available) {
                surface(glaze, face)
                canvas.drawRoundRect(face.android(), layout.radius, layout.radius, paint)
            }
            paint.shader = null
            // One fine warm rim; the highlight follows only the upper edge.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = scale * .011f
            paint.color = if (available) 0xffbda373.toInt() else 0xffb6a78b.toInt()
            if (available) surface(goldTrim, face)
            canvas.drawRoundRect(face.inset(paint.strokeWidth / 2).android(), layout.radius, layout.radius, paint)
            paint.shader = null
            // A single upper-edge glint suggests a raised face without concentric outlines.
            paint.strokeWidth = scale * .014f
            paint.color = if (available) 0xcffffef7.toInt() else 0x55fffef7
            canvas.drawLine(face.left + layout.radius, face.top + scale * .022f,
                face.right - layout.radius, face.top + scale * .022f, paint)
            if (selected || blockedTileId == tile.id) {
                paint.strokeWidth = scale * .035f
                paint.color = if (selected) theme.palette.primaryAccent else theme.palette.secondaryAccent
                canvas.drawRoundRect(face.inset(paint.strokeWidth / 2).android(), layout.radius, layout.radius, paint)
            }
            paint.style = Paint.Style.FILL
            // Draw the whole lower face first. Opaque upper bodies/faces in the same
            // depth order physically occlude it, including partially covered artwork.
            if (back) drawMystery(canvas, face.android(), layout.radius, scale, available)
            if (!tile.removed) {
                canvas.save()
                canvas.translate(r.centerX(), r.centerY())
                canvas.scale(scale * .34f, scale * .34f)
                if (!back) BotanicalSymbols.drawTile(canvas, paint, tile.symbol, context)
                canvas.restore()
                if (!available && !back) {
                    paint.color = (theme.palette.tileShadow and 0x00ffffff) or (12 shl 24)
                    canvas.drawRoundRect(face.android(), layout.radius, layout.radius, paint)
                }
            }
            canvas.restoreToCount(saved)
        }
        hintEffect.validate(game, now)
        // Foreground decoration shares the authoritative face transform, never hit geometry.
        hintEffect.face(game, layout)?.let { face -> drawHint(canvas, layout, face, now) }
        if (reveals.active(now, ValueAnimator.areAnimatorsEnabled()) || effects.active.isNotEmpty() || (hintedTileId != null && ValueAnimator.areAnimatorsEnabled())) postInvalidateOnAnimation()
    }
    private fun drawMystery(c: Canvas, face: RectF, radius: Float, scale: Float, available: Boolean) {
        val bounds = BoardRect(face.left, face.top, face.right, face.bottom)
        paint.style = Paint.Style.FILL
        surface(if (available) gardenGreen else gardenBlocked, bounds)
        c.drawRoundRect(face, radius, radius, paint)
        if (available) {
            surface(mysteryGlaze, bounds)
            c.drawRoundRect(face, radius, radius, paint)
        }
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scale * .014f
        surface(mysteryGoldTrim, bounds)
        c.drawRoundRect(face.left + scale * .012f, face.top + scale * .012f,
            face.right - scale * .012f, face.bottom - scale * .012f, radius, radius, paint)
        paint.shader = null
        paint.strokeWidth = scale * .008f
        paint.color = if (available) 0x99fff3d5.toInt() else 0x55fff3d5
        c.drawRoundRect(face.left + scale * .043f, face.top + scale * .043f,
            face.right - scale * .043f, face.bottom - scale * .043f, radius * .72f, radius * .72f, paint)
        // Fine warm glint preserves ceramic depth without whitening the green face.
        paint.strokeWidth = scale * .018f
        paint.color = if (available) 0xcffff3d5.toInt() else 0x66fff3d5
        c.drawLine(face.left + radius, face.top + scale * .025f,
            face.right - radius, face.top + scale * .025f, paint)
        c.save()
        c.translate(face.centerX(), face.centerY() + scale * .025f)
        c.scale(scale * .31f, scale * .31f)
        paint.style = Paint.Style.FILL
        for (side in emblemSides) {
            c.save(); c.scale(side, 1f)
            paint.color = 0x33105c3a
            c.translate(0f, .045f); c.drawPath(gardenLeaf, paint); c.translate(0f, -.045f)
            paint.shader = leafIvory; c.drawPath(gardenLeaf, paint); paint.shader = null
            paint.style = Paint.Style.STROKE; paint.strokeWidth = .026f
            paint.color = 0xff539e6b.toInt()
            c.drawLine(.13f, .64f, .69f, .28f, paint)
            paint.style = Paint.Style.FILL
            c.restore()
        }
        // Six broad shaded petals read as a flower even at normal board size.
        c.save(); c.translate(0f, -.3f)
        for (i in 0 until 6) {
            c.save(); c.rotate(i * 60f)
            paint.color = 0x44156c48
            c.drawOval(-.205f, -.65f, .245f, -.03f, paint)
            paint.shader = flowerGold
            c.drawOval(-.225f, -.69f, .225f, -.07f, paint)
            paint.shader = null
            c.restore()
        }
        paint.color = 0xffb88a36.toInt(); c.drawCircle(0f, 0f, .235f, paint)
        paint.color = 0xffffe6a0.toInt(); c.drawCircle(0f, -.025f, .185f, paint)
        paint.color = 0xfffff3d5.toInt(); c.drawCircle(-.045f, -.08f, .065f, paint)
        c.restore(); c.restore()
        paint.shader = null; paint.style = Paint.Style.FILL
    }
    private fun drawHint(canvas: Canvas, layout: BoardGeometry, face: BoardRect, now: Long) {
        val density = resources.displayMetrics.density
        val pulse = hintEffect.brightness(now, ValueAnimator.areAnimatorsEnabled())
        val stroke = 3.5f * density
        hintBounds.set(face.left, face.top, face.right, face.bottom)
        // Stroke outside the ceramic rim keeps botanical artwork untouched.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke + 2.5f * density
        paint.color = theme.palette.backgroundTop
        canvas.drawRoundRect(hintBounds, layout.radius, layout.radius, paint)
        paint.strokeWidth = stroke
        paint.color = theme.palette.primaryAccent
        paint.alpha = (215 + 40 * pulse).toInt()
        canvas.drawRoundRect(hintBounds, layout.radius, layout.radius, paint)
        paint.alpha = 255
        val radius = min(11f * density, layout.scale * .22f)
        val bulb = hintIndicator ?: hintEffect.indicator(face, width.toFloat(), height.toFloat(), radius, game, layout)
            .also { hintIndicator = it }
        paint.strokeWidth = 2f * density
        paint.color = theme.palette.backgroundTop
        val anchorX = if (bulb.x > face.center.x) face.right - layout.radius else face.left + layout.radius
        val anchorY = if (bulb.y > face.center.y) face.bottom else face.top
        canvas.drawLine(bulb.x, bulb.y, anchorX, anchorY, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(bulb.x, bulb.y, radius, paint)
        paint.color = theme.palette.primaryAccent
        canvas.drawCircle(bulb.x, bulb.y, radius * .82f, paint)
        // Original vector bulb: globe, neck, filament and base, no font/emoji dependency.
        paint.color = theme.palette.backgroundTop
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
                tapStarted = event.eventTime
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
                if (id != null && allowTile(id) && reveals.accepts(id, tapStarted)) {
                    if (game.status == Status.PLAYING && game.isAvailable(id)) {
                        removeCallbacks(clearBlocked); blockedTileId = null
                        clearHint()
                        val before = game.tiles
                        val concealed = before.first { it.id == id }.concealed
                        val cues = com.tilebloom.game.domain.GameplayAudio.events(game) { game.interact(id) }
                        cues.forEach(audioCue)
                        if (concealed) {
                            reveals.revealed(id, SystemClock.uptimeMillis())
                            announceForAccessibility(context.getString(R.string.mystery_revealed, BotanicalSymbols.label(context, before.first { it.id == id }.symbol)))
                        }
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
        cachedGeometry = null
        clearFeedback()
    }
    override fun onDetachedFromWindow() {
        clearFeedback()
        super.onDetachedFromWindow()
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}

internal object BotanicalSymbols {
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
        Symbol.VEINED_LEAF -> R.string.symbol_veined_leaf
        Symbol.DOUBLE_LEAF -> R.string.symbol_double_leaf
        Symbol.YELLOW_DAISY -> R.string.symbol_yellow_daisy
        Symbol.PINK_PEONY -> R.string.symbol_pink_peony
        Symbol.PINK_CAMELLIA -> R.string.symbol_pink_camellia
        Symbol.PURPLE_CROCUS -> R.string.symbol_purple_crocus
        Symbol.PURPLE_BELLFLOWER -> R.string.symbol_purple_bellflower
        Symbol.YELLOW_MARIGOLD -> R.string.symbol_yellow_marigold
        Symbol.GOLDEN_CHRYSANTHEMUM -> R.string.symbol_golden_chrysanthemum
        Symbol.MONSTERA_LEAF -> R.string.symbol_monstera_leaf
        Symbol.EUCALYPTUS_LEAF -> R.string.symbol_eucalyptus_leaf
        Symbol.RED_JAPANESE_MAPLE -> R.string.symbol_red_japanese_maple
        Symbol.ORANGE_SUGAR_MAPLE -> R.string.symbol_orange_sugar_maple
        Symbol.OLIVE_BRANCH -> R.string.symbol_olive_branch
        Symbol.ROSEMARY_BRANCH -> R.string.symbol_rosemary_branch
    })
    // Bounded process cache: 35 x 256 x 256 ARGB pixels = at most 8.75 MiB.
    // Android resources are local; no Context or Activity is retained.
    private val artwork = arrayOfNulls<Bitmap>(Symbol.entries.size)
    private val sources = arrayOfNulls<Rect>(Symbol.entries.size)
    private val destinations = arrayOfNulls<RectF>(Symbol.entries.size)
    // Maximum ink extents in shared symbol coordinates: board uses .34 × tile width.
    // Tall sprigs get portrait space; round flowers stay balanced; wings favor width.
    private fun opticalSize(symbol: Symbol): Pair<Float, Float> = when (symbol) {
        Symbol.LAVENDER, Symbol.FERN, Symbol.BAMBOO, Symbol.OLIVE_BRANCH, Symbol.ROSEMARY_BRANCH, Symbol.PURPLE_BELLFLOWER -> 2.30f to 2.94f
        Symbol.IRIS, Symbol.CACTUS, Symbol.PINECONE -> 2.28f to 2.78f
        Symbol.LEAF, Symbol.VEINED_LEAF, Symbol.DOUBLE_LEAF -> 2.30f to 2.70f
        Symbol.BUTTERFLY, Symbol.DRAGONFLY -> 2.38f to 2.58f
        Symbol.SUNFLOWER -> 2.22f to 2.50f
        Symbol.FLOWER, Symbol.CHERRY_BLOSSOM, Symbol.YELLOW_DAISY -> 2.34f to 2.60f
        else -> 2.30f to 2.64f
    }
    private val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val resourceIds = intArrayOf(
        R.drawable.botanical_leaf, R.drawable.botanical_lotus, R.drawable.botanical_fern,
        R.drawable.botanical_flower, R.drawable.botanical_berries, R.drawable.botanical_sunflower,
        R.drawable.botanical_mushroom, R.drawable.botanical_butterfly, R.drawable.botanical_bamboo,
        R.drawable.botanical_acorn, R.drawable.botanical_maple_leaf, R.drawable.botanical_pinecone,
        R.drawable.botanical_iris, R.drawable.botanical_ginkgo_leaf, R.drawable.botanical_cherry_blossom,
        R.drawable.botanical_dragonfly, R.drawable.botanical_cactus, R.drawable.botanical_lavender,
        R.drawable.botanical_ladybug, R.drawable.botanical_snail, R.drawable.botanical_veined_leaf,
        R.drawable.botanical_double_leaf, R.drawable.botanical_yellow_daisy,
        R.drawable.botanical_pink_peony,
        R.drawable.botanical_pink_camellia,
        R.drawable.botanical_purple_crocus,
        R.drawable.botanical_purple_bellflower,
        R.drawable.botanical_yellow_marigold,
        R.drawable.botanical_golden_chrysanthemum,
        R.drawable.botanical_monstera_leaf,
        R.drawable.botanical_eucalyptus_leaf,
        R.drawable.botanical_red_japanese_maple,
        R.drawable.botanical_orange_sugar_maple,
        R.drawable.botanical_olive_branch,
        R.drawable.botanical_rosemary_branch
    )
    // Warm once at view creation, before layout/drawing and animated interactions.
    fun prepare(context: Context) {
        for (id in resourceIds.indices) load(context, id)
    }
    private fun load(context: Context, id: Int): Bitmap = artwork[id] ?: requireNotNull(
        BitmapFactory.decodeResource(context.resources, resourceIds[id], BitmapFactory.Options().apply {
            inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    ).also { bitmap ->
        // Scan once on decode, preserving every nontransparent pixel and aspect ratio.
        val row = IntArray(bitmap.width)
        var left = bitmap.width; var top = bitmap.height; var right = -1; var bottom = -1
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            for (x in row.indices) if (Color.alpha(row[x]) > 0) {
                left = minOf(left, x); right = maxOf(right, x)
                top = minOf(top, y); bottom = maxOf(bottom, y)
            }
        }
        require(right >= left && bottom >= top) { "Empty botanical asset $id" }
        val source = Rect(left, top, right + 1, bottom + 1)
        val symbol = Symbol.entries.single { it.stableId == id }
        val (maxWidth, maxHeight) = opticalSize(symbol)
        val factor = minOf(maxWidth / source.width(), maxHeight / source.height())
        val halfWidth = source.width() * factor / 2
        val halfHeight = source.height() * factor / 2
        sources[id] = source
        destinations[id] = RectF(-halfWidth, -halfHeight, halfWidth, halfHeight)
        artwork[id] = bitmap
    }
    private val welcomeDestination = RectF(-1.12f, -1.12f, 1.12f, 1.12f)
    // Home artwork retains its original composition; only gameplay uses optical fitting.
    fun draw(c: Canvas, p: Paint, symbol: Symbol, context: Context) {
        c.drawBitmap(load(context, symbol.stableId), null, welcomeDestination, artworkPaint)
        p.style = Paint.Style.FILL
    }
    fun drawTile(c: Canvas, p: Paint, symbol: Symbol, context: Context) {
        val bitmap = load(context, symbol.stableId)
        // Dedicated paint prevents caller stroke style, alpha, or tint leaking into artwork.
        c.drawBitmap(bitmap, sources[symbol.stableId], requireNotNull(destinations[symbol.stableId]), artworkPaint)
        p.style = Paint.Style.FILL
    }
}
