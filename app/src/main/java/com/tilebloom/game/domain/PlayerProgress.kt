package com.tilebloom.game.domain

/** Only a contiguous prefix of completions can unlock the introductory journey. */
data class PlayerProgress(val completed: Set<Int> = emptySet(), val active: SessionSnapshot? = null, val undoCredits: Int = 3) {
    val highestUnlocked: Int get() = minOf(Levels.COUNT, (1..Levels.COUNT).firstOrNull { it !in completed } ?: Levels.COUNT)
}

object ProgressCodec {
    const val SCHEMA = 2
    fun encode(p: PlayerProgress): String {
        val s = p.active
        return listOf("$SCHEMA", p.completed.sorted().joinToString(","),
            s?.let { "${it.id},${it.seed},${it.version}" } ?: "",
            s?.game?.moves?.joinToString(";") { "${it.first},${it.second}" } ?: "", p.undoCredits.toString()).joinToString("\n")
    }
    fun decode(raw: String): PlayerProgress {
        require(raw.length <= 8192)
        val lines = raw.split('\n')
        val schema = lines[0].toInt()
        require(schema in 1..SCHEMA)
        val credits = if (schema == 1) 3 else lines.getOrNull(4)?.toIntOrNull() ?: error("Missing credit balance")
        require(credits >= 0)
        val recorded = lines.getOrNull(1).orEmpty().split(',').mapNotNull { it.toIntOrNull() }.toSet()
        val completed = (1..Levels.COUNT).takeWhile { it in recorded }.toSet()
        val active = runCatching {
            val identity = lines[2].split(',')
            val moves = lines.getOrNull(3).orEmpty().takeIf { it.isNotEmpty() }?.split(';')?.map {
                val pair = it.split(','); require(pair.size == 2); PairMove(pair[0].toInt(), pair[1].toInt())
            }.orEmpty()
            SessionSnapshot(identity[0].toInt(), identity[1].toLong(), identity[2].toInt(), GameSnapshot(moves, null))
        }.getOrNull()
        return PlayerProgress(completed, active, credits)
    }
}

/** Validates an entire candidate through the production engine before applying any moves. */
class ProgressController(saved: PlayerProgress = PlayerProgress()) {
    val session = GardenSession()
    val wallet = UndoCreditWallet(saved.undoCredits)
    fun undo(): Boolean = wallet.undo { session.game.undo() }
    private var completed = (1..Levels.COUNT).takeWhile { it in saved.completed }.toSet()
    var recovered = false; private set
    val highestUnlocked get() = PlayerProgress(completed).highestUnlocked
    init {
        saved.active?.let {
            if (it.id !in 1..highestUnlocked || !session.restore(it)) {
                recovered = true
                val id = it.id.takeIf { n -> n in 1..highestUnlocked } ?: 1
                val level = Levels.get(id)
                check(session.restore(SessionSnapshot(id, level.seed, level.version, GameSnapshot(emptyList(), null))))
            }
        }
        capture()
    }
    fun capture(): PlayerProgress {
        if (session.game.status == Status.WON) completed = completed + session.level.id
        return PlayerProgress(completed, session.snapshot().let { it.copy(game = it.game.copy(selectedId = null)) }, wallet.balance)
    }
    fun select(id: Int): Boolean {
        if (id !in 1..highestUnlocked) return false
        if (id == session.level.id) return true
        val level = Levels.get(id)
        return session.restore(SessionSnapshot(id, level.seed, level.version, GameSnapshot(emptyList(), null)))
    }
}
