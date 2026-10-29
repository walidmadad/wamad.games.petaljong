package com.tilebloom.game.domain

data class AudioPreferences(val effects: Boolean = true, val music: Boolean = false, val muted: Boolean = false)
enum class AudioCue { SELECT, REVEAL, HIDE, MATCH, HINT, UNDO, RESTART, VICTORY, DEFEAT }
interface AudioBackend {
    fun music(playing: Boolean)
    fun stopEffects()
    fun effect(cue: AudioCue)
}
/** Main-thread state machine. Preferences remain independent of temporary interruptions. */
class AudioPolicy(private val backend: AudioBackend, initial: AudioPreferences = AudioPreferences(),
    private val save: (AudioPreferences) -> Unit = {}) {
    var preferences = initial; private set
    private var foreground = false
    private var advertisement = false
    private var focus = false
    private var musicPlaying = false
    private var lastCue: AudioCue? = null
    private var lastTime = Long.MIN_VALUE
    fun preferences(value: AudioPreferences) { preferences = value; save(value); reconcile() }
    fun foreground(value: Boolean) { foreground = value; reconcile() }
    fun advertisement(value: Boolean) { advertisement = value; reconcile() }
    fun focus(value: Boolean) { focus = value; reconcile() }
    private val permitted get() = foreground && !advertisement && focus && !preferences.muted
    private fun reconcile() {
        val playing = permitted && preferences.music
        if (playing != musicPlaying) { musicPlaying = playing; backend.music(playing) }
        if (!permitted || !preferences.effects) backend.stopEffects()
    }
    fun perform(cue: AudioCue, now: Long, action: () -> Boolean): Boolean {
        if (!action()) return false
        play(cue, now)
        return true
    }
    fun play(cue: AudioCue, now: Long) {
        if (!permitted || !preferences.effects) return
        if (lastCue == cue && now - lastTime in 0..65) return
        lastCue = cue; lastTime = now; backend.effect(cue)
    }
}
/** Observe only a successful player transition, never restoration or animation callbacks. */
object GameplayAudio {
    fun events(before: Game, action: () -> Boolean): List<AudioCue> {
        val pairs = before.remainingPairs
        val mystery = before.activeMysteryId
        val remaining = before.remaining.size
        val status = before.status
        if (!action()) return emptyList()
        return buildList {
            if (mystery != null && mystery != before.activeMysteryId) add(AudioCue.HIDE)
            if (before.activeMysteryId != null && before.activeMysteryId != mystery) add(AudioCue.REVEAL)
            if (before.remainingPairs < pairs) add(AudioCue.MATCH)
            else if (before.remaining.size < remaining) add(AudioCue.SELECT)
            if (status == Status.PLAYING && before.status != status)
                add(if (before.status == Status.WON) AudioCue.VICTORY else AudioCue.DEFEAT)
        }
    }
}
