package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import org.junit.Assert.*
import org.junit.Test

class AudioPolicyTest {
    private class Fake : AudioBackend {
        var stops = 0
        val cues = mutableListOf<AudioCue>()
        override fun stopEffects() { stops++ }
        override fun effect(cue: AudioCue) { cues.add(cue) }
    }
    private fun ready(f: Fake, p: AudioPreferences = AudioPreferences()) = AudioPolicy(f,p).also { it.foreground(true); it.focus(true) }
    @Test fun effectsDefaultOnAndUnmuted() { assertTrue(AudioPreferences().effects); assertFalse(AudioPreferences().muted) }
    @Test fun muteSuppressesEveryCueAndUnmuteRestoresEffects() {
        val f=Fake(); val p=ready(f)
        p.preferences(p.preferences.copy(muted=true))
        AudioCue.entries.forEach { p.play(it,100) }
        assertTrue(f.cues.isEmpty()); assertTrue(f.stops>0)
        p.preferences(p.preferences.copy(muted=false)); p.play(AudioCue.SELECT,200)
        assertEquals(listOf(AudioCue.SELECT),f.cues)
    }
    @Test fun savedPreferencesRestoreController() {
        var saved=AudioPreferences(); val first=AudioPolicy(Fake(),save={saved=it})
        first.preferences(AudioPreferences(muted=true)); val f=Fake(); val second=ready(f,saved)
        second.play(AudioCue.MATCH,100); assertTrue(f.cues.isEmpty())
        assertEquals(first.preferences,second.preferences)
    }
    @Test fun backgroundSuppressesWithoutReplay() {
        val f=Fake(); val p=ready(f); p.foreground(false)
        AudioCue.entries.forEach { p.play(it,100) }; p.foreground(true)
        assertTrue(f.cues.isEmpty()); p.play(AudioCue.SELECT,200); assertEquals(1,f.cues.size)
    }
    @Test fun focusLossSuppressesWithoutReplay() {
        val f=Fake(); val p=ready(f); p.focus(false); p.play(AudioCue.MATCH,100)
        p.focus(true); assertTrue(f.cues.isEmpty())
    }
    @Test fun fullScreenAdsSuppressEveryCueWithoutReplay() {
        val f=Fake(); val p=ready(f); p.advertisement(true)
        AudioCue.entries.forEach { p.play(it,100) }; p.advertisement(false)
        assertTrue(f.cues.isEmpty()); p.play(AudioCue.MATCH,200); assertEquals(1,f.cues.size)
        p.advertisement(true); p.preferences(p.preferences.copy(muted=true)); p.advertisement(false)
        p.play(AudioCue.SELECT,300); assertEquals(1,f.cues.size)
    }
    @Test fun rapidAlternatingEffectsAreBoundedPerCue() {
        val f=Fake(); val p=ready(f)
        repeat(100) { p.play(AudioCue.SELECT,it.toLong()); p.play(AudioCue.MATCH,it.toLong()) }
        assertEquals(4,f.cues.size)
    }
    @Test fun levelChangesDoNotResetMute() {
        val f=Fake(); val p=ready(f); p.preferences(p.preferences.copy(muted=true))
        repeat(3) { val g=game(); GameplayAudio.events(g){g.interact(0)}.forEach{p.play(it,100)} }
        assertTrue(p.preferences.muted); assertTrue(f.cues.isEmpty())
    }
    private fun game(mystery: Boolean=false) = Game(listOf(Tile(0,Symbol.LEAF,0.0,0.0,0,mystery=mystery),Tile(1,Symbol.LEAF,2.0,0.0,0),Tile(2,Symbol.FLOWER,4.0,0.0,0),Tile(3,Symbol.FLOWER,6.0,0.0,0)))
    private fun tap(g:Game,id:Int)=GameplayAudio.events(g){g.interact(id)}
    @Test fun collectionEmitsSelect() { val g=game(); assertEquals(listOf(AudioCue.SELECT),tap(g,0)) }
    @Test fun blockedEmitsNothing() {
        val g=Game(listOf(Tile(0,Symbol.LEAF,0.0,0.0,0),Tile(1,Symbol.LEAF,0.0,0.0,1)))
        assertTrue(tap(g,0).isEmpty())
    }
    @Test fun revealEmitsReveal() { assertEquals(listOf(AudioCue.REVEAL),tap(game(true),0)) }
    @Test fun collectingRevealedMysteryDoesNotEmitHide() {
        val g=game(true); tap(g,0)
        assertEquals(listOf(AudioCue.SELECT),tap(g,0))
    }
    @Test fun mismatchHidesMysteryAndCollectsNormal() {
        val g=game(true); tap(g,0)
        assertEquals(listOf(AudioCue.HIDE,AudioCue.SELECT),tap(g,2))
    }
    @Test fun directMysteryMatchDoesNotEmitHide() {
        val g=game(true); tap(g,0)
        assertEquals(listOf(AudioCue.MATCH),tap(g,1))
    }
    @Test fun switchingEmitsHideAndReveal() {
        val g=Game(listOf(Tile(0,Symbol.LEAF,0.0,0.0,0,mystery=true),Tile(1,Symbol.LEAF,2.0,0.0,0,mystery=true)))
        tap(g,0); assertEquals(listOf(AudioCue.HIDE,AudioCue.REVEAL),tap(g,1))
    }
    @Test fun matchEmitsExactlyOnce() {
        val g=game(); tap(g,0); assertEquals(listOf(AudioCue.MATCH),tap(g,1)); assertTrue(tap(g,1).isEmpty())
    }
    @Test fun victoryEmitsOnce() {
        val g=game(); listOf(0,1,2).forEach{tap(g,it)}
        assertEquals(listOf(AudioCue.MATCH,AudioCue.VICTORY),tap(g,3)); assertTrue(tap(g,3).isEmpty())
    }
    @Test fun defeatEmitsOnce() {
        val g=Game(listOf(Symbol.LEAF,Symbol.FLOWER,Symbol.LOTUS,Symbol.VEINED_LEAF).mapIndexed{i,s->Tile(i,s,i*2.0,0.0,0)})
        (0..2).forEach{tap(g,it)}; assertEquals(listOf(AudioCue.SELECT,AudioCue.DEFEAT),tap(g,3)); assertTrue(tap(g,3).isEmpty())
    }
    @Test fun observingDoesNotChangeUndoOrSnapshot() {
        val a=game(true); val b=game(true); listOf(0,0,1,2).forEach { tap(a,it); b.interact(it) }
        assertEquals(b.snapshot(),a.snapshot()); assertEquals(b.undo(),a.undo()); assertEquals(b.snapshot(),a.snapshot())
    }
    @Test fun undoOnlyEmitsAfterSuccessfulAction() {
        val f=Fake(); val p=ready(f); val g=game()
        assertFalse(p.perform(AudioCue.UNDO,1){g.undo()}); assertTrue(f.cues.isEmpty())
        tap(g,0); assertTrue(p.perform(AudioCue.UNDO,100){g.undo()})
        assertEquals(listOf(AudioCue.UNDO),f.cues)
        assertFalse(p.perform(AudioCue.UNDO,200){g.undo()}); assertEquals(1,f.cues.size)
    }
    @Test fun hintAndRestartFollowAcceptedActions() {
        val f=Fake(); val p=ready(f)
        assertFalse(p.perform(AudioCue.HINT,0){false})
        assertTrue(p.perform(AudioCue.HINT,100){true})
        assertTrue(p.perform(AudioCue.RESTART,200){true})
        assertEquals(listOf(AudioCue.HINT,AudioCue.RESTART),f.cues)
    }

}
