package nl.tue.s2id90.group15.testPlayer;

import java.util.Collections;
import java.util.List;
import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import org10x10.dam.game.Move;

/**
 * A simple draughts player that plays random moves
 * and values all moves with value 0.
 * @author huub
 */
public class testPlayer extends DraughtsPlayer {

    public testPlayer() {
        super(testPlayer.class.getResource("smiley.png"));
    }
    
    @Override
    /** @return a random move **/
    public Move getMove(DraughtsState s) {
        List<Move> moves = s.getMoves();
        Collections.shuffle(moves);
        return moves.get(0);
    }

    @Override
    public Integer getValue() {
        return 0;
    }
}