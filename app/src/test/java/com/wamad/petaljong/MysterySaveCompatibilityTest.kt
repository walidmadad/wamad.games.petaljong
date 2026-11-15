package com.wamad.petaljong

import com.wamad.petaljong.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class MysterySaveCompatibilityTest {
    @Test fun v10BoardsMatchTheActualPreviousCompiledGenerator() {
        val expected=mapOf(
            8L to "37a7d3a37d456db19b80fb1947361324eb95bbfd4199ee63068bf8c77b0e2cb4",
            12L to "508a748bf4679d26ccbd6c2a95b7adf189aa90a20d88668a59d3145e1e7cb173",
            31L to "ee971f47ecda46cf73c9c8c2b4de537c90cfb14ce52e6b91a82db20cf427f99f",
            75L to "1d689cc69369200471078f0965dc69edee292b1dd45cab16e8058f61034b0d8f",
            101L to "3bfdda730f38b8dfc917c1eea3c31438ce26166db2ebff1a91921b8304e3de7f",
            1001L to "15aabb999294640a9006e4964d4d1181720a497b5c93ca226a36c208a550d3a2")
        for ((id, hash) in expected) {
            val level=VersionTenGenerator.generate(id)
            val value="${level.seed}|${level.tiles}|${level.winningSelections}"
            val actual=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
            assertEquals("Saved v10 board $id",hash,actual)
            val game=Game(level.tiles)
            level.winningSelections.take(8).forEach { assertTrue(game.collectRevealed(it)) }
            val saved=PlayerProgress(active=SessionSnapshot(id,level.seed,10,game.snapshot()),highestCompleted=id-1,undoCredits=2,hintCredits=1)
            val restored=ProgressController(ProgressCodec.decode(ProgressCodec.encode(saved)))
            assertFalse(restored.recovered); assertEquals(level.tiles,restored.session.level.tiles)
            assertEquals(game.remaining,restored.session.game.remaining)
            assertEquals(game.tray,restored.session.game.tray)
            assertEquals(2,restored.wallet.balance); assertEquals(1,restored.hintCredits)
        }
    }
}
