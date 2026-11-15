package com.wamad.petaljong
import com.wamad.petaljong.domain.*
import com.wamad.petaljong.presentation.board.*
import org.junit.Assert.*
import org.junit.Test
class EndlessTest {
    private val ids = ((1L..40L).toList() + listOf(50L,100L,500L,1000L,10000L,Int.MAX_VALUE.toLong()+1,1000000000000L,Long.MAX_VALUE-1,Long.MAX_VALUE)).distinct()
    @Test fun deterministicMatrixAndWitnesses() {
        ids.forEach { id ->
            val start=System.nanoTime(); val l=Levels.get(id); LevelValidator.validate(l)
            println("Generation HOST JVM level=$id ms=${(System.nanoTime()-start)/1000000} tiles=${l.tiles.size} quality=${l.qualityTargetMet}")
            assertEquals(l,Levels.get(id)); assertEquals(id,l.id); assertTrue(l.tiles.size in if (id >= 6) DifficultyPolicy.profile(id).targetTiles else 24..48)
            assertTrue(l.attempts<=64); if(id>=6) assertTrue(l.candidatesEvaluated<=EndlessGenerator.CANDIDATES)
            assertTrue(l.candidateQuality.all { it.searchNodes <= (if (id >= 6) DifficultyPolicy.profile(id).analysisBudget else 5000) })
            val g=Game(l.tiles); l.winningSelections.forEach { assertTrue(g.collectRevealed(it)) }; assertEquals(Status.WON,g.status)
        }
    }
    @Test fun recentGeometryAndBoardsVary() {
        for(id in 16L..60L) { val l=EndlessGenerator.layout(id); for(d in 1..10) assertNotEquals(EndlessGenerator.geometryFingerprint(l),EndlessGenerator.geometryFingerprint(EndlessGenerator.layout(id-d))) }
        val sample = EndlessGenerator.layout(31)
        assertTrue(AdvancedLayouts.recentDuplicate(sample, listOf(sample)))
        val translated = sample.copy(slots = sample.slots.map { it.copy(x = it.x + 12.0, y = it.y - 4.0) })
        assertEquals(EndlessGenerator.geometryFingerprint(sample), EndlessGenerator.geometryFingerprint(translated))
        assertTrue(AdvancedLayouts.recentDuplicate(translated, listOf(sample)))
        assertEquals(8,(6L..13L).map { EndlessGenerator.layout(it).id.substringBefore('-') }.toSet().size)
        assertEquals(12,(6L..17L).map { Levels.get(it).tiles }.toSet().size)
    }
    @Test fun fallbackIsVerifiedAndReproducible() { val a=EndlessGenerator.generate(100,true); assertEquals(a,EndlessGenerator.generate(100,true)); LevelValidator.validate(a); assertFalse(a.qualityTargetMet); assertEquals(EndlessGenerator.VERSION,a.version)
        val other = EndlessGenerator.generate(101,true)
        assertNotEquals(EndlessGenerator.geometryFingerprint(a.layout),EndlessGenerator.geometryFingerprint(other.layout))
    }
    @Test fun invalidIdsRejected() { assertThrows(IllegalArgumentException::class.java) { Levels.get(0) }; assertThrows(IllegalArgumentException::class.java) { Levels.get(-1) } }
    @Test fun geometryAndHitTestingAllVariants() {
        (6L..37L).forEach { id -> val l=Levels.get(id)
            for((w,h) in listOf(284f to 300f,360f to 520f,600f to 260f)) {
                val geo=BoardGeometry(w,h,tiles=l.tiles); val game=Game(l.tiles)
                l.tiles.forEach { val r=geo.shadow(it); assertTrue(r.left>=0 && r.top>=0 && r.right<=w && r.bottom<=h) }
                l.winningSelections.forEach { tid -> val t=game.remaining.first { it.id==tid }; val p=geo.face(t).center; assertEquals(tid,geo.hitTest(game.tiles,p.x,p.y)?.id); assertTrue(game.collectRevealed(tid)) }
            }
        }
    }
    @Test fun maximumIdentityDoesNotOverflow() {
        val l=Levels.get(Long.MAX_VALUE); val c=ProgressController(PlayerProgress(active=SessionSnapshot(l.id,l.seed,l.version,GameSnapshot(emptyList(),null,l.winningSelections)),highestCompleted=Long.MAX_VALUE))
        assertEquals(Status.WON,c.session.game.status); assertFalse(c.session.canAdvance); assertNull(c.prepareNext()); assertEquals(Long.MAX_VALUE,c.capture().frontier)
    }
}
