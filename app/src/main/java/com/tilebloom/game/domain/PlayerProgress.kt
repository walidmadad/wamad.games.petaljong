package com.tilebloom.game.domain

/** completed is retained only as a five-level legacy adapter. The durable record is O(1). */
data class PlayerProgress(val completed: Set<Int> = emptySet(), val active: SessionSnapshot? = null,
    val undoCredits: Int = 3, val highestCompleted: Long = 0, val hintCredits: Int = 3) {
    val frontier: Long get() = maxOf(highestCompleted, (1..5).takeWhile { it in completed }.size.toLong())
    val highestUnlocked: Long get() = if (frontier == Long.MAX_VALUE) frontier else frontier + 1
}
object ProgressCodec {
    const val SCHEMA = 6
    fun encode(p: PlayerProgress): String {
        val s = p.active
        return listOf("$SCHEMA", p.frontier.toString(), s?.let { "${it.id},${it.seed},${it.version},${it.game.rulesVersion}" } ?: "",
            s?.game?.let { g -> (g.selections ?: g.moves.flatMap { listOf(it.first, it.second) }).joinToString(";") } ?: "", p.undoCredits.toString(), p.hintCredits.toString(), s?.game?.actionLog?.joinToString(";") { "${if (it.kind == ActionKind.REVEAL) "R" else "C"}:${it.tileId}" } ?: "").joinToString("\n")
    }
    fun decode(raw: String): PlayerProgress {
        require(raw.length <= 8192)
        val lines = raw.split('\n'); val schema = lines[0].toInt(); require(schema in 1..SCHEMA)
        val credits = if (schema == 1) 3 else lines.getOrNull(4)?.toIntOrNull() ?: error("Missing credits")
        require(credits >= 0)
        val frontier = if (schema >= 3) lines[1].toLong().also { require(it >= 0) } else {
            val recorded = lines.getOrNull(1).orEmpty().split(',').mapNotNull { it.toIntOrNull() }.toSet()
            (1..5).takeWhile { it in recorded }.size.toLong()
        }
        val active = runCatching {
            val identity = lines[2].split(',')
            require(identity.size == if (schema >= 4) 4 else 3)
            val actions = lines.getOrNull(3).orEmpty().takeIf { it.isNotEmpty() }?.split(';').orEmpty()
            val snapshot = if (schema >= 4) {
                GameSnapshot(emptyList(), null, actions.map { it.toInt() }, identity[3].toInt(), if (schema >= 6) lines.getOrNull(6).orEmpty().takeIf { it.isNotEmpty() }?.split(';')?.map {
                    val parts = it.split(':'); require(parts.size == 2 && parts[0] in listOf("R", "C"))
                    GameAction(if (parts[0] == "R") ActionKind.REVEAL else ActionKind.COLLECT, parts[1].toInt())
                } else null)
            } else {
                val pairs = actions.map {
                    val pair = it.split(','); require(pair.size == 2)
                    PairMove(pair[0].toInt(), pair[1].toInt())
                }
                // Legacy schema actions are pairs, explicitly expanded during validated restoration.
                GameSnapshot(pairs, null)
            }
            SessionSnapshot(identity[0].toLong(), identity[1].toLong(), identity[2].toInt(), snapshot)
        }.getOrNull()
        val hints = if (schema < 5) 3 else lines.getOrNull(5)?.toIntOrNull() ?: error("Missing hint credits")
        require(hints in 0..3)
        return PlayerProgress((1..minOf(5L, frontier).toInt()).toSet(), active, credits, frontier, hints)
    }
}
class ProgressController(saved: PlayerProgress = PlayerProgress()) {
    val session = GardenSession()
    val wallet = UndoCreditWallet(saved.undoCredits)
    var hintCredits = saved.hintCredits; private set
    /** Candidate is persisted before either live wallet is changed. Main-thread transaction. */
    fun grantReward(purpose: RewardPurpose, save: (PlayerProgress) -> Boolean): Boolean {
        val current = capture()
        val candidate = when (purpose) {
            RewardPurpose.UNDO -> if (wallet.balance == 0) current.copy(undoCredits = 1) else return false
            RewardPurpose.HINT -> if (hintCredits == 0) current.copy(hintCredits = 1) else return false
        }
        if (!save(candidate)) return false
        when (purpose) {
            RewardPurpose.UNDO -> wallet.rewardTicket().earned()
            RewardPurpose.HINT -> hintCredits++
        }
        return true
    }
    fun acceptHint(id: Int, expected: GameSnapshot): Boolean {
        if (hintCredits <= 0 || session.game.snapshot() != expected || session.game.status != Status.PLAYING || !session.game.isAvailable(id)) return false
        val candidate = Game(session.level.tiles)
        if (!candidate.restore(expected) || !candidate.interact(id) || candidate.status == Status.LOST) return false
        hintCredits--
        return true
    }
    private var completed = saved.frontier
    var recovered = false; private set
    val highestUnlocked get() = if (completed == Long.MAX_VALUE) completed else completed + 1
    init {
        val active = saved.active
        val pending = active != null && active.id == completed && completed > 0 &&
            session.restore(active) && session.game.status == Status.WON
        if (!pending) {
            // A completed snapshot with a corrupt/missing history cannot become playable.
            val id = highestUnlocked
            val level = Levels.get(id)
            val compatible = active?.takeIf { it.id == id }
            if (compatible == null || !session.restore(compatible)) {
                recovered = active != null
                val replacement = Game(level.tiles)
                if (completed == Long.MAX_VALUE) level.winningActions.forEach {
                    check(if (it.kind == ActionKind.REVEAL) replacement.reveal(it.tileId) else replacement.select(it.tileId))
                }
                check(session.restore(SessionSnapshot(id, level.seed, level.version, replacement.snapshot())))
            }
        }
        capture()
    }
    /** Publish the next board only after the entire candidate save is durable.
     * Retrying after a crash sees either the pending victory or the committed next board.
     */
    fun finalizeCompletedEntry(save: (PlayerProgress) -> Boolean): Boolean {
        val next = prepareNext() ?: return true
        val candidate = nextProgress(next) ?: return false
        if (!save(candidate)) return false
        return commitNext(next)
    }
    fun undo(): Boolean = wallet.undo { session.game.undo() }
    fun capture(): PlayerProgress {
        if (session.game.status == Status.WON) completed = maxOf(completed, session.level.id)
        return PlayerProgress((1..minOf(5L, completed).toInt()).toSet(),
            session.snapshot().let { it.copy(game = it.game.copy(selectedId = null)) }, wallet.balance, completed, hintCredits)
    }
    /** No selection/navigation API can load any board, including an unlocked one. */
    fun select(id: Int): Boolean = id.toLong() == session.level.id && session.game.status != Status.WON
    fun prepareNext(): LevelDefinition? {
        capture()
        if (!session.canAdvance || completed != session.level.id) return null
        return Levels.get(session.level.id + 1)
    }
    fun nextProgress(next: LevelDefinition): PlayerProgress? {
        capture()
        if (!session.canAdvance || next.id != completed + 1) return null
        LevelValidator.validate(next)
        return capture().copy(active = SessionSnapshot(next.id, next.seed, next.version, GameSnapshot(emptyList(), null)))
    }
    fun commitNext(next: LevelDefinition): Boolean = nextProgress(next) != null && session.installNext(next)
    fun advance(): Boolean = prepareNext()?.let { commitNext(it) } ?: false
}
