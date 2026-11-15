package com.wamad.petaljong.storage

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.wamad.petaljong.domain.*
import java.io.File
import java.util.concurrent.Executors

/** One ordered writer per process; no lifecycle callback ever submits an old snapshot. */
object ProgressRepository {
    private val writer = Executors.newSingleThreadExecutor()
    @Volatile var lastWriteFailed = false; private set
    private var latest: String? = null
    private var owner = 0L
    private var storageName = "player-progress-v1"
    /** Instrumentation only: call before acquiring an activity, drain before cleanup. */
    fun awaitWrites() { writer.submit {}.get() }
    @Synchronized fun useTestStorage(name: String?) {
        require(name == null || name.startsWith("test-"))
        GameStore.resetForTestStorage()
        storageName = name ?: "player-progress-v1"; latest = null; owner++; lastWriteFailed = false
    }
    @Synchronized fun acquire(): Long = ++owner
    @Synchronized fun load(context: Context): PlayerProgress {
        val raw = latest ?: runCatching { file(context).openRead().bufferedReader().use { it.readTextBounded() } }.onFailure { if (it !is java.io.FileNotFoundException) Log.w("TileBloomSave", "Cannot read save", it) }.getOrNull()
        return raw?.let { runCatching { ProgressCodec.decode(it).also { p -> if (p.active == null) Log.w("TileBloomSave", "Missing or malformed board; safe board loaded") } }.getOrElse {
            Log.w("TileBloomSave", "Invalid save envelope; attempting conservative credit recovery", it)
            // A malformed schema-2 balance is never interpreted as a new introductory allocation.
            val lines = raw.split('\n').toMutableList()
            if (lines.firstOrNull() in listOf("2", "3", "4", "5", "6") && raw.length <= 8192) {
                while (lines.size < 5) lines.add("")
                lines[4] = lines[4].toIntOrNull()?.takeIf { it >= 0 }?.toString() ?: "0"
                if (lines[0] in listOf("5", "6")) {
                    while (lines.size < 6) lines.add("")
                    lines[5] = lines[5].toIntOrNull()?.takeIf { it in 0..3 }?.toString() ?: "0"
                }
                runCatching { ProgressCodec.decode(lines.joinToString("\n")) }.getOrElse { PlayerProgress(undoCredits = 0, hintCredits = 0) }
            } else PlayerProgress()
        } } ?: PlayerProgress()
    }
    @Synchronized fun save(context: Context, token: Long, progress: PlayerProgress) {
        if (token != owner) return
        val raw = ProgressCodec.encode(progress)
        if (raw == latest) return
        latest = raw
        val target = file(context)
        writer.execute {
            var stream: java.io.FileOutputStream? = null
            try {
                stream = target.startWrite()
                stream.write(raw.toByteArray(Charsets.UTF_8))
                target.finishWrite(stream)
                lastWriteFailed = false
            } catch (e: Exception) {
                lastWriteFailed = true
                target.failWrite(stream)
                synchronized(this) { if (latest == raw) latest = null }
                Log.e("TileBloomSave", "Save failed; durable progress was not confirmed", e)
            }
        }
    }
    /** Transition caller waits on the ordered writer; latest is published only after fsync. */
    fun saveDurably(context: Context, token: Long, progress: PlayerProgress): Boolean {
        val raw = ProgressCodec.encode(progress)
        val target = synchronized(this) { if (token != owner) return false else file(context) }
        return writer.submit<Boolean> {
            var stream: java.io.FileOutputStream? = null
            try {
                stream = target.startWrite(); stream.write(raw.toByteArray(Charsets.UTF_8)); target.finishWrite(stream)
                synchronized(this) { latest = raw; lastWriteFailed = false }
                true
            } catch (e: Exception) {
                target.failWrite(stream); lastWriteFailed = true
                Log.e("TileBloomSave", "Transition not committed", e); false
            }
        }.get()
    }
    private fun java.io.BufferedReader.readTextBounded(): String {
        val chars = CharArray(8193)
        var count = 0
        while (count < chars.size) { val n = read(chars, count, chars.size-count); if (n < 0) break; count += n }
        require(count <= 8192) { "Oversized save" }
        return String(chars, 0, count)
    }
    private fun file(context: Context) = AtomicFile(File(context.filesDir, storageName))
}
