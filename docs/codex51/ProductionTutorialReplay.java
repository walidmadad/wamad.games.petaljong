import com.tilebloom.game.domain.*;
import java.util.*;
public class Tutorial51Replay {
    static void verify(boolean value) { if (!value) throw new AssertionError("Production tutorial replay failed"); }
    public static void main(String[] args) {
        var state = new ProgressController(); var guide = state.getTutorial();
        var game = state.getSession().getGame(); var initial = game.snapshot();
        verify(guide.begin(0)); verify(guide.continueGuide());
        int first = guide.getTarget(); verify(game.isAvailable(first)); verify(game.select(first)); guide.collected();
        verify(game.getTray().size()==1 && game.getTraySlots().size()==4);
        verify(guide.continueGuide()); verify(state.undo()); verify(game.snapshot().equals(initial)); verify(!state.undo());
        var hint = HintPolicy.INSTANCE.suggest(state.getSession().getLevel().getTiles(),game.snapshot(),300);
        verify(hint.getId()==first); verify(state.acceptHint(hint.getId(),game.snapshot()));
        verify(guide.getTarget().equals(hint.getId())); verify(!state.acceptHint(hint.getId(),game.snapshot()));
        verify(game.select(guide.getTarget())); guide.collected();
        int second=guide.getTarget(); verify(game.isAvailable(second)); verify(game.select(second)); guide.collected();
        verify(guide.getStep()==TutorialStep.FINISHED && game.getTray().isEmpty() && game.getClearedTiles().size()==2);
        verify(state.getWallet().getBalance()==3 && state.getHintCredits()==3);
        var persisted = new ProgressController(ProgressCodec.INSTANCE.decode(ProgressCodec.INSTANCE.encode(state.capture())));
        verify(persisted.getTutorial().getStep()==TutorialStep.FINISHED);
        var all = new ArrayList<Integer>(); all.add(first); all.add(second);
        for(int id:state.getSession().getLevel().getWinningSelections()) {
            if(id==first || id==second) continue;
            verify(persisted.getSession().getGame().isAvailable(id));
            verify(persisted.getSession().getGame().select(id)); all.add(id);
        }
        verify(persisted.getSession().getGame().getStatus()==Status.WON);
        verify(persisted.getSession().getLevel().getId()==1L);
        verify(persisted.getWallet().getBalance()==3 && persisted.getHintCredits()==3);
        System.out.println("Production controller replay PASS");
        System.out.println("Hint-selected pair: "+first+", "+second+"; legal at each selection");
        System.out.println("Complete winning sequence: "+all);
        System.out.println("Restored controller status: WON; level remains 1; Undo=3; Hint=3");
    }
}
