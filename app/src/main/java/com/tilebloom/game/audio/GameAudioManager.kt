package com.tilebloom.game.audio

import android.content.Context
import android.media.*
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.tilebloom.game.R
import com.tilebloom.game.domain.*

/** Activity-owned resources; retains application context only. All methods run on main. */
class GameAudioManager(context: Context, storageName: String = "garden_audio") : AudioBackend {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(storageName, Context.MODE_PRIVATE)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val system = app.getSystemService(AudioManager::class.java)
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attributes).build()
    private val sounds = mutableMapOf<AudioCue, Int>()
    private val loaded = mutableSetOf<Int>()
    private val streams = java.util.ArrayDeque<Int>()
    private var player: MediaPlayer? = null
    private var prepared = false
    private var wantedMusic = false
    private var closed = false
    private var active = false
    private var ad = false
    private var requested = false
    val policy = AudioPolicy(this, AudioPreferences(prefs.getBoolean("effects", true),
        prefs.getBoolean("music", false), prefs.getBoolean("muted", false))) {
        prefs.edit().putBoolean("effects", it.effects).putBoolean("music", it.music)
            .putBoolean("muted", it.muted).apply()
    }
    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ change ->
            if (!closed) {
                policy.focus(change == AudioManager.AUDIOFOCUS_GAIN)
                if (change == AudioManager.AUDIOFOCUS_LOSS) requested = false
            }
        }, Handler(Looper.getMainLooper())).build()
    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (!closed && status == 0) loaded.add(id) }
        val resources = listOf(R.raw.select, R.raw.reveal, R.raw.hide, R.raw.match, R.raw.hint,
            R.raw.undo, R.raw.restart, R.raw.victory, R.raw.defeat)
        AudioCue.entries.forEachIndexed { index, cue -> sounds[cue] = pool.load(app, resources[index], 1) }
        runCatching {
            MediaPlayer().also { media ->
                player = media; media.setAudioAttributes(attributes)
                app.resources.openRawResourceFd(R.raw.zen_garden).use { media.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                media.isLooping = true; media.setVolume(.18f, .18f)
                media.setOnPreparedListener { if (!closed) { prepared = true; if (wantedMusic) it.start() } }
                media.setOnErrorListener { _, _, _ -> prepared = false; true }
                media.prepareAsync()
            }
        }.onFailure { player?.release(); player = null }
    }
    fun preferences(value: AudioPreferences) { policy.preferences(value); synchronizeFocus() }
    fun foreground(value: Boolean) { active = value; policy.foreground(value); synchronizeFocus() }
    fun advertisement(value: Boolean) { ad = value; policy.advertisement(value); synchronizeFocus() }
    private fun synchronizeFocus() {
        val p = policy.preferences
        val need = active && !ad && !p.muted && (p.effects || p.music)
        if (!need) { policy.focus(false); if (requested) system.abandonAudioFocusRequest(request); requested = false }
        else if (!requested) {
            requested = system.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            policy.focus(requested)
        }
    }
    fun perform(cue: AudioCue, action: () -> Boolean) = policy.perform(cue, SystemClock.uptimeMillis(), action)
    fun play(cue: AudioCue) = policy.play(cue, SystemClock.uptimeMillis())
    override fun music(playing: Boolean) {
        wantedMusic = playing
        if (prepared) runCatching { if (playing) player?.start() else player?.pause() }
    }
    override fun stopEffects() { streams.forEach { pool.stop(it) }; streams.clear() }
    override fun effect(cue: AudioCue) {
        val id = sounds[cue] ?: return
        if (id !in loaded) return // Never queue a stale tap during decoding.
        if (streams.size >= 3) pool.stop(streams.removeFirst())
        val stream = pool.play(id, .30f, .30f, 1, 0, 1f)
        if (stream != 0) streams.addLast(stream)
    }
    fun close() {
        foreground(false); closed = true; pool.release(); player?.release(); player = null
    }
}
