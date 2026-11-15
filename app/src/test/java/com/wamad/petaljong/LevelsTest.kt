package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import com.wamad.petaljong.presentation.board.*
import org.junit.Assert.*
import org.junit.Test

class LevelsTest {
    private fun finish(s: GardenSession) { s.level.winningSelections.forEach { assertTrue(s.game.collectRevealed(it)) } }
    private fun rejected(level: LevelDefinition) {
        try { LevelValidator.validate(level); fail("Accepted corrupt level") } catch (_: IllegalArgumentException) { }
    }
    @Test fun introductoryFixtureVersioned() {
        val l = Levels.get(1)
        assertNotEquals(LevelOne.tiles(), l.tiles); assertNotEquals(LevelOne.witness, l.witness); assertEquals(3,l.version)
        assertEquals(24, l.tiles.size); assertEquals(123L, l.seed)
    }
    @Test fun reproducible() { assertEquals(Levels.get(3), Levels.get(3)) }
    @Test fun seedsDiffer() {
        assertNotEquals(LevelGenerator.generate(2, 1, Layouts.terrace).tiles,
            LevelGenerator.generate(2, 2, Layouts.terrace).tiles)
    }
    @Test fun uniqueIds() { (1..5).forEach { val t = Levels.get(it).tiles; assertEquals(t.size, t.map { it.id }.toSet().size) } }
    @Test fun evenCounts() { (1..5).forEach { assertEquals(0, Levels.get(it).tiles.size % 2) } }
    @Test fun evenSymbols() { (1..5).forEach { assertTrue(Levels.get(it).tiles.groupingBy { it.symbol }.eachCount().values.all { n -> n % 2 == 0 }) } }
    @Test fun validGeometry() { (1..5).forEach { LevelValidator.validate(Levels.get(it)) } }
    @Test fun overlapsExist() { (1..5).forEach { val t = Levels.get(it).tiles; assertTrue(t.any { a -> t.any { b -> b.layer > a.layer && Rules.overlaps(a,b) } }) } }
    @Test fun witnessesAreLegalAndComplete() {
        (1..5).forEach { val l = Levels.get(it); val g = Game(l.tiles)
            l.witness.forEach { p -> assertTrue(g.isAvailable(p.first)); assertTrue(g.isAvailable(p.second)); assertTrue(g.match(p.first,p.second)) }
            assertEquals(Status.WON, g.status); assertTrue(g.remaining.isEmpty())
        }
    }
    @Test fun corruptCandidatesRejected() {
        val l = Levels.get(2)
        rejected(l.copy(witness = l.witness.drop(1)))
        rejected(l.copy(witness = listOf(PairMove(0,0)) + l.witness.drop(1)))
        rejected(l.copy(tiles = l.tiles.map { it.copy(id = 0) }))
        rejected(l.copy(tiles = l.tiles.map { it.copy(x = 0.0, y = 0.0) }))
        rejected(l.copy(tiles = l.tiles.map { it.copy(layer = 0) }))
    }
    @Test fun boundedImpossibleLayout() {
        val slots = listOf(TileSlot(0,0.0,0.0,0), TileSlot(1,0.0,0.0,1))
        try { LevelGenerator.generate(9,0,BoardLayout("impossible",slots)); fail() } catch (_: IllegalStateException) { }
        assertEquals(64, LevelGenerator.MAX_ATTEMPTS)
    }
    @Test fun restartExact() { val s = GardenSession(); finish(s); s.advance(); val l = s.level
        s.game.match(l.witness[0].first,l.witness[0].second); s.restart(); assertEquals(l.tiles,s.game.tiles); assertEquals(l,s.level) }
    @Test fun completionEnablesNext() { val s=GardenSession(); assertFalse(s.canAdvance); finish(s); assertTrue(s.canAdvance) }
    @Test fun advancesCorrectly() { val s=GardenSession(); finish(s); assertTrue(s.advance()); assertEquals(Levels.get(2),s.level) }
    @Test fun advanceClearsHistoryAndSelection() { val s=GardenSession(); finish(s); s.advance(); assertNull(s.game.selected); assertTrue(s.game.snapshot().moves.isEmpty()); assertEquals(s.level.tiles,s.game.tiles) }
    @Test fun deadlockNeverAdvances() { val s=GardenSession(); deadlockPath(s.game).forEach { assertTrue(s.game.match(it.first,it.second)) }; assertEquals(Status.PLAYING,s.game.status); assertFalse(s.advance()); assertEquals(1L,s.level.id) }
    @Test fun fifthIsNotTerminal() { val s=GardenSession(); repeat(5) { finish(s); if(it<4) assertTrue(s.advance()) }; assertEquals(5L,s.level.id); assertTrue(s.canAdvance); assertFalse(s.restart()); assertTrue(s.advance()); assertEquals(6L,s.level.id) }
    @Test fun restoresEveryLevel() { val s=GardenSession(); repeat(5) {
        val p=s.level.witness.first(); s.game.match(p.first,p.second)
        val free=s.game.tiles.first { s.game.isAvailable(it.id) }; s.game.collectRevealed(free.id)
        val restored=GardenSession(); assertTrue(restored.restore(s.snapshot())); assertEquals(s.snapshot(),restored.snapshot())
        s.restart(); finish(s); s.advance()
    } }
    @Test fun corruptRestorationAtomic() { val s=GardenSession(); val before=s.snapshot()
        assertFalse(s.restore(before.copy(seed=99))); assertFalse(s.restore(before.copy(id=99)))
        assertFalse(s.restore(before.copy(version=99))); assertFalse(s.restore(before.copy(game=GameSnapshot(listOf(PairMove(0,0)),null))))
        assertEquals(before,s.snapshot())
    }
    @Test fun geometryAcrossLayouts() { (1..5).forEach { id -> val l=Levels.get(id)
        for ((w,h) in listOf(284f to 300f, 360f to 520f, 600f to 260f)) {
            val g=BoardGeometry(w,h,tiles=l.tiles)
            l.tiles.forEach { val r=g.shadow(it); assertTrue(r.left>=0); assertTrue(r.top>=0); assertTrue(r.right<=w); assertTrue(r.bottom<=h) }
            val game=Game(l.tiles)
            l.witness.forEach { p -> for(tid in listOf(p.first,p.second)) { val t=game.remaining.first { it.id==tid }; val c=g.face(t).center; assertEquals(tid,g.hitTest(game.tiles,c.x,c.y)?.id) }; assertTrue(game.match(p.first,p.second)) }
        }
    } }
    @Test fun boundedSeedMatrix() {
        (1..5).forEach { id ->
            val g=Game(Levels.get(id).tiles)
            println("Level $id: ${g.remaining.size} tiles, ${g.remaining.count { g.isAvailable(it.id) }} initially free, ${g.matchingPairs().size} initial matching choices")
        }
        val start=System.nanoTime(); var maxAttempt=0
        listOf(Layouts.terrace,Layouts.woodland,Layouts.courtyard).forEach { layout -> (0L..39L).forEach { seed ->
            val l=LevelGenerator.generate(2,seed,layout); LevelValidator.validate(l); maxAttempt=maxOf(maxAttempt,l.attempts)
        } }
        println("120-board matrix: ${(System.nanoTime()-start)/1_000_000} ms; max attempts=$maxAttempt")
    }
    @Test fun distinctFamiliesAndModestCounts() { assertTrue((1..5).map { Levels.get(it).layout.id }.toSet().size>=3); assertEquals(listOf(24,24,28,30,28),(1..5).map { Levels.get(it).tiles.size }) }
}
