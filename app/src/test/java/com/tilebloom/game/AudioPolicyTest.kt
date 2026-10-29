package com.tilebloom.game

import com.tilebloom.game.domain.*
import org.junit.Assert.*
import org.junit.Test

class AudioPolicyTest {
    private class Fake : AudioBackend {
        var playing = false
        var starts = 0
        var stops = 0
        val cues = mutableListOf<AudioCue>()
        override fun music(playing: Boolean) { this.playing = playing; if (playing) starts++ }
        override fun stopEffects() { stops++ }
        override fun effect(cue: AudioCue) { cues.add(cue) }
    }
    private fun ready(f: Fake, p: AudioPreferences = AudioPreferences()) = AudioPolicy(f,p).also { it.foreground(true); it.focus(true) }
    @Test fun effectsDefaultOn() { assertTrue(AudioPreferences().effects) }
    @Test fun musicDefaultOff() { assertFalse(AudioPreferences().music) }
    @Test fun masterDefaultOff() { assertFalse(AudioPreferences().muted) }
    @Test fun muteImmediatelyStopsBoth() {
        val f=Fake(); val p=ready(f,AudioPreferences(music=true)); assertTrue(f.playing)
        p.preferences(p.preferences.copy(muted=true)); p.play(AudioCue.SELECT,100)
        assertFalse(f.playing); assertTrue(f.cues.isEmpty()); assertTrue(f.stops>0)
    }
    @Test fun unmuteRestoresBoth() {
        val f=Fake(); val p=ready(f,AudioPreferences(music=true))
        p.preferences(p.preferences.copy(muted=true)); p.preferences(p.preferences.copy(muted=false))
        p.play(AudioCue.MATCH,100); assertTrue(f.playing); assertEquals(listOf(AudioCue.MATCH),f.cues)
    }
    @Test fun unmuteDoesNotEnableMusic() {
        val f=Fake(); val p=ready(f); p.preferences(p.preferences.copy(muted=true)); p.preferences(p.preferences.copy(muted=false)); assertFalse(f.playing)
    }
    @Test fun disableMusicKeepsEffects() {
        val f=Fake(); val p=ready(f,AudioPreferences(music=true)); p.preferences(p.preferences.copy(music=false)); p.play(AudioCue.SELECT,1)
        assertFalse(f.playing); assertEquals(1,f.cues.size)
    }
    @Test fun disableEffectsKeepsMusic() {
        val f=Fake(); val p=ready(f,AudioPreferences(music=true)); p.preferences(p.preferences.copy(effects=false)); p.play(AudioCue.SELECT,1)
        assertTrue(f.playing); assertTrue(f.cues.isEmpty())
    }
    @Test fun savedPreferencesRestoreController() {
        var saved=AudioPreferences(); val first=AudioPolicy(Fake(),save={saved=it})
        first.preferences(AudioPreferences(false,true,true)); val second=AudioPolicy(Fake(),saved)
        assertEquals(first.preferences,second.preferences)
    }
    @Test fun backgroundPausesAndSuppressesEffects() {
        val f=Fake(); val p=ready(f,AudioPreferences(music=true)); p.foreground(false); p.play(AudioCue.MATCH,1)
        assertFalse(f.playing); assertTrue(f.cues.isEmpty()); p.foreground(true); assertTrue(f.playing)
    }
    @Test fun focusLossPausesAndGainResumes() {
        val f=Fake(); val p=ready(f,AudioPreferences(music=true)); p.focus(false); p.play(AudioCue.MATCH,1)
        assertFalse(f.playing); assertTrue(f.cues.isEmpty()); p.focus(true); assertTrue(f.playing)
    }
    private fun adSuspension() {
        val f=Fake(); val p=ready(f,AudioPreferences(music=true)); p.advertisement(true); p.play(AudioCue.VICTORY,1)
        assertFalse(f.playing); assertTrue(f.cues.isEmpty()); p.advertisement(false); assertTrue(f.playing)
    }
    @Test fun rewardedSuspends() = adSuspension()
    @Test fun interstitialSuspends() = adSuspension()
    @Test fun dismissalRespectsMuteAndDisabledMusic() {
        val f=Fake(); val p=ready(f,AudioPreferences(music=true)); p.advertisement(true)
        p.preferences(p.preferences.copy(muted=true)); p.advertisement(false); assertFalse(f.playing)
        p.preferences(AudioPreferences()); assertFalse(f.playing)
    }
    @Test fun duplicateLifecycleDoesNotRestartMusic() {
        val f=Fake(); val p=ready(f,AudioPreferences(music=true)); repeat(10) { p.foreground(true); p.focus(true); p.advertisement(false) }; assertEquals(1,f.starts)
    }
    @Test fun recreationReleasesOldPlaybackBeforeNew() {
        val f=Fake(); val p=ready(f,AudioPreferences(music=true)); p.foreground(false)
        val next=Fake(); ready(next,p.preferences); assertFalse(f.playing); assertEquals(1,next.starts)
    }
    @Test fun rapidIdenticalEffectsAreBounded() {
        val f=Fake(); val p=ready(f); repeat(100) { p.play(AudioCue.MATCH,it.toLong()) }; assertEquals(2,f.cues.size)
    }
    private fun game(mystery: Boolean=false) = Game(listOf(Tile(0,Symbol.LEAF,0.0,0.0,0,mystery=mystery),Tile(1,Symbol.LEAF,2.0,0.0,0),Tile(2,Symbol.FLOWER,4.0,0.0,0),Tile(3,Symbol.FLOWER,6.0,0.0,0)))
    private fun tap(g:Game,id:Int)=GameplayAudio.events(g){g.interact(id)}
    @Test fun collectionEmitsSelect() { val g=game(); assertEquals(listOf(AudioCue.SELECT),tap(g,0)) }
    @Test fun blockedEmitsNothing() {
        val g=Game(listOf(Tile(0,Symbol.LEAF,0.0,0.0,0),Tile(1,Symbol.LEAF,0.0,0.0,1)))
        assertTrue(tap(g,0).isEmpty())
    }
    @Test fun revealEmitsReveal() { assertEquals(listOf(AudioCue.REVEAL),tap(game(true),0)) }
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
