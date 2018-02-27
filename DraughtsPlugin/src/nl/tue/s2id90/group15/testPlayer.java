package nl.tue.s2id90.group15;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import java.util.Collections;
import java.util.List;
import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import org10x10.dam.game.Move;

/**
 * Group 15 - Petar Galic & Filip Davidovic
 */
public class testPlayer extends DraughtsPlayer {
    private int bestValue = 0;
    int maxSearchDepth;
    int currentSearchDepth; // used by iterative deepening 
    boolean isWhite;
    
    /** boolean that indicates that the GUI asked the player to stop thinking. */
    private boolean stopped;

    public testPlayer(int maxSearchDepth) {
        super("thumbnail.jpg");
        this.maxSearchDepth = maxSearchDepth;
    }
    
    @Override public Move getMove(DraughtsState s) {
        return getRandomValidMove(s);
//        Move bestMove = null;
//        isWhite = s.isWhiteToMove();
//        bestValue = 0;
//        currentSearchDepth = 1;
//        DraughtsNode node = new DraughtsNode(s); // the root of the search tree, current state
//        try {
//            while(currentSearchDepth <= maxSearchDepth) { // iterative deepening
//                // compute bestMove and bestValue in a call to alphaBeta
//                bestValue = alphaBeta(node, MIN_VALUE, MAX_VALUE, currentSearchDepth);
//
//                // store the bestMove found uptill now
//                // NB this is not done in case of an AIStoppedException in alphaBeta()
//                bestMove  = node.getBestMove();
//
//                // print the results for debugging reasons
//                System.err.format(
//                    "%s: depth= %2d, best move = %5s, value=%d\n", 
//                    this.getClass().getSimpleName(), currentSearchDepth, bestMove, bestValue
//                );
//                currentSearchDepth++;
//            }
//        } catch (AIStoppedException ex) {  /* nothing to do */  }
//        
//        if (bestMove == null) {
//            System.err.println("no valid move found!");
//            return getRandomValidMove(s);
//        } else {
//            return bestMove;
//        }
    } 

    /** This method's return value is displayed in the AICompetition GUI.
     * 
     * @return the value for the draughts state s as it is computed in a call to getMove(s). 
     */
    @Override public Integer getValue() { 
       return bestValue;
    }

    /** Tries to make alphaBeta search stop. Search should be implemented such that it
     * throws an AIStoppedException when boolean stopped is set to true;
    **/
    @Override public void stop() {
       stopped = true; 
    }
    
    /** returns random valid move in state s, or null if no moves exist. */
    Move getRandomValidMove(DraughtsState s) {
        List<Move> moves = s.getMoves();
        Collections.shuffle(moves);
        return moves.isEmpty()? null : moves.get(0);
    }
    
    /** Implementation of alphaBeta that automatically chooses the white player
     *  as maximizing player and the black player as minimizing player.
     * @param rootNode contains DraughtsState and has field to which the best move can be assigned.
     * @param alpha
     * @param beta
     * @param depth maximum recursion Depth
     * @return the computed value of this rootNode
     * @throws AIStoppedException
     **/
    int alphaBeta(DraughtsNode rootNode, int alpha, int beta, int depth) throws AIStoppedException {
        return alphaBetaMax(rootNode, alpha, beta, depth);
    }
    
    /** Does an alphaBeta computation with the given alpha and beta
     * where the player that is to move in node is the minimizing player.
     * 
     * <p>Typical pieces of code used in this method are:
     *     <ul> <li><code>DraughtsState state = node.getState()</code>.</li>
     *          <li><code> state.doMove(move); .... ; state.undoMove(move);</code></li>
     *          <li><code>node.setBestMove(bestMove);</code></li>
     *          <li><code>if(stopped) { stopped=false; throw new AIStoppedException(); }</code></li>
     *     </ul>
     * </p>
     * @param node contains DraughtsState and has field to which the best move can be assigned.
     * @param alpha
     * @param beta
     * @param depth  maximum recursion Depth
     * @return the compute value of this node
     * @throws AIStoppedException thrown whenever the boolean stopped has been set to true.
     */
     int alphaBetaMin(DraughtsNode node, int alpha, int beta, int depth) throws AIStoppedException {
        if (stopped) { stopped = false; throw new AIStoppedException(); } // check for the termination request by the GUI
        if(depth == 0) { // check if the max search depth was reached. if it was, return the evaluation of the current state
            return evaluate(node.getState());
        }
        DraughtsState state = node.getState();
        List<Move> possibleMoves = state.getMoves(); // all possible moves from the given state
        for(Move possibleMove : possibleMoves) {            
            state.doMove(possibleMove); // advance from the current state with the selected move
            int betaN = alphaBetaMax(new DraughtsNode(state), alpha, beta, depth-1);
            if(betaN < beta) {
                beta = betaN;
            }
            state.undoMove(possibleMove); // unadvance from the derrived state with the selected move to get back to the current state
            if(beta <= alpha) { // return beta and terminate since this node is not going to be reached
                return alpha;
            }
        }
        return beta; 
     }
    
    int alphaBetaMax(DraughtsNode node, int alpha, int beta, int depth) throws AIStoppedException {
        if (stopped) { stopped = false; throw new AIStoppedException(); } // check for the termination request by the GUI
        if(depth == 0) { // check if the max search depth was reached. if it was, return the evaluation of the current state
            return evaluate(node.getState());
        }
        DraughtsState state = node.getState();
        List<Move> possibleMoves = state.getMoves(); // all possible moves from the given state
        Move bestMove = null;
        for(Move possibleMove : possibleMoves) {
            state.doMove(possibleMove); // advance from the current state with the selected move
            int alphaN = alphaBetaMin(new DraughtsNode(state), alpha, beta, depth-1);
            if(alphaN > alpha) {
                alpha = alphaN;
                bestMove = possibleMove;
            }
            state.undoMove(possibleMove); // unadvance from the derrived state with the selected move to get back to the current state
            if(alpha >= beta) { // return beta and terminate since this node is not going to be reached
                return beta;
            }
        }
        node.setBestMove(bestMove);
        return alpha; 
    }

    /** A method that evaluates the given state. */
    int evaluate(DraughtsState state) { 
        int[] pieces = state.getPieces();
        int eval = 0;
        // material difference
        int whiteCount = 0;
        int blackCount = 0;
        final int kingWeight = 2; // ToDo: check for the correctness of the weigths by testing
        final int normalWeight = 1;
        for(int i = 0; i < pieces.length; i++) {
            switch (pieces[i]) {
                case DraughtsState.BLACKKING:
                    blackCount += kingWeight; 
                    break;
                case DraughtsState.BLACKPIECE:
                    blackCount += normalWeight;
                    break;
                case DraughtsState.WHITEKING:
                    whiteCount += kingWeight;
                    break;
                case DraughtsState.WHITEPIECE:
                    whiteCount += normalWeight;
                    break;
                default:
                    break;
            }
        }
        if(isWhite) {
            eval += (whiteCount - blackCount);
        } else {
            eval += (blackCount - whiteCount);
        }
        
        //ToDo: implement other evaluation techniques
        return eval; 
    }
}
