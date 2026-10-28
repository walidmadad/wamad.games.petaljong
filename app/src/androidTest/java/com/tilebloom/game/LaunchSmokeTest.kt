package com.tilebloom.game

import android.app.Instrumentation
import android.os.SystemClock
import android.test.ActivityInstrumentationTestCase2
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.TextView
import com.tilebloom.game.domain.Levels
import com.tilebloom.game.presentation.MainActivity
import com.tilebloom.game.presentation.MahjongBoardView
import com.tilebloom.game.presentation.board.BoardPoint
import java.util.concurrent.atomic.AtomicReference

@Suppress("DEPRECATION")
class LaunchSmokeTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    private fun ready(screen: MainActivity) {
        val deadline = SystemClock.uptimeMillis() + 30000
        while (SystemClock.uptimeMillis() < deadline) {
            var loaded = false
            instrumentation.runOnMainSync { loaded = screen.findViewById<MahjongBoardView>(R.id.board)?.width?.let { it > 0 } == true }
            if (loaded) return
            SystemClock.sleep(20)
        }
        fail("Board did not load")
    }
    override fun getActivity(): MainActivity = super.getActivity().also { if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) ready(it) }
    private fun waitNext(id: Long) {
        val deadline = SystemClock.uptimeMillis() + 30000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            if (!com.tilebloom.game.storage.GameStore.transitioning &&
                com.tilebloom.game.storage.GameStore.get(instrumentation.targetContext).session.level.id == id) return
            SystemClock.sleep(20)
        }
        fail("Next Level did not load $id")
    }
    override fun setUp() {
        super.setUp()
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        com.tilebloom.game.storage.ProgressRepository.useTestStorage("test-smoke-progress")
        java.io.File(instrumentation.targetContext.filesDir, "test-smoke-progress").delete()
        java.io.File(instrumentation.targetContext.filesDir, "test-smoke-progress.bak").delete()
    }
    override fun tearDown() {
        super.tearDown()
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        java.io.File(instrumentation.targetContext.filesDir, "test-smoke-progress").delete()
        java.io.File(instrumentation.targetContext.filesDir, "test-smoke-progress.bak").delete()
        com.tilebloom.game.storage.ProgressRepository.useTestStorage(null)
    }
    fun testThreeCreditsThenRejectFourthUndo() {
        activity; instrumentation.waitForIdleSync()
        ui {
            val undo = activity.findViewById<Button>(R.id.undo)
            assertEquals("Undo (3)", undo.text.toString())
            assertTrue(undo.isShown)
            assertFalse(undo.isEnabled)
        }
        repeat(3) {
            tap(firstA); tap(firstB)
            ui {
                activity.findViewById<Button>(R.id.undo).performClick()
                assertEquals("Undo (${2-it})", activity.findViewById<Button>(R.id.undo).text.toString())
            }
        }
        tap(firstA); tap(firstB)
        ui {
            assertFalse(activity.findViewById<Button>(R.id.undo).isEnabled)
            activity.findViewById<Button>(R.id.undo).performClick()
            assertEquals("Undo (0)", activity.findViewById<Button>(R.id.undo).text.toString())
            activity.findViewById<Button>(R.id.restart).performClick()
            assertEquals("Undo (0)", activity.findViewById<Button>(R.id.undo).text.toString())
        }
        assertCount(24)
    }
    fun testSchemaOneMigrationShowsCreditsAndDoesNotRefillOnRelaunch() {
        val original = com.tilebloom.game.domain.ProgressController(
            com.tilebloom.game.domain.PlayerProgress(setOf(1, 2)))
        val move = original.session.level.witness.first()
        assertTrue(original.session.game.match(move.first, move.second))
        val legacy = com.tilebloom.game.domain.ProgressCodec.encode(original.capture())
            .split('\n').take(4).toMutableList().apply { this[0] = "1"; this[1] = "1,2" }.joinToString("\n")
        java.io.File(instrumentation.targetContext.filesDir, "test-smoke-progress").writeText(legacy)
        activity; instrumentation.waitForIdleSync()
        assertCount(26)
        ui {
            val undo = activity.findViewById<Button>(R.id.undo)
            assertTrue(undo.isShown); assertEquals("Undo (3)", undo.text.toString())
            undo.performClick()
            assertEquals("Undo (2)", undo.text.toString())
            activity.finish()
        }
        instrumentation.waitForIdleSync()
        val repo = com.tilebloom.game.storage.ProgressRepository
        repo.awaitWrites(); repo.useTestStorage("test-smoke-progress")
        val saved = repo.load(instrumentation.targetContext)
        assertEquals(setOf(1, 2), saved.completed)
        assertEquals(original.session.snapshot().copy(game = com.tilebloom.game.domain.GameSnapshot(emptyList(), null)), saved.active)
        assertEquals(2, saved.undoCredits)
        val relaunched = instrumentation.startActivitySync(android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        ready(relaunched)
        try {
            instrumentation.waitForIdleSync()
            ui { assertEquals("Undo (2)", relaunched.findViewById<Button>(R.id.undo).text.toString()) }
        } finally { ui { relaunched.finish() } }
    }
    fun testCreditBalanceRemainsVisibleAfterVictory() {
        activity; instrumentation.waitForIdleSync()
        Levels.get(1).witness.forEach { tap(it.first); tap(it.second) }
        ui {
            val undo = activity.findViewById<Button>(R.id.undo)
            assertTrue(undo.isShown)
            assertEquals("Undo (3)", undo.text.toString())
        }
    }
    private fun exhaustCredits() {
        activity; instrumentation.waitForIdleSync()
        repeat(3) { tap(firstA); tap(firstB); ui { activity.findViewById<Button>(R.id.undo).performClick() } }
    }
    fun testZeroCreditsSurviveRelaunch() {
        exhaustCredits()
        ui { activity.finish() }; instrumentation.waitForIdleSync()
        val repo=com.tilebloom.game.storage.ProgressRepository
        repo.awaitWrites(); repo.useTestStorage("test-smoke-progress")
        val launched=instrumentation.startActivitySync(android.content.Intent(instrumentation.targetContext,MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            ready(launched)
            instrumentation.waitForIdleSync()
            ui { assertEquals("Undo (0)",launched.findViewById<Button>(R.id.undo).text.toString()) }
        } finally { ui { launched.finish() } }
    }
    fun testRecreationKeepsZeroCredits() {
        exhaustCredits(); tap(firstA); tap(firstB)
        val monitor=Instrumentation.ActivityMonitor(MainActivity::class.java.name,null,false)
        instrumentation.addMonitor(monitor)
        var replacement: MainActivity?=null
        try {
            ui { activity.recreate() }
            replacement=monitor.waitForActivityWithTimeout(5000) as? MainActivity
            assertNotNull(replacement); instrumentation.waitForIdleSync()
            val screen=replacement!!; ready(screen)
            ui {
                assertEquals("Undo (0)",screen.findViewById<Button>(R.id.undo).text.toString())
                assertFalse(screen.findViewById<Button>(R.id.undo).isEnabled)
            }
            assertCount(22,screen)
        } finally { instrumentation.removeMonitor(monitor); replacement?.let { ui { it.finish() } } }
    }
    fun testStaleOwnerCannotOverwriteNewCreditSnapshot() {
        val repo=com.tilebloom.game.storage.ProgressRepository
        val old=repo.acquire(); val newer=repo.acquire()
        val context=instrumentation.targetContext
        repo.save(context,newer,com.tilebloom.game.domain.PlayerProgress(undoCredits=0))
        repo.save(context,old,com.tilebloom.game.domain.PlayerProgress(undoCredits=3))
        repo.awaitWrites()
        assertEquals(0,repo.load(context).undoCredits)
    }
    fun testInvalidBalanceRecoveryKeepsProgressWithoutFreeRefill() {
        java.io.File(instrumentation.targetContext.filesDir,"test-smoke-progress").writeText("2\n1,2\n\n\n-1")
        val saved=com.tilebloom.game.storage.ProgressRepository.load(instrumentation.targetContext)
        assertEquals(setOf(1,2),saved.completed); assertEquals(0,saved.undoCredits)
    }
    fun testUndoRestoresPairSelectionAndRestartState() {
        activity; instrumentation.waitForIdleSync()
        ui { assertFalse(activity.findViewById<Button>(R.id.undo).isEnabled) }
        tap(firstA); tap(firstB); assertCount(22)
        ui { activity.findViewById<Button>(R.id.undo).performClick() }
        assertCount(24)
        ui {
            assertEquals("Pairs left: 12", activity.findViewById<TextView>(R.id.counter).text.toString())
            assertEquals(0, activity.findViewById<MahjongBoardView>(R.id.board).activeMatchEffectCount)
            assertFalse(activity.findViewById<Button>(R.id.undo).isEnabled)
        }
        tap(firstA)
        ui { assertEquals(firstA, activity.findViewById<MahjongBoardView>(R.id.board).selectedTileId) }
        tap(firstB); assertCount(22)
        ui { activity.findViewById<Button>(R.id.restart).performClick() }
        assertCount(24)
        ui { assertFalse(activity.findViewById<Button>(R.id.undo).isEnabled) }
    }
    fun testDeadlockUndoRecoversThroughPresentation() {
        activity; instrumentation.waitForIdleSync()
        deadlockMoves.forEach { tap(it.first); tap(it.second) }
        ui {
            val undo = activity.findViewById<Button>(R.id.undo)
            assertTrue(undo.isShown); assertTrue(undo.isEnabled)
            assertEquals("Undo (3)", undo.text.toString()); undo.performClick()
        }
        assertCount(24-deadlockMoves.size*2+2)
        ui {
            assertEquals(activity.getString(R.string.instruction), activity.findViewById<TextView>(R.id.feedback).text.toString())
            assertEquals("Undo (2)", activity.findViewById<Button>(R.id.undo).text.toString())
        }
        tap(deadlockMoves.last().first)
        ui { assertEquals(deadlockMoves.last().first, board().selectedTileId) }
    }
    fun testUndoAfterFreshLaunchPersistsShorterHistory() {
        val original = activity
        tap(firstA); tap(firstB); tap(secondA); tap(secondB)
        ui { original.finish() }; instrumentation.waitForIdleSync()
        val repository = com.tilebloom.game.storage.ProgressRepository
        repository.awaitWrites(); repository.useTestStorage("test-smoke-progress")
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val relaunched = instrumentation.startActivitySync(intent) as MainActivity
        try {
            instrumentation.waitForIdleSync(); assertCount(20, relaunched)
            ui { relaunched.findViewById<Button>(R.id.undo).performClick() }
            assertCount(22, relaunched)
            repository.awaitWrites(); repository.useTestStorage("test-smoke-progress")
            val saved = repository.load(instrumentation.targetContext)
            assertEquals(listOf(com.tilebloom.game.domain.PairMove(firstA, firstB)), saved.active!!.game.moves)
        } finally { ui { relaunched.finish() } }
    }
    fun testPreviousPuzzleVersionUpgradePreservesProgress() {
        java.io.File(instrumentation.targetContext.filesDir,"test-smoke-progress")
            .writeText("1\n1,2\n1,0,2\n16,20;0,4")
        com.tilebloom.game.storage.ProgressRepository.useTestStorage("test-smoke-progress")
        activity; instrumentation.waitForIdleSync(); assertCount(28)
        val pair = Levels.get(3).witness.first()
        tap(pair.first); tap(pair.second); assertCount(26)
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        val saved = com.tilebloom.game.storage.ProgressRepository.load(instrumentation.targetContext)
        assertEquals(setOf(1,2),saved.completed)
        assertEquals(3L,saved.active!!.id)
        assertEquals(listOf(pair),saved.active!!.game.moves)
    }
    fun testDurableStoreWithoutBundle() {
        activity
        tap(firstA); tap(firstB)
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        // Drop process memory cache, then read the real AtomicFile without any Bundle.
        com.tilebloom.game.storage.ProgressRepository.useTestStorage("test-smoke-progress")
        val p = com.tilebloom.game.domain.ProgressController(com.tilebloom.game.storage.ProgressRepository.load(instrumentation.targetContext))
        assertEquals(11, p.session.game.remainingPairs)
        assertEquals(listOf(com.tilebloom.game.domain.PairMove(firstA,firstB)), p.session.game.snapshot().moves)
        assertFalse(p.select(2))
    }
    fun testCompletionDurableAndReplayPreservesIt() {
        activity
        Levels.get(1).witness.forEach { tap(it.first); tap(it.second) }
        ui { assertNull(activity.findViewById<Button>(R.id.replay_level)); activity.findViewById<Button>(R.id.next_level).performClick() }
        waitNext(2)
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        com.tilebloom.game.storage.ProgressRepository.useTestStorage("test-smoke-progress")
        val p = com.tilebloom.game.domain.ProgressController(com.tilebloom.game.storage.ProgressRepository.load(instrumentation.targetContext))
        assertEquals(setOf(1), p.capture().completed)
        assertFalse(p.select(1)); assertFalse(p.select(3))
        assertEquals(2L,p.session.level.id)
    }
    fun testStorageFailureIsReportedAndRecovers() {
        val repository = com.tilebloom.game.storage.ProgressRepository
        val blocker = java.io.File(instrumentation.targetContext.filesDir, "test-blocked")
        blocker.writeText("blocks directory creation")
        try {
            repository.useTestStorage("test-blocked/progress")
            val token = repository.acquire()
            repository.save(instrumentation.targetContext, token, com.tilebloom.game.domain.ProgressController().capture())
            repository.awaitWrites()
            assertTrue(repository.lastWriteFailed)
            assertEquals(1L,repository.load(instrumentation.targetContext).highestUnlocked)
        } finally { blocker.delete() }
    }
    fun testFreshActivityLaunchWithoutBundleRestoresMoves() {
        val original = activity
        tap(firstA); tap(firstB)
        ui { original.finish() }
        instrumentation.waitForIdleSync()
        com.tilebloom.game.storage.ProgressRepository.awaitWrites()
        com.tilebloom.game.storage.ProgressRepository.useTestStorage("test-smoke-progress")
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val relaunched = instrumentation.startActivitySync(intent) as MainActivity
        try {
            instrumentation.waitForIdleSync()
            assertCount(22,relaunched)
            ui { assertEquals(0,board(relaunched).activeMatchEffectCount) }
        } finally { ui { relaunched.finish() } }
    }
    private val firstA get() = Levels.get(1).witness[0].first
    private val firstB get() = Levels.get(1).witness[0].second
    private val secondA get() = Levels.get(1).witness[1].first
    private val secondB get() = Levels.get(1).witness[1].second
    private val deadlockMoves get() = listOf(15 to 23,10 to 22,9 to 20,6 to 16,17 to 18,0 to 14)
        .map { com.tilebloom.game.domain.PairMove(it.first,it.second) }
    private val blockedId = 19 // Middle cap: visibly present, blocked on both sides.
    private val differentId get(): Int {
        val tiles = Levels.get(1).tiles
        val symbol = tiles.first { it.id == firstA }.symbol
        return tiles.first { com.tilebloom.game.domain.Rules.available(it, tiles) && it.symbol != symbol }.id
    }
    private fun <T> ui(action: () -> T): T {
        val result = AtomicReference<T>()
        instrumentation.runOnMainSync { result.set(action()) }
        return result.get()
    }
    private fun board(screen: MainActivity = activity) = screen.findViewById<MahjongBoardView>(R.id.board)
    private fun target(id: Int, screen: MainActivity = activity): BoardPoint {
        ready(screen)
        return ui {
        val view = board(screen)
        val local = view.tileCenter(id) ?: error("Tile $id no longer exists")
        val location = IntArray(2); view.getLocationOnScreen(location)
        BoardPoint(local.x + location[0], local.y + location[1])
        }
    }
    private fun send(action: Int, point: BoardPoint, down: Long) {
        val event = MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,point.x,point.y,0)
        try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
    }
    private fun tap(id: Int, screen: MainActivity = activity) {
        instrumentation.waitForIdleSync()
        val p = target(id,screen); val now = SystemClock.uptimeMillis()
        send(MotionEvent.ACTION_DOWN,p,now); send(MotionEvent.ACTION_UP,p,now)
        instrumentation.waitForIdleSync()
    }
    private fun assertCount(expected: Int, screen: MainActivity = activity) {
        ready(screen)
        ui {
            assertEquals(expected,board(screen).remainingTileCount)
            assertEquals(screen.getString(R.string.pairs,expected/2),screen.findViewById<TextView>(R.id.counter).text.toString())
        }
    }
    fun testLaunchAndRestart() {
        val screen = activity; instrumentation.waitForIdleSync()
        ui {
            assertTrue(board().isShown); assertTrue(board().width > 0); assertTrue(board().height > 0)
            assertNotNull(screen.findViewById<Button>(R.id.restart))
            screen.findViewById<Button>(R.id.restart).performClick()
            assertFalse(screen.isFinishing)
        }
        assertCount(24)
    }
    fun testCoordinateWitnessAndPlayAgain() {
        activity; instrumentation.waitForIdleSync(); assertCount(24)
        Levels.get(1).witness.forEachIndexed { index,pair ->
            tap(pair.first); ui { assertEquals(pair.first,board().selectedTileId) }
            tap(pair.second); assertCount(22-index*2); ui { assertNull(board().selectedTileId) }
        }
        ui {
            assertFalse(activity.findViewById<TextView>(R.id.feedback).isShown)
            assertFalse(activity.findViewById<Button>(R.id.restart).isShown)
            assertTrue(activity.findViewById<Button>(R.id.next_level).isShown)
            assertNull(activity.findViewById<Button>(R.id.replay_level))
            fun countTitles(view: android.view.View): Int {
                if (!view.isShown) return 0
                val own = if (view is TextView && view.text.toString() == activity.getString(R.string.won)) 1 else 0
                return own + if (view is android.view.ViewGroup) (0 until view.childCount).sumOf { countTitles(view.getChildAt(it)) } else 0
            }
            assertEquals(1, countTitles(activity.window.decorView))
            activity.findViewById<Button>(R.id.next_level).performClick()
        }
        waitNext(2); assertCount(Levels.get(2).tiles.size)
        tap(Levels.get(2).witness.first().first)
    }
    fun testCoordinateDeadlockAndRestart() {
        activity; instrumentation.waitForIdleSync()
        for (pair in deadlockMoves) { tap(pair.first); tap(pair.second) }
        assertCount(24-deadlockMoves.size*2)
        ui {
            assertTrue(activity.findViewById<TextView>(R.id.feedback).text.toString().contains(activity.getString(R.string.no_moves)))
            assertFalse(activity.findViewById<Button>(R.id.next_level).isShown)
            assertNull(activity.findViewById<Button>(R.id.replay_level))
            assertTrue(activity.findViewById<Button>(R.id.restart).isShown)
            activity.findViewById<Button>(R.id.restart).performClick()
        }
        assertCount(24); ui { assertNull(board().selectedTileId) }
    }
    fun testSelectionBlockedTapAndDrag() {
        activity; instrumentation.waitForIdleSync()
        tap(firstA); tap(blockedId) // Middle cap is visible but laterally blocked; it must not replace selection.
        ui { assertEquals(firstA,board().selectedTileId) }; assertCount(24)
        tap(firstA); ui { assertNull(board().selectedTileId) }
        tap(firstA); tap(differentId) // Available but different symbols: move selection.
        ui { assertEquals(differentId,board().selectedTileId) }; assertCount(24)
        tap(differentId)
        val p = target(firstA); val down = SystemClock.uptimeMillis()
        val distance = ui { ViewConfiguration.get(activity).scaledTouchSlop + 12f }
        send(MotionEvent.ACTION_DOWN,p,down)
        send(MotionEvent.ACTION_MOVE,BoardPoint(p.x+distance,p.y),down)
        send(MotionEvent.ACTION_MOVE,p,down); send(MotionEvent.ACTION_UP,p,down)
        instrumentation.waitForIdleSync(); ui { assertNull(board().selectedTileId) }
        assertCount(24)
    }
    fun testRestartCancelsPendingTouchAndBlockedFeedback() {
        activity; instrumentation.waitForIdleSync(); tap(blockedId)
        val p = target(firstA); val down = SystemClock.uptimeMillis()
        send(MotionEvent.ACTION_DOWN,p,down)
        ui { activity.findViewById<Button>(R.id.restart).performClick() }
        send(MotionEvent.ACTION_UP,p,down)
        SystemClock.sleep(180); instrumentation.waitForIdleSync()
        assertCount(24); ui { assertNull(board().selectedTileId) }
        tap(firstA); ui { assertEquals(firstA,board().selectedTileId) }
    }
    fun testRapidTouchesDuringBlockedFeedbackAndAfterRestart() {
        activity; instrumentation.waitForIdleSync()
        tap(firstA); tap(blockedId); tap(firstB)
        assertCount(22); ui { assertNull(board().selectedTileId) }
        // Rapidly toggle one available tile; it must never match itself.
        val p = target(secondA)
        repeat(12) {
            val now = SystemClock.uptimeMillis()
            send(MotionEvent.ACTION_DOWN,p,now); send(MotionEvent.ACTION_UP,p,now)
        }
        instrumentation.waitForIdleSync(); assertCount(22)
        ui { activity.findViewById<Button>(R.id.restart).performClick() }
        tap(firstA); tap(firstB); assertCount(22)
    }
    fun testBatchedReleaseMovementCancelsTap() {
        activity; instrumentation.waitForIdleSync()
        ui {
            val view = board()
            val p = view.tileCenter(firstA)!!
            val now = SystemClock.uptimeMillis()
            val press = MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,p.x,p.y,0)
            try { view.dispatchTouchEvent(press) } finally { press.recycle() }
            val distance = ViewConfiguration.get(activity).scaledTouchSlop + 12f
            val release = MotionEvent.obtain(now,now+1,MotionEvent.ACTION_MOVE,p.x+distance,p.y,0)
            release.addBatch(now+2,p.x,p.y,1f,1f,0)
            release.action = MotionEvent.ACTION_UP
            try {
                assertEquals(1,release.historySize)
                view.dispatchTouchEvent(release)
            } finally { release.recycle() }
            assertNull(view.selectedTileId)
        }
        assertCount(24); tap(firstA); ui { assertEquals(firstA,board().selectedTileId) }
    }
    fun testMatchEffectsRestartAndCompletion() {
        activity; instrumentation.waitForIdleSync()
        // Dispatch through the real view in one UI turn so no frame can expire copies early.
        ui {
            val view = board()
            fun touch(id: Int) {
                val p = view.tileCenter(id)!!
                val now = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(now, now, action, p.x, p.y, 0)
                    try { view.dispatchTouchEvent(event) } finally { event.recycle() }
                }
            }
            touch(firstA); touch(firstB)
            assertEquals(22, view.remainingTileCount)
            assertNull(view.tileCenter(firstA))
            if (android.animation.ValueAnimator.areAnimatorsEnabled()) {
                assertEquals(2, view.activeMatchEffectCount)
            } else assertEquals(0, view.activeMatchEffectCount)
            touch(secondA); touch(secondB)
            assertEquals(20, view.remainingTileCount)
            activity.findViewById<Button>(R.id.restart).performClick()
            assertEquals(0, view.activeMatchEffectCount)
            assertEquals(24, view.remainingTileCount)
            touch(firstA); touch(firstB)
        }
        SystemClock.sleep(220); instrumentation.waitForIdleSync()
        ui { assertEquals(0, board().activeMatchEffectCount) }
        assertCount(22)
        tap(secondA); tap(secondB); assertCount(20)
    }
    fun testActivityRecreationRestoresPlayedState() {
        val original = activity; instrumentation.waitForIdleSync()
        tap(firstA); tap(firstB); tap(secondA); assertCount(22)
        val monitor = Instrumentation.ActivityMonitor(MainActivity::class.java.name,null,false)
        instrumentation.addMonitor(monitor)
        var replacement: MainActivity? = null
        try {
            ui { original.recreate() }
            replacement = monitor.waitForActivityWithTimeout(5000) as? MainActivity
            assertNotNull("Recreated activity did not launch",replacement)
            instrumentation.waitForIdleSync()
            val restored = replacement!!; ready(restored)
            assertCount(22,restored); ui {
                assertEquals(secondA,board(restored).selectedTileId)
                assertEquals(0,board(restored).activeMatchEffectCount)
            }
            tap(secondB,restored); assertCount(20,restored)
        } finally {
            instrumentation.removeMonitor(monitor)
            replacement?.let { screen -> ui { screen.finish() } }
        }
    }
    fun testCoordinateProgressionBeyondFiveAndDoubleTap() {
        activity
        for (id in 1..7) {
            val definition = Levels.get(id)
            assertCount(definition.tiles.size)
            definition.witness.forEach { tap(it.first); tap(it.second) }
            ui {
                assertFalse(activity.findViewById<Button>(R.id.restart).isShown)
                assertNull(activity.findViewById<Button>(R.id.replay_level))
                val next=activity.findViewById<Button>(R.id.next_level)
                assertTrue(next.isShown); next.performClick(); next.performClick()
            }
            waitNext(id.toLong()+1)
            val nextDefinition=Levels.get(id+1)
            assertCount(nextDefinition.tiles.size)
            val pair=nextDefinition.witness.first(); tap(pair.first); tap(pair.second)
            ui { activity.findViewById<Button>(R.id.restart).performClick() }
            assertCount(nextDefinition.tiles.size)
            assertEquals(id.toLong()+1,com.tilebloom.game.storage.GameStore.get(instrumentation.targetContext).session.level.id)
        }
    }
    fun testPendingVictoryRecreation() {
        activity
        Levels.get(1).witness.forEach { tap(it.first); tap(it.second) }
        val monitor=Instrumentation.ActivityMonitor(MainActivity::class.java.name,null,false)
        instrumentation.addMonitor(monitor)
        var replacement: MainActivity?=null
        try {
            ui { activity.recreate() }
            replacement=monitor.waitForActivityWithTimeout(5000) as? MainActivity
            val screen=replacement!!; ready(screen)
            ui { assertTrue(screen.findViewById<Button>(R.id.next_level).isShown); assertFalse(screen.findViewById<Button>(R.id.restart).isShown) }
            assertCount(0,screen)
        } finally { instrumentation.removeMonitor(monitor); replacement?.let { ui { it.finish() } } }
    }    fun testGeneratedLevelSaveRestoreAndCredits() {
        val repo=com.tilebloom.game.storage.ProgressRepository
        val c=com.tilebloom.game.domain.ProgressController(com.tilebloom.game.domain.PlayerProgress(highestCompleted=5,undoCredits=1))
        java.io.File(instrumentation.targetContext.filesDir,"test-smoke-progress").writeText(com.tilebloom.game.domain.ProgressCodec.encode(c.capture()))
        activity
        val l=Levels.get(6); val m=l.witness.first()
        tap(m.first); tap(m.second)
        ui { activity.findViewById<Button>(R.id.undo).performClick(); assertEquals("Undo (0)",activity.findViewById<Button>(R.id.undo).text.toString()) }
        tap(m.first); tap(m.second); assertCount(l.tiles.size-2)
        ui { activity.finish() }; repo.awaitWrites(); repo.useTestStorage("test-smoke-progress")
        val screen=instrumentation.startActivitySync(android.content.Intent(instrumentation.targetContext,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try { ready(screen); assertCount(l.tiles.size-2,screen)
            ui { assertEquals("Undo (0)",screen.findViewById<Button>(R.id.undo).text.toString()); screen.findViewById<Button>(R.id.restart).performClick() }
            assertCount(l.tiles.size,screen)
            assertEquals(6L,com.tilebloom.game.storage.GameStore.get(instrumentation.targetContext).session.level.id)
        } finally { ui { screen.finish() } }
    }

    fun testTier20To21ThroughCoordinates() = milestoneThroughCoordinates(20)
    fun testTier40To41ThroughCoordinates() = milestoneThroughCoordinates(40)
    private fun milestoneThroughCoordinates(id: Long) {
        val c = com.tilebloom.game.domain.ProgressController(com.tilebloom.game.domain.PlayerProgress(highestCompleted=id-1,undoCredits=1))
        java.io.File(instrumentation.targetContext.filesDir,"test-smoke-progress")
            .writeText(com.tilebloom.game.domain.ProgressCodec.encode(c.capture()))
        activity
        val level=Levels.get(id)
        level.witness.forEach { tap(it.first); tap(it.second) }
        ui { val next=activity.findViewById<Button>(R.id.next_level); next.performClick(); next.performClick() }
        waitNext(id+1)
        val next=Levels.get(id+1)
        assertCount(next.tiles.size)
        assertTrue(com.tilebloom.game.domain.DifficultyPolicy.transitionMessage(id+1)!!.contains("New tiles discovered"))
        val opening=next.witness.first(); tap(opening.first); tap(opening.second)
        ui { activity.findViewById<Button>(R.id.restart).performClick() }
        assertCount(next.tiles.size)
        assertEquals(next,com.tilebloom.game.storage.GameStore.get(instrumentation.targetContext).session.level)
        ui {
            val image=android.graphics.Bitmap.createBitmap(board().width,board().height,android.graphics.Bitmap.Config.ARGB_8888)
            board().draw(android.graphics.Canvas(image)); image.recycle()
        }
        next.witness.forEach { pair -> tap(pair.first); tap(pair.second) }
        ui { assertEquals("Undo (1)",activity.findViewById<Button>(R.id.undo).text.toString()) }
        assertCount(0)
    }

    fun testMilestone21RecreationAndRelaunch() {
        val repo=com.tilebloom.game.storage.ProgressRepository
        val c=com.tilebloom.game.domain.ProgressController(com.tilebloom.game.domain.PlayerProgress(highestCompleted=20,undoCredits=1))
        java.io.File(instrumentation.targetContext.filesDir,"test-smoke-progress")
            .writeText(com.tilebloom.game.domain.ProgressCodec.encode(c.capture()))
        activity
        val level=Levels.get(21); val pair=level.witness.first()
        tap(pair.first); tap(pair.second)
        val monitor=Instrumentation.ActivityMonitor(MainActivity::class.java.name,null,false)
        instrumentation.addMonitor(monitor)
        var replacement: MainActivity?=null
        try {
            ui { activity.recreate() }
            replacement=monitor.waitForActivityWithTimeout(5000) as? MainActivity
            val screen=replacement!!; ready(screen); assertCount(level.tiles.size-2,screen)
            assertEquals(1L,com.tilebloom.game.domain.DifficultyPolicy.tier(com.tilebloom.game.storage.GameStore.get(instrumentation.targetContext).session.level.id))
            ui { assertEquals("Undo (1)",screen.findViewById<Button>(R.id.undo).text.toString()); screen.finish() }
        } finally { instrumentation.removeMonitor(monitor); replacement?.let { ui { it.finish() } } }
        repo.awaitWrites(); repo.useTestStorage("test-smoke-progress")
        val screen=instrumentation.startActivitySync(android.content.Intent(instrumentation.targetContext,MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            ready(screen); assertCount(level.tiles.size-2,screen)
            val saved=com.tilebloom.game.storage.GameStore.get(instrumentation.targetContext)
            assertEquals(level,saved.session.level); assertFalse(saved.select(20)); assertEquals(1,saved.wallet.balance)
            ui { screen.findViewById<Button>(R.id.restart).performClick() }; assertCount(level.tiles.size,screen)
        } finally { ui { screen.finish() } }
    }

}
