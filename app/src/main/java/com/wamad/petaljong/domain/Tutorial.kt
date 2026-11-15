package com.wamad.petaljong.domain

/** Names are durable IDs. Exemptions are authorized only by the matching live step and board. */
enum class TutorialStep {
    NOT_STARTED, INTRODUCTION, SELECT_TILE, EXPLAIN_TRAY, DEMONSTRATE_UNDO,
    DEMONSTRATE_HINT, COMPLETE_PAIR, FINISHED, SKIPPED;
    val active get() = this !in listOf(NOT_STARTED, FINISHED, SKIPPED)
}

class TutorialGuide(private val session: GardenSession, initial: TutorialStep) {
    var step = initial; private set
    // Match the real Hint policy so its animated tile is also the next pair target.
    // Move that pair to the front only if replaying the remaining verified witness still wins.
    private val verifiedOpening: List<Int>? by lazy {
        runCatching {
            require(session.level.id == 1L)
            val initial = Game(session.level.tiles)
            val first = requireNotNull(HintPolicy.suggest(session.level.tiles, initial.snapshot()).id)
            require(!initial.tiles.first { it.id == first }.mystery && initial.select(first))
            val symbol = initial.tray.single().symbol
            session.level.winningSelections.firstNotNullOfOrNull { second ->
                val tile = initial.remaining.firstOrNull { it.id == second && it.symbol == symbol && !it.mystery }
                if (tile == null || !initial.isAvailable(second)) null else {
                    val proof = Game(session.level.tiles)
                    val pair = listOf(first, second)
                    val solution = pair + session.level.winningSelections.filterNot { it in pair }
                    if (solution.all { proof.select(it) } && proof.status == Status.WON) pair else null
                }
            }
        }.getOrNull()
    }
    private val opening get() = verifiedOpening.orEmpty()
    private fun expected(count: Int): GameSnapshot = Game(session.level.tiles).also { g ->
        opening.take(count).forEach { check(g.select(it)) }
    }.snapshot()
    private fun matches(count: Int) = session.game.snapshot().let {
        it.copy(selectedId = null) == expected(count).copy(selectedId = null)
    }
    /** No puzzle mutation: the complete reordered production witness must be verified first. */
    fun compatible(): Boolean = verifiedOpening != null
    fun begin(frontier: Long): Boolean {
        if (step != TutorialStep.NOT_STARTED) return false
        step = if (frontier == 0L && compatible() && matches(0)) TutorialStep.INTRODUCTION else TutorialStep.SKIPPED
        return step.active
    }
    fun restoreSafely() {
        if (!step.active) return
        val valid = compatible() && when (step) {
            TutorialStep.EXPLAIN_TRAY, TutorialStep.DEMONSTRATE_UNDO -> matches(1)
            TutorialStep.COMPLETE_PAIR -> matches(0) || matches(1)
            else -> matches(0)
        }
        if (!valid) step = TutorialStep.SKIPPED // Never regenerate an interrupted player's puzzle.
    }
    fun continueGuide(): Boolean {
        step = when (step) {
            TutorialStep.INTRODUCTION -> TutorialStep.SELECT_TILE
            TutorialStep.EXPLAIN_TRAY -> TutorialStep.DEMONSTRATE_UNDO
            else -> return false
        }
        return true
    }
    val target: Int? get() = when (step) {
        TutorialStep.SELECT_TILE -> opening.firstOrNull()
        TutorialStep.COMPLETE_PAIR -> opening.getOrNull(if (matches(1)) 1 else 0)
        else -> null
    }?.takeIf { session.game.isAvailable(it) }
    fun allowsTile(id: Int) = !step.active || id == target
    fun collected() {
        if (step == TutorialStep.SELECT_TILE && matches(1)) step = TutorialStep.EXPLAIN_TRAY
        else if (step == TutorialStep.COMPLETE_PAIR && matches(2)) step = TutorialStep.FINISHED
    }
    fun undo(): Boolean {
        if (step != TutorialStep.DEMONSTRATE_UNDO || !matches(1) || !session.game.undo()) return false
        step = TutorialStep.DEMONSTRATE_HINT
        return true
    }
    fun hint(id: Int, snapshot: GameSnapshot): Boolean {
        if (step != TutorialStep.DEMONSTRATE_HINT || !matches(0) || session.game.snapshot() != snapshot) return false
        val suggestion = HintPolicy.suggest(session.level.tiles, snapshot)
        if (suggestion.id != id || !session.game.isAvailable(id)) return false
        step = TutorialStep.COMPLETE_PAIR
        return true
    }
    fun skip() { if (step.active || step == TutorialStep.NOT_STARTED) step = TutorialStep.SKIPPED }
}
