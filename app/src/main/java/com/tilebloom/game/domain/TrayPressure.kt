package com.tilebloom.game.domain

import java.util.Random

/** Fixed-budget individual-action probes through Game; measured susceptibility, not a solver. */
data class TrayPressure(val probes: Int, val losses: Int, val wins: Int, val threeHeldStates: Int) {
    companion object {
        fun measure(level: LevelDefinition, probes: Int = 12): TrayPressure {
            require(probes in 1..24)
            val random = Random(level.seed xor 290029L)
            var losses = 0; var wins = 0; var pressure = 0
            repeat(probes) {
                val game = Game(level.tiles)
                repeat(level.tiles.size) {
                    if (game.status == Status.PLAYING) {
                        game.remaining.filter { it.concealed && game.isAvailable(it.id) }.forEach { check(game.reveal(it.id)) }
                        val free = game.remaining.filter { game.isAvailable(it.id) }
                        val matches = free.filter { t -> game.tray.any { it.symbol == t.symbol } }
                        val pool = if (matches.isNotEmpty() && random.nextBoolean()) matches else free
                        check(game.select(pool[random.nextInt(pool.size)].id))
                        if (game.tray.size == 3) pressure++
                    }
                }
                if (game.status == Status.WON) wins++ else losses++
            }
            return TrayPressure(probes, losses, wins, pressure)
        }
    }
}
