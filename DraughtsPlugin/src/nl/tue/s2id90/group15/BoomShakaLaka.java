package nl.tue.s2id90.group15;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import org10x10.dam.game.Move;

/**
 * Group 15 - Petar Galic & Filip Davidovic
 */
// "struct" stored in the hash table
// stores the depth at which the node was when the field got hashed, the heuristic evaluation of the node & the best evaluated move in the node
class HashFieldValue {
    public static final int HASH_EXACT = 0; // flag that explains that the evaluation in this hash field is definite evaluation (no alpha/beta cut-offs)
    public static final int HASH_BETA = 1; // flag that explains that there was a beta cut-off (evaluation is at least beta or higher)
    public static final int HASH_ALPHA = 2; // flag that explains that there was an alpha cut-off (evaluation was not as high as alpha)
    public int depth; // the greater the number the closer to the root
    public int evaluation;
    public Move bestMove;
    public int flag;
    
    HashFieldValue(int depth, int evaluation, Move bestMove, int flag) {
        this.depth = depth;
        this.evaluation = evaluation;
        this.bestMove = bestMove;
        this.flag = flag;
    }
}

public class BoomShakaLaka extends DraughtsPlayer {
    private int bestValue = 0;
    int maxSearchDepth;
    int currentSearchDepth; // used by iterative deepening 
    boolean isWhite;
    private final Random random = new Random();
    private final long[][] zobristTable;
    Hashtable<Long, HashFieldValue> transpositionTable = new Hashtable<>();
    
    /** boolean that indicates that the GUI asked the player to stop thinking. */
    private boolean stopped;

    public BoomShakaLaka(int maxSearchDepth) {
        super("thumbnail.jpg");
        this.maxSearchDepth = maxSearchDepth;
        this.zobristTable = new long[50][4];
        for(int i = 0; i < 50; i++) {
            for(int j = 0; j < 4; j++) {
                this.zobristTable[i][j] = random.nextLong();
            }
        }
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
    
    // function that calculates the Zobrist hash value of a board state
    // it does so in a two step process:
    // 1. generate a 50x4 array of pseudorandom long numbers (64-bits), where 50 represents the number of squares on the board and 4 represents the four possible pieces (white/black kings and normal pieces). this is done in the constructor of this class
    // 2. for each piece on the board XOR its positions random long number agains the current Zobrist hash value (h).
    // for example, a white king's random number on the second square is stored in zorbistTable[1][0]
    // indexes are the following:
    // WHITEKING = 0, WHITEPIECE = 1, BLACKKING = 2, BLACKPIECE = 3
    private long getZobristHash(int[] board) {
        long h = 0L;
        for(int i = 1; i < board.length; i++) {
            if(board[i] != DraughtsState.EMPTY) {
                int index = 0;
                switch(board[i]) {
                    case DraughtsState.WHITEKING:
                        index = 0;
                        break;
                    case DraughtsState.WHITEPIECE:
                        index = 1;
                        break;
                    case DraughtsState.BLACKKING:
                        index = 2;
                        break;
                    case DraughtsState.BLACKPIECE:
                        index = 3;
                        break;
                    default:
                        break;
                }
                h = h ^ zobristTable[i - 1][index];
            }
        }
        return h;
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
    
    // method that orders possible moves by the following criteria:
    // 1. evaluation of each of the states after making a move
    // since this is an ordering for alphaBetaMin this method orders them in ascending order
    List<Move> orderMovesMin(DraughtsState state, List<Move> possibleMoves) {
        possibleMoves.sort(new Comparator<Move>() {
            @Override
            public int compare(Move m1, Move m2) {
                state.doMove(m1);
                int m1Eval = evaluate(state);
                state.undoMove(m1);
                state.doMove(m2);
                int m2Eval = evaluate(state);
                state.undoMove(m2);
                
                if(m1Eval == m2Eval) {
                    return 0; // two states have an equal evaluation
                } else if(m1Eval < m2Eval) {
                    return 1; // first state has a lower evaluation than the second state
                } else {
                    return -1; // second state has a lower evaluation than the first state
                }
            }
        });
        return possibleMoves;
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
        // check whether the transposition table contains an entry for this state
        long zobristHash = getZobristHash(state.getPieces()); // get the zobrist hash of the state
        HashFieldValue transpositionTableValue = transpositionTable.get(zobristHash);
        if(transpositionTableValue != null) {
            if(transpositionTableValue.depth >= depth) { // there is an entry in the hash table for this state, check whether that value has a higher depth than the one at which this node is
                switch(transpositionTableValue.flag) { // return the appropriate value and set the best move based on the flag
                    case HashFieldValue.HASH_EXACT:
                        node.setBestMove(transpositionTableValue.bestMove);
                    case HashFieldValue.HASH_BETA:
                        node.setBestMove(transpositionTableValue.bestMove);
                }
                return transpositionTableValue.evaluation;
            } // else first search the best move from this hash field 
        }
        
        List<Move> possibleMoves = orderMovesMin(state, state.getMoves()); // all possible moves from the given state ordered in ascending order
        Move bestMove = null;
        for(Move possibleMove : possibleMoves) {            
            state.doMove(possibleMove); // advance from the current state with the selected move
            int betaN = alphaBetaMax(new DraughtsNode(state), alpha, beta, depth - 1);
            if(betaN < beta) {
                beta = betaN;
                bestMove = possibleMove;
            }
            state.undoMove(possibleMove); // unadvance from the derrived state with the selected move to get back to the current state
            if(beta <= alpha) { // return beta and terminate since this node is not going to be reached
                transpositionTable.put(zobristHash, new HashFieldValue(depth, alpha, null, HashFieldValue.HASH_ALPHA));
                return alpha;
            }
        }
        node.setBestMove(bestMove);
        transpositionTable.put(zobristHash, new HashFieldValue(depth, beta, bestMove, HashFieldValue.HASH_EXACT));
        return beta; 
     }
     
     // method that orders possible moves by the following criteria:
    // 1. evaluation of each of the states after making a move
    // since this is an ordering for alphaBetaMax this method orders them in descending order
    List<Move> orderMovesMax(DraughtsState state, List<Move> possibleMoves) {
        possibleMoves.sort(new Comparator<Move>() {
            @Override
            public int compare(Move m1, Move m2) {
                state.doMove(m1);
                int m1Eval = evaluate(state);
                state.undoMove(m1);
                state.doMove(m2);
                int m2Eval = evaluate(state);
                state.undoMove(m2);
                
                if(m1Eval == m2Eval) {
                    return 0; // two states have an equal evaluation
                } else if(m1Eval < m2Eval) {
                    return -1; // first state has a lower evaluation than the second state
                } else {
                    return 1; // second state has a lower evaluation than the first state
                }
            }
        });
        return possibleMoves;
    }
    
    int alphaBetaMax(DraughtsNode node, int alpha, int beta, int depth) throws AIStoppedException {
        if (stopped) { stopped = false; throw new AIStoppedException(); } // check for the termination request by the GUI
        if(depth == 0) { // check if the max search depth was reached. if it was, return the evaluation of the current state
            return evaluate(node.getState());
        }
        DraughtsState state = node.getState();
        // check whether the transposition table contains an entry for this state
        long zobristHash = getZobristHash(state.getPieces()); // get the zobrist hash of the state
        HashFieldValue transpositionTableValue = transpositionTable.get(zobristHash);
        if(transpositionTableValue != null) {
            if(transpositionTableValue.depth >= depth) { // there is an entry in the hash table for this state, check whether that value has a higher depth than the one at which this node is
                switch(transpositionTableValue.flag) { // alpha cut-offs don't store the best move because "there is none". if the stored evaluation was an alpha cut-off we need to search the node again.
                    case HashFieldValue.HASH_EXACT:
                        node.setBestMove(transpositionTableValue.bestMove);
                        return transpositionTableValue.evaluation; // return the stored evaluation
                    case HashFieldValue.HASH_BETA:
                        node.setBestMove(transpositionTableValue.bestMove);
                        return transpositionTableValue.evaluation; // return the stored evaluation
                }
            } // else first search the best move from this hash field 
        }
        
        List<Move> possibleMoves = orderMovesMax(state, state.getMoves()); // all possible moves from the given state ordered in descending order
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
                transpositionTable.put(zobristHash, new HashFieldValue(depth, beta, bestMove, HashFieldValue.HASH_BETA));
                return beta;
            }
        }
        node.setBestMove(bestMove);
        transpositionTable.put(zobristHash, new HashFieldValue(depth, alpha, bestMove, HashFieldValue.HASH_EXACT));
        return alpha; 
    }
    
    // Method that checks whether the key is contained in the array (only works for int arrays and keys)
    private boolean arrayContains(int[] array, int key) {
        for(int i = 0; i < array.length; i++) { // cycle through the whole array
            if(array[i] == key) { // if the entry in the array equals the key
                return true; // the array contains the key and the function returns true
            }
        }
        
        return false; // return false if a match is not found
    }
    
    // A method that checks whether the given square is protected
    // square is protected if:
    // - there is a piece on one side of its diagonal and an empty square on the other side of the diagoanl (this stands for either of the two diagonals)
    // - the first piece on one side of the diagonal is a king, and there exists an empty square on the other side of its diagonal before any other piece (this stans for either of the two diagonals)
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
        final int[] lower = new int[] {-5, -4, 5, 6}; // left up, right up, left down, right down (1 <= current % 10 <= 5)
        final int[] higher = new int[] {-6, -5, 4, 5}; // left up, right up, left down, right down (6 <= current % 10 <= 9 || current % 10 == 0)
        int[] ret = new int[2]; // array that is to be returned after required calculations
        boolean isLower = current % 10 <= 5 && current % 10 >= 1; 
        
        switch(direction) { // depending on the direction parameter calculate the upper or lower two squares
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
        int[] pieces = state.getPieces(); // array that contains the board state
        int eval = 0; // variable used to calculate the heuristic evaluation of the state
        ArrayList<Integer> blackKings = new ArrayList<>();
        ArrayList<Integer> whiteKings = new ArrayList<>();
        int lastBlackPiece = 0;
        int lastWhitePiece = 0;
        boolean firstWhite = false;
        
        // material difference with weights
        int whiteCount = 0; // number of white's pieces
        int blackCount = 0; // number of black's pieces
        final int kingWeight = 2; // weight given to a king
        final int normalWeight = 1; // weight given to a regular piece
        final int[] squareWeights = new int[] {5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 3, 3, 3, 5, 5, 3, 2, 2, 4, 4, 2, 1, 3, 5, 5, 3, 1, 2, 4, 4, 2, 2, 3, 5, 5, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5}; // the weight of each square
        for(int i = 1; i < pieces.length; i++) { // loop that iterates through the whole board
            switch (pieces[i]) { 
                case DraughtsState.BLACKKING: // if the piece is a black king, add the king weight multiplied with the square weight to the black's piece count
                    blackCount += squareWeights[i - 1] * kingWeight;
                    blackKings.add(i); 
                    break;
                case DraughtsState.BLACKPIECE: // if the piece is a black piece, add the normal piece weight multiplied with the square weight to the black's piece count
                    blackCount += squareWeights[i - 1] * normalWeight;
                    break;
                case DraughtsState.WHITEKING: // if the piece is a white king, add the king weight multiplied with the square weight to the white's piece count
                    whiteCount += squareWeights[i - 1] * kingWeight;
                    whiteKings.add(i);
                    break;
                case DraughtsState.WHITEPIECE: // if the piece is a white piece, add the normal piece weight multiplied with the square weight to the white's piece count
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
        int protectedMiddleSquares = 0; // variable used to store the number of protected squares in the middle
        for(int i = 16; i <= 35; i++) { // middle are squares from 16 to 35 (including 16 and 35)
            if(isSquareProtected(pieces, i)) { // check whether the square is protected by calling the isSquareProtected function
                protectedMiddleSquares++; // if the function returns true, the square is protected, so we can increment the number of protected squares
            }
        }
        
        // runaway pieces (free path to becoming a king)
        int runawayPieces = 0; // variable used to store the number of runaway pieces
        int[] endSquares; // array that contains the five squares at the end of the board (uninitialized because the end squares depend on the side the player is playing)
        int myPiece = isWhite ? DraughtsState.WHITEPIECE : DraughtsState.BLACKPIECE; // variable that stores the value of our piece
        if(isWhite) { // if statement that assigns the appropriate values to endSquares (with respect to isWhite)
            endSquares = new int[] {1, 2, 3, 4, 5};
        } else {
            endSquares = new int[] {46, 47, 48, 49, 50};
        }
        for(int i = 1; i < pieces.length; i++) { // loop that cycles through all the squares
            if(pieces[i] == myPiece) { // only inspect our pieces (not kings, but normal pieces)
                int n = i; // variable that stores the number of the square currently inspected in the while loop below
                boolean reachedEnd = false; // boolean that is used to terminate the while loop below (set to true when the end of the board is reached, i.e. when n is at the end of the board)
                while(!reachedEnd) { // while loop that inspects all the squares from the inspected piece to the end of the board (or until another piece is found on the path)
                    int[] newSquares; // array that is used to store the two squares that are above or below n (depending on the side of the player)
                    if(isWhite) { // if statement that popluates the newSquares array and increments n in the appropraite direction
                        newSquares = nextSquare(n, "up"); // get two squares that are above n 
                        n -= 10; // decrement n (move two rows up)
                    } else {
                        newSquares = nextSquare(n, "down"); // get two squares that are below n 
                        n += 10; // increment n (move two rows down)
                    }
                    // check if any of the squares contains a piece (if yes the inspected piece is not runaway, so brek the loop)
                    if(n >= 1 && n <= 50 && pieces[n] != DraughtsState.EMPTY) {
                        break;
                    }
                    // at least one of the two squares has to be empty
                    if((newSquares[0] >= 1 && newSquares[0] <= 50 && pieces[newSquares[0]] != DraughtsState.EMPTY) || (newSquares[1] >= 1 && newSquares[1] <= 50 && pieces[newSquares[1]] != DraughtsState.EMPTY)) {
                        break;
                    }
                    
                    // check for the termination of the while loop (reached the end of the board)
                    if(!(n >= 1 && n <= 50) || arrayContains(endSquares, n)) {
                        reachedEnd = true;
                    }
                }
                // if that checks whether the while loop was terminated because the end of the board was reached
                // if this is the case then we have inspected all the squares from the piece to the end of the board and found no piece on the way (the piece is runaway)
                if(reachedEnd) {
                    runawayPieces++; // increment the number of runaway pieces
                }
            }
        }
        
        //trapped kings
//        List<Move> availableMoves = state.getMoves();
//        for(Move move: availableMoves) {
//            if (move.isKingMove()) {
//                if (isWhite) {
//                    if (whiteKings.contains(move.getBeginPiece())) {
//                        whiteKings.remove(move.getBeginPiece());
//                    }
//                }
//                else {
//                    if(blackKings.contains(move.getBeginPiece())) {
//                        blackKings.remove(move.getBeginPiece());
//                    }
//                }
//            }
//        }
        
        //formation heuristics
        int numberOfPieces = 0;
        if (isWhite) {
            int row = lastWhitePiece/5; // get the furthest row that has white pieces on it
            for (int i = 1; i <5 ;i++) { // check how many white pieces are on that row
                switch(pieces[row*5+i]){
                case DraughtsState.WHITEPIECE:
                    numberOfPieces++;
                    break;
                case DraughtsState.WHITEKING:
                    numberOfPieces++;
                    break;
                }
                        
            }
        } else {
            int row = lastBlackPiece/5; // get the furthest row that has a black piece on it
            for (int i = 1; i <5 ;i++) {//check how many black pieces are on the furthest row
                switch(pieces[row*5+i]){
                case DraughtsState.BLACKPIECE:
                    numberOfPieces++;
                    break;
                case DraughtsState.BLACKKING:
                    numberOfPieces++;
                    break;
                }
                        
            }
            
        }
        
        
        // calculate the final result and return 
        if(isWhite) { // depending on the side the player is playing, calculate material difference and subtract the number of trapped kings
            eval += (whiteCount - blackCount) - whiteKings.size();
        } else {
            eval += (blackCount - whiteCount) - blackKings.size();
        }
        eval += protectedNumber + protectedMiddleSquares + runawayPieces;
        
        return eval; 
    }
}
