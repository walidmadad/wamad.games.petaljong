package com.wamad.petaljong.domain

data class AudioPreferences(val effects: Boolean = true, val muted: Boolean = false)
enum class AudioCue { SELECT, REVEAL, HIDE, MATCH, HINT, UNDO, RESTART, VICTORY, DEFEAT, BUTTON }
interface AudioBackend {
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
    private val lastTimes = mutableMapOf<AudioCue, Long>()
    fun preferences(value: AudioPreferences) { preferences = value; save(value); reconcile() }
    fun foreground(value: Boolean) { foreground = value; reconcile() }
    fun advertisement(value: Boolean) { advertisement = value; reconcile() }
    fun focus(value: Boolean) { focus = value; reconcile() }
    private val permitted get() = foreground && !advertisement && focus && !preferences.muted
    private fun reconcile() {
        if (!permitted || !preferences.effects) backend.stopEffects()
    }
    fun perform(cue: AudioCue, now: Long, action: () -> Boolean): Boolean {
        if (!action()) return false
        play(cue, now)
        return true
    }
    fun play(cue: AudioCue, now: Long) {
        if (!permitted || !preferences.effects) return
        if (lastTimes[cue]?.let { now - it in 0..65 } == true) return
        lastTimes[cue] = now; backend.effect(cue)
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
            if (mystery != null && mystery != before.activeMysteryId && before.remaining.any { it.id == mystery }) add(AudioCue.HIDE)
            if (before.activeMysteryId != null && before.activeMysteryId != mystery) add(AudioCue.REVEAL)
            if (before.remainingPairs < pairs) add(AudioCue.MATCH)
            else if (before.remaining.size < remaining) add(AudioCue.SELECT)
            if (status == Status.PLAYING && before.status != status)
                add(if (before.status == Status.WON) AudioCue.VICTORY else AudioCue.DEFEAT)
        }
    }
}
