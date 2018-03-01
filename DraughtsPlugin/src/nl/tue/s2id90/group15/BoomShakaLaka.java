package nl.tue.s2id90.group15;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import org10x10.dam.game.Move;

/**
 * Group 15 - Petar Galic & Filip Davidovic
 */
public class BoomShakaLaka extends DraughtsPlayer {
    private int bestValue = 0;
    int maxSearchDepth;
    int currentSearchDepth; // used by iterative deepening 
    boolean isWhite;
    
    /** boolean that indicates that the GUI asked the player to stop thinking. */
    private boolean stopped;

    public BoomShakaLaka(int maxSearchDepth) {
        super("thumbnail.jpg");
        this.maxSearchDepth = maxSearchDepth;
    }
    
    @Override public Move getMove(DraughtsState s) {
        Move bestMove = null;
        isWhite = s.isWhiteToMove();
        bestValue = 0;
        currentSearchDepth = 1;
        DraughtsNode node = new DraughtsNode(s); // the root of the search tree, current state
        try {
            while(currentSearchDepth <= maxSearchDepth) { // iterative deepening
                // compute bestMove and bestValue in a call to alphaBeta
                bestValue = alphaBeta(node, MIN_VALUE, MAX_VALUE, currentSearchDepth);

                // store the bestMove found uptill now
                // NB this is not done in case of an AIStoppedException in alphaBeta()
                bestMove  = node.getBestMove();

                // print the results for debugging reasons
                System.err.format(
                    "%s: depth= %2d, best move = %5s, value=%d\n", 
                    this.getClass().getSimpleName(), currentSearchDepth, bestMove, bestValue
                );
                currentSearchDepth++;
            }
        } catch (AIStoppedException ex) {  /* nothing to do */  }
        
        if (bestMove == null) {
            System.err.println("no valid move found!");
            return getRandomValidMove(s);
        } else {
            return bestMove;
        }
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
            int betaN = alphaBetaMax(new DraughtsNode(state), alpha, beta, depth - 1);
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
            int alphaN = alphaBetaMin(new DraughtsNode(state), alpha, beta, depth - 1);
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
    
    // Method that checks whether the key is contained in the array (only works for int arrays and keys)
    private boolean arrayContains(int[] array, int key) {
        for(int i = 0; i < array.length; i++) {
            if(array[i] == key) {
                return true;
            }
        }
        
        return false;
    }
    
    // A method that checks whether the given square is protected
    private boolean isSquareProtected(int[] pieces, int square) {
        final int[] lower = new int[] {-5, 6, -4, 5};
        final int[] higher = new int[] {-6, 5, -5, 4};
        final int[] edgeSquares = new int[] {1, 2, 3, 4, 5, 6, 15, 16, 25, 26, 35, 36, 45, 46, 47, 48, 49, 50};
        final int myPiece = isWhite ? DraughtsState.WHITEPIECE : DraughtsState.BLACKPIECE;
        final int myKing = isWhite ? DraughtsState.WHITEKING : DraughtsState.BLACKKING;
        
        // initiate variables used by the while loop later to determine in which order to increment to go by the diagonal
        boolean isLower = false;
        int[] tar = higher;
        if(square % 10 <= 5 && square % 10 >= 1) {
            isLower = true;
            tar = lower;
        }
        
        // check whether the square is protected with a piece from a square directly adjacent to it
        if(pieces[square + tar[0]] == DraughtsState.EMPTY && pieces[square + tar[1]] == myPiece) {
            return true;
        }
        if(pieces[square + tar[1]] == DraughtsState.EMPTY && pieces[square + tar[0]] == myPiece) {
            return true;
        }
        if(pieces[square + tar[2]] == DraughtsState.EMPTY && pieces[square + tar[3]] == myPiece) {
            return true;
        }
        if(pieces[square + tar[3]] == DraughtsState.EMPTY && pieces[square + tar[2]] == myPiece) {
            return true;
        }
        // for a square to be protected there needs to be a king on one side of its diagonal, and an empty square on the other 
        // initiate the variables used to check for this
        boolean hasKingMinus = false, hasEmptyMinus = false, hasKingPlus = false, hasEmptyPlus = false;
        for(int i = 0; i < 4; i++) {
            // if i % 2 == 0 cross check has been done and the variables are initialize again for the other cross check
            if(i % 2 == 0) {
                hasKingMinus = false;
                hasEmptyMinus = false;
                hasKingPlus = false;
                hasEmptyPlus = false;
            }
            // initiate n so that we don't change square
            int n = square;
            // set flip flop to the previously defined diagonal order
            boolean flipFlop = isLower;
            while(!arrayContains(edgeSquares, n)) { // run the loop until an edge of a board is reached
                if(pieces[n] == myKing) { // check whether the square on the diagonal contains our king
                    // set the appropriate variable to true
                    if(i % 2 == 0) {
                        hasKingMinus = true;
                    } else {
                        hasKingPlus = true;
                    }
                    // break because the potential king on the other side cannot jump over this one
                    break;
                } else if(pieces[n] == DraughtsState.EMPTY) { // check whether the square on the diagonal contains an empty square
                    // set the appropriate variable to true
                    if(i % 2 == 0) {
                        hasEmptyMinus = true;
                    } else {
                        hasEmptyPlus = true;
                    }
                } else {
                    break;
                }
                // go on the diagonal in the appropriate direction
                if(flipFlop) {
                    n += lower[i];
                    flipFlop = false;
                } else {
                    n += higher[i];
                    flipFlop = true;
                }
            }
            // at the end of the traversal on one diagonal, check whether there is a king on on side and an empty space on the other (if yes the square is protected)
            if(i % 2 == 1 && ((hasKingMinus && hasEmptyPlus) || (hasKingPlus && hasEmptyMinus))) {
                return true;
            }
        }
        // no matches found, return false
        return false;
    }
    
    // Method that returns the two squares either up or down (depending on direction) from the current one
    int[] nextSquare(int current, String direction)  {
        final int[] lower = new int[] {-5, -4, 5, 6};
        final int[] higher = new int[] {-6, -5, 4, 5};
        int[] ret = new int[2];
        boolean isLower = false;
        if(current % 10 <= 5 && current % 10 >= 1) {
            isLower = true;
        }
        
        switch(direction) {
            case "up":
                if(isLower) {
                    ret[0] = current + lower[0];
                    ret[1] = current + lower[1];
                } else {
                    ret[0] = current + higher[0];
                    ret[1] = current + higher[1];
                }
                break;
            case "down":
                if(isLower) {
                    ret[0] = current + lower[2];
                    ret[1] = current + lower[3];
                } else {
                    ret[0] = current + higher[2];
                    ret[1] = current + higher[3];
                }
                break;
        }
        
        return ret;
    }

    /** A method that evaluates the given state. */
    int evaluate(DraughtsState state) { 
        int[] pieces = state.getPieces();
        int eval = 0;
        ArrayList<Integer> blackKings = new ArrayList<>();
        ArrayList<Integer> whiteKings = new ArrayList<>();
        
        // material difference with weights
        int whiteCount = 0;
        int blackCount = 0;
        final int kingWeight = 2; // ToDo: check for the correctness of the weigths by testing
        final int normalWeight = 1;
        final int[] squareWeights = new int[] {5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 3, 3, 3, 5, 5, 3, 2, 2, 4, 4, 2, 1, 3, 5, 5, 3, 1, 2, 4, 4, 2, 2, 3, 5, 5, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5};
        for(int i = 1; i < pieces.length; i++) {
            switch (pieces[i]) {
                case DraughtsState.BLACKKING:
                    blackCount += squareWeights[i - 1] * kingWeight;
                    blackKings.add(i);
                    break;
                case DraughtsState.BLACKPIECE:
                    blackCount += squareWeights[i - 1] * normalWeight;
                    break;
                case DraughtsState.WHITEKING:
                    whiteCount += squareWeights[i - 1] * kingWeight;
                    whiteKings.add(i);
                    break;
                case DraughtsState.WHITEPIECE:
                    whiteCount += squareWeights[i - 1] * normalWeight;
                    break;
                default:
                    break;
            }
        }
        
        //number of protected pieces heuristics
        int[] protectedPieces = new int[] {1, 2, 3, 4, 5, 6, 15, 16, 25, 26, 35, 36, 45, 46, 47, 48, 49, 50};
        int protectedNumber = 0;
        for (int i =0;i<protectedPieces.length;i++) {
            switch(pieces[protectedPieces[i]]) {
            case DraughtsState.BLACKKING:
                    if (!isWhite) {
                        protectedNumber += 1;
                    }
                    break;
                case DraughtsState.BLACKPIECE:
                    if (!isWhite) {
                        protectedNumber += 1;
                    }
                    break;
                case DraughtsState.WHITEKING:
                    if (isWhite) {
                        protectedNumber += 1;
                    }
                    break;
                case DraughtsState.WHITEPIECE:
                    if (isWhite) {
                        protectedNumber += 1;
                    }
                    break;
                default:
                    break;
            }
        }
        
        // number of squares protected in the middle
        int protectedMiddleSquares = 0;
        for(int i = 16; i <= 35; i++) {
            if(isSquareProtected(pieces, i)) {
                protectedMiddleSquares++;
            }
        }
        
        // runaway pieces (free path to becoming a king)
        int runawayPieces = 0;
        int[] endSquares;
        int myPiece = isWhite ? DraughtsState.WHITEPIECE : DraughtsState.BLACKPIECE;
        if(isWhite) {
            endSquares = new int[] {1, 2, 3, 4, 5};
        } else {
            endSquares = new int[] {46, 47, 48, 49, 50};
        }
        for(int i = 1; i < pieces.length; i++) {
            if(pieces[i] == myPiece) {
                int n = i;
                boolean reachedEnd = false;
                while(!reachedEnd) {
                    int[] newSquares;
                    if(isWhite) {
                        newSquares = nextSquare(n, "up");
                        n -= 10;
                    } else {
                        newSquares = nextSquare(n, "down");
                        n += 10;
                    }
                    // check if any of the squares contains a piece (if yes the inspected piece is not runaway, so brek the loop)
                    if(n >= 1 && n <= 50 && pieces[n] != DraughtsState.EMPTY) {
                        break;
                    }
                    if(newSquares[0] >= 1 && newSquares[0] <= 50 && pieces[newSquares[0]] != DraughtsState.EMPTY) {
                        break;
                    }
                    if(newSquares[1] >= 1 && newSquares[1] <= 50 && pieces[newSquares[1]] != DraughtsState.EMPTY) {
                        break;
                    }
                    
                    // check for the termination of the while loop (reached the end of the board)
                    if(!(n >= 1 && n <= 50) || arrayContains(endSquares, n)) {
                        reachedEnd = true;
                    }
                }
                if(reachedEnd) {
                    runawayPieces++;
                }
            }
        }
        
        //trapped kings
        List<Move> availableMoves = state.getMoves();
        for(Move move: availableMoves) {
            if (move.isKingMove()) {
                if (isWhite) {
                    if (whiteKings.contains(move.getBeginPiece())) {
                        whiteKings.remove(move.getBeginPiece());
                    }
                }
                else {
                    if(blackKings.contains(move.getBeginPiece())) {
                        blackKings.remove(move.getBeginPiece());
                    }
                }
            }
        }
        
        
        
        // calculate the final result and return 
        if(isWhite) {
            eval += (whiteCount - blackCount)-whiteKings.size();
        } else {
            eval += (blackCount - whiteCount)-blackKings.size();
        }
        eval += protectedNumber + protectedMiddleSquares + runawayPieces;
        
        return eval; 
    }
}
