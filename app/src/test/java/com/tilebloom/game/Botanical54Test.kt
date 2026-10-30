package com.tilebloom.game

import com.tilebloom.game.domain.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class Botanical54Test {
    @Test fun twelveAppendOnlyIdentitiesHaveDistinctProductionResourcesAndTranslations() {
        assertEquals((0..34).toList(),Symbol.entries.map { it.stableId })
        assertEquals(12,BotanicalPalette.additions.size)
        assertEquals(listOf("LEAF","LOTUS","FERN","FLOWER","BERRIES","SUNFLOWER","MUSHROOM","BUTTERFLY","BAMBOO","ACORN","MAPLE_LEAF","PINECONE","IRIS","GINKGO_LEAF","CHERRY_BLOSSOM","DRAGONFLY","CACTUS","LAVENDER","LADYBUG","SNAIL","VEINED_LEAF","DOUBLE_LEAF","YELLOW_DAISY"),Symbol.entries.take(23).map { it.name })
        val renderer=File("app/src/main/java/com/tilebloom/game/presentation/MahjongBoardView.kt").readText()
        val bytes=BotanicalPalette.additions.map { s ->
            val name=s.name.lowercase()
            val f=File("app/src/main/res/drawable-nodpi/botanical_$name.webp")
            assertTrue(f.isFile); assertTrue(f.length()>1000)
            assertTrue(renderer.contains("R.drawable.botanical_$name"))
            for(folder in listOf("values","values-fr","values-de","values-es","values-pt-rBR","values-ar","values-hi","values-ja")) {
                assertTrue(File("app/src/main/res/$folder/strings.xml").readText().contains("name=\"symbol_$name\""))
            }
            java.security.MessageDigest.getInstance("SHA-256").digest(f.readBytes()).toList()
        }
        assertEquals(12,bytes.distinct().size)
    }
    @Test fun everyVariantMatchesOnlyItsOwnStableIdentityAndUsesOrdinaryTrayAndUndo() {
        for(a in BotanicalPalette.additions) for(b in BotanicalPalette.additions.filter { it!=a }) {
            val g=Game(listOf(Tile(0,a,0.0,0.0,0),Tile(1,b,2.0,0.0,0),Tile(2,a,4.0,0.0,0),Tile(3,b,6.0,0.0,0)))
            assertTrue(g.select(0)); assertTrue(g.select(1)); assertEquals(2,g.tray.size)
            assertTrue(g.select(2)); assertEquals(listOf(b),g.tray.map { it.symbol }); assertEquals(4,g.traySlots.size)
            assertTrue(g.undo()); assertEquals(listOf(a,b),g.tray.map { it.symbol })
            assertTrue(g.select(2)); assertTrue(g.select(3)); assertEquals(Status.WON,g.status)
        }
    }
    @Test fun explicitFamilyRequirementsCooccurAndEachHasPartners() {
        for(id in listOf(8L,12,16,23,31,50,75,150,501)) {
            val l=Levels.get(id)
            LevelValidator.validate(l)
            val symbols=l.tiles.map { it.symbol }
            assertTrue(symbols.containsAll(BotanicalPalette.required(id)))
            assertTrue(BotanicalPalette.relatedGroups(symbols).size >= if(id>=31) 2 else 1)
            assertTrue(l.tiles.groupingBy { it.symbol }.eachCount().values.all { it>=2 && it%2==0 })
            assertTrue(l.candidatesEvaluated in 2..EndlessGenerator.CANDIDATES)
            if(id>=31) assertTrue(l.tiles.size<=60)
        }
    }
    @Test fun mysteriesKeepVisibleNormalPartnersAndWinningActionsReplayProduction() {
        for(id in listOf(12L,23,40,75,150,501)) {
            val l=Levels.get(id); val g=Game(l.tiles)
            for(t in l.tiles.filter { it.mystery }) assertTrue(l.tiles.any { it.id!=t.id && it.symbol==t.symbol && !it.mystery })
            for(action in l.winningActions) {
                assertTrue(if(action.kind==ActionKind.REVEAL) g.reveal(action.tileId) else g.select(action.tileId))
                assertTrue(g.remaining.count { it.revealed }<=1); assertNotEquals(Status.LOST,g.status)
            }
            assertEquals(Status.WON,g.status)
            val a=l.strategicAnalysis!!
            assertTrue(a.consequentialTrayChoices>0); assertTrue(a.provenImmediateTraps>0)
        }
    }
    @Test fun oldVersionElevenSaveKeepsExactPuzzleWalletHistoryAndFrontier() {
        val old=VersionElevenGenerator.generate(75)
        val text=old.tiles.joinToString(";"){ "${it.id}:${it.symbol.stableId}:${it.x}:${it.y}:${it.layer}:${it.mystery}" }+":"+old.winningSelections.joinToString(",")
        val digest=java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString(""){ "%02x".format(it) }
        assertEquals("38a4e7753b8f6cdd3e02f515be1a503cbb46e6b6266c4c844dd1a94ab1cb84b7",digest)
        val game=Game(old.tiles)
        old.winningSelections.take(5).forEach { assertTrue(game.collectRevealed(it)) }
        val saved=PlayerProgress(active=SessionSnapshot(old.id,old.seed,11,game.snapshot()),highestCompleted=74,undoCredits=2,hintCredits=1)
        val restored=ProgressController(ProgressCodec.decode(ProgressCodec.encode(saved)))
        assertFalse(restored.recovered); assertEquals(old,restored.session.level)
        assertEquals(game.snapshot(),restored.session.game.snapshot()); assertEquals(game.tray,restored.session.game.tray)
        assertEquals(2,restored.wallet.balance); assertEquals(1,restored.hintCredits); assertEquals(74L,restored.capture().frontier)
        assertTrue(restored.undo()); assertEquals(1,restored.wallet.balance)
        assertEquals(12,Levels.get(76).version)
    }
    @Test fun boundedPlanningAndUnknownAreReproducible() {
        val l=Levels.get(75)
        for(depth in listOf(3,6)) {
            val a=PlanningPlayer.play(l,SimulatedPlayer.Strategy.LOOKAHEAD,200,depth,17)
            assertEquals(a,PlanningPlayer.play(l,SimulatedPlayer.Strategy.LOOKAHEAD,200,depth,17))
            assertTrue(a.nodes<=200)
        }
        val g=Game(l.tiles)
        assertEquals(Solvability.UNKNOWN_BUDGET_EXCEEDED,TrayAnalyzer.analyze(l.tiles,g.snapshot(),0).result.outcome)
        val impossible=Game(listOf(Tile(0,Symbol.LEAF,0.0,0.0,0),Tile(1,Symbol.FLOWER,2.0,0.0,0)))
        assertEquals(Solvability.UNSOLVABLE,TrayAnalyzer.analyze(impossible.tiles,impossible.snapshot(),100).result.outcome)
    }
    @Test fun expertHeldoutGreedyImprovesAgainstFrozenBaselineWithoutGivingPlayersTheWitness() {
        var oldWins=0; var newWins=0; var planningWins=0
        var oldPressure=0; var newPressure=0
        for(id in listOf(50L,51,101)) {
            val old=VersionElevenGenerator.generate(id); val fresh=Levels.get(id)
            oldWins+=(10..21).count { PlanningPlayer.play(old,SimulatedPlayer.Strategy.GREEDY,tieBreak=it).won }
            newWins+=(10..21).count { PlanningPlayer.play(fresh,SimulatedPlayer.Strategy.GREEDY,tieBreak=it).won }
            planningWins+=(10..11).count { PlanningPlayer.play(fresh,SimulatedPlayer.Strategy.LOOKAHEAD,1800,6,it).won }
            oldPressure+=StrategicAnalyzer.analyze(old).witnessThreeHeld
            newPressure+=fresh.strategicAnalysis!!.witnessThreeHeld
        }
        assertTrue("Held-out greedy wins: old=$oldWins new=$newWins",newWins<oldWins)
        assertTrue("Planning=$planningWins/6 vs greedy=$newWins/36",planningWins*6>newWins)
        assertTrue(newPressure>oldPressure)
    }
    @Test fun analyzerFindsDirectMysteryEscapeFromAnOtherwiseFullTray() {
        val tiles=listOf(
            Tile(0,Symbol.LEAF,0.0,0.0,0),Tile(1,Symbol.LOTUS,2.0,0.0,0),Tile(2,Symbol.FERN,4.0,0.0,0),
            Tile(3,Symbol.FLOWER,6.0,0.0,1,mystery=true),Tile(4,Symbol.FLOWER,8.0,0.0,0),
            Tile(5,Symbol.LEAF,5.5,-0.5,0),Tile(6,Symbol.LOTUS,6.5,-0.5,0),Tile(7,Symbol.FERN,5.5,0.5,0))
        val g=Game(tiles);listOf(0,1,2).forEach { assertTrue(g.select(it)) }
        assertEquals(3,g.tray.size)
        assertEquals(listOf(3,4),g.remaining.filter { g.isAvailable(it.id) }.map { it.id })
        for(id in listOf(3,4)) {
            val branch=g.fork();if(id==3) assertTrue(branch.reveal(id))
            assertTrue(branch.select(id));assertEquals(Status.LOST,branch.status)
        }
        val before=g.snapshot();val result=TrayAnalyzer.analyze(tiles,before,300)
        assertEquals(Solvability.SOLVABLE,result.result.outcome)
        assertEquals(before,g.snapshot())
        val twoMysteries=tiles.map { if(it.id==4) it.copy(mystery=true) else it }
        val hidden=Game(twoMysteries);listOf(0,1,2).forEach { assertTrue(hidden.select(it)) }
        assertEquals(Solvability.UNSOLVABLE,TrayAnalyzer.analyze(twoMysteries,hidden.snapshot(),300).result.outcome)
        assertTrue(g.reveal(3));assertTrue(g.select(4));assertEquals(3,g.tray.size)
        assertTrue(g.select(5));assertTrue(g.select(6));assertTrue(g.select(7));assertEquals(Status.WON,g.status)
    }
    @Test fun reportedFourDistinctTrapsMatchEveryProductionAlternative() {
        val l=Levels.get(75);val game=Game(l.tiles);var traps=0;var consequential=0
        for(id in l.winningSelections) {
            val losses=game.remaining.filter { game.isAvailable(it.id) }.count { tile ->
                val branch=game.fork()
                if(tile.concealed) assertTrue(branch.reveal(tile.id))
                assertTrue(branch.select(tile.id));branch.status==Status.LOST
            }
            traps+=losses;if(losses>0) consequential++
            assertTrue(game.collectRevealed(id))
        }
        assertEquals(traps,l.strategicAnalysis!!.provenImmediateTraps)
        assertEquals(consequential,l.strategicAnalysis!!.consequentialTrayChoices)
    }
    @Test fun playerTracesReplayThroughProductionRevealsAndDirectMatches() {
        val level=Levels.get(75)
        for(strategy in SimulatedPlayer.Strategy.entries) {
            val result=PlanningPlayer.play(level,strategy,300)
            val replay=Game(level.tiles)
            assertTrue(replay.restore(result.snapshot!!,preserveTemporary=true))
            assertEquals(result.actions,replay.snapshot().selections)
            assertEquals(result.won,replay.status==Status.WON)
            assertTrue(replay.remaining.count { it.revealed }<=1)
        }
    }
    @Test fun alreadyQualifiedFirstCandidateStopsAfterMinimumComparison() {
        val level=EndlessGenerator.generate(1001)
        assertTrue(level.qualityTargetMet);assertTrue(level.candidateQuality.first().targetMet)
        assertEquals(2,level.candidatesEvaluated);assertEquals(1,level.attempts)
        assertEquals("pyramid-4-0",level.layout.id);LevelValidator.validate(level)
    }
    @Test fun indexedAvailabilityMatchesProductionAfterEveryWitnessAction() {
        for(id in listOf(8L,16,31,51,101,501)) {
            val l=Levels.get(id); val index=AvailabilityIndex(l.tiles); val game=Game(l.tiles)
            for(action in l.winningActions) {
                val expected=game.tiles.filter { Rules.available(it,game.tiles) }.map { it.id }
                assertEquals(expected,game.tiles.filter { game.isAvailable(it.id) }.map { it.id })
                assertEquals(expected,index.free(game.remaining).map { it.id })
                assertTrue(if(action.kind==ActionKind.REVEAL) game.reveal(action.tileId) else game.select(action.tileId))
            }
        }
        val original=LevelOne.tiles();val index=AvailabilityIndex(original)
        for(mask in 0..255) {
            val current=original.mapIndexed { i,t -> t.copy(removed=i<8 && mask and (1 shl i)!=0) }
            assertEquals(current.filter { Rules.available(it,current) }.map { it.id },index.free(current).map { it.id })
        }
        val signBit=(0..62).map { Tile(it,Symbol.LEAF,it*2.0,0.0,0) }+Tile(63,Symbol.LEAF,0.0,0.0,1)
        val signed=Game(signBit);assertFalse(signed.isAvailable(0));assertTrue(signed.isAvailable(63))
        assertTrue(signed.select(63));assertTrue(signed.isAvailable(0));assertFalse(signed.isAvailable(63))
        val big=(0..70).map { Tile(it,Symbol.LEAF,it*2.0,0.0,0) }
        assertEquals(big,AvailabilityIndex(big).free(big))
    }
    @Test fun unseenMysteryIdentitiesCannotAffectFirstPlanningDecision() {
        val tiles=listOf(Tile(0,Symbol.LEAF,0.0,0.0,0,mystery=true),Tile(1,Symbol.LOTUS,2.0,0.0,0,mystery=true),
            Tile(2,Symbol.FERN,0.0,0.0,1),Tile(3,Symbol.FLOWER,2.0,0.0,1),Tile(4,Symbol.FERN,4.0,0.0,0),Tile(5,Symbol.FLOWER,6.0,0.0,0))
        fun level(ts:List<Tile>)=LevelDefinition(31,0,12,BoardLayout("hidden",ts.map { TileSlot(it.id,it.x,it.y,it.layer) }),ts,emptyList(),0)
        val first=PlanningPlayer.play(level(tiles),SimulatedPlayer.Strategy.LOOKAHEAD,200).actions.first()
        for(s in BotanicalPalette.additions) assertEquals(first,PlanningPlayer.play(level(tiles.map { if(it.id<2) it.copy(symbol=s) else it }),SimulatedPlayer.Strategy.LOOKAHEAD,200).actions.first())
    }
}
