package entrants.pacman.matt_barthet;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.*;

import static entrants.pacman.matt_barthet.Agent_Utility.*;

public class MyPacMan_TDL extends PacmanController {

    /**
     * Initialises the variables for the Q-Learning algorithm.
     */
    private static final int COMPUTATIONAL_BUDGET = 40;
    private static final float LEARNING_RATE = 0.2f;
    private static final float DISCOUNT_FACTOR = 0.9f;
    private static final float EPSILON = 0.9f;
    private static final int MAXIMUM_STEPS = 100;
    private static final ArrayList<int[]> stateSpace = initialiseStates();
    private static ArrayList<QEntry> qTable;

    private static Game currentGame;
    private static final MOVE[] moves = new MOVE[]{MOVE.UP, MOVE.LEFT, MOVE.DOWN, MOVE.RIGHT};
    Random randomGenerator;

    public MyPacMan_TDL() {
        ghostEdibleTime = new int[Constants.GHOST.values().length];
        qTable = initialiseTable();
    }

    /**
     * Initialise the set space to cover every possible state in the learning paradigm.
     * State format: {Wall North/West/South/East}-{Direction}-{Threat North/West/South/East}-{Trapped}
     * State Value Ranges: {0,1}-{0,1}-{0,1}-{0,1}-{0,1,2,3,4}-{0,1}-{0,1}-{0,1}-{0,1}-{0,1}{0,1}
     * @return the ArrayList contain the state space.
     */
    private static ArrayList<int[]> initialiseStates(){
        ArrayList<int[]> stateSpace = new ArrayList<>();
        for(int i = 0; i < 4; i++){
            for(int j = 0; j < 5; j++){
                for(int k = 0; k < 2; k++){
                    for(int l = 0; l < 2; l++){
                        for(int m = 0; m < 2; m++){
                            for(int n = 0; n < 2; n++){
                                for(int o = 0; o < 2; o++){
                                    for(int p = 0; p < 2; p++){
                                        for(int q = 0; q < 2; q++){
                                            for(int r = 0; r < 2; r++){
                                                stateSpace.add(new int[]{i, j, k, l, m, n, o, p, q, r});
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return stateSpace;
    }

    /**
     * Initialise the Q-Table, which contains every possible state-action pair combination from the
     * previously established state space and action space.  The table prunes out any actions which
     * cause Ms. Pacman to make a move into a barrier.
     * @return the generated Q-Table as an ArrayList of QEntries.
     */
    private ArrayList<QEntry> initialiseTable(){
        ArrayList<QEntry> qTable = new ArrayList<>();
        int index = 0;
        for (int[] state : stateSpace) {
            for (MOVE move: moves) {
                if(state[0] == 1 && move == MOVE.UP || state[2] == 1 && move == MOVE.DOWN)
                    continue;
                if(state[1] == 1 && move == MOVE.LEFT || state[3] == 1 && move == MOVE.RIGHT)
                    continue;
                qTable.add(new QEntry(state, move, index));
                index++;
            }
        }
        return qTable;
    }

    /**
     * Function which determines the best move for Ms. Pacman to take through a Q-Learning
     * algorithm. Also updates Ms. Pacman's knowledge of the game state and ghost locations.
     * @param game: the current state of the game at this tick.
     * @param timeDue: how long Ms. Pacman has to decide about her move.
     * @return the chosen move based on the results of the algorithm.
     */
    public MOVE getMove(Game game, long timeDue) {
        randomGenerator = new Random();
        updateObservations(game);
        observeGhosts(game);
        if(game != currentGame){ currentGame = game; }
        MOVE bestMove = reinforcementLearning();
        predictions.update();
        return bestMove;
    }

    /**
     * While within the set time limit, execute episodes of the algorithm using the given
     * starting state to converge on the best possible move.
     * @return the optimal move in current game state.
     */
    private MOVE reinforcementLearning(){
        long startTime = new Date().getTime();
        QEntry startState = getState(currentGame, currentGame.getPacmanCurrentNodeIndex());
        int counter =0;
        while(new Date().getTime() < startTime + COMPUTATIONAL_BUDGET){
            Game simulation = getGameSimulation(currentGame, predictions, ghostEdibleTime);
            learningEpisode(simulation, startState);
            counter++;
        }
        System.out.println(counter);
        assert startState != null;
        ArrayList<QEntry> possibleMoves = getSimilarEntries(startState);
        return getBestEntry(possibleMoves).action;
    }

    private void learningEpisode(Game simulation, QEntry startingState){
        MOVE nextMove;
        QEntry currentState = startingState;
        for(int stepCount = 0; stepCount < MAXIMUM_STEPS && !simulation.wasPacManEaten(); stepCount++){
            nextMove = chooseAction(currentState);
            MOVE lastMove = simulation.getPacmanLastMoveMade();
            simulation.advanceGame(nextMove, getBasicGhostMoves(simulation));
            QEntry previousState = currentState;
            currentState = getState(simulation, simulation.getPacmanCurrentNodeIndex());
            assert currentState != null;
            previousState.updateValue(rewardFunction(simulation, previousState, lastMove), getBestEntry(getSimilarEntries(currentState)));
        }
    }

    /**
     * Function to get the first qEntry corresponding to the current game state.
     * @param game: current game being played.
     * @param location: Ms. Pacman's location in the maze
     * @return the first entry in the QTable containing this state.
     */
    private QEntry getState(Game game, int location){

        int[] currentState = new int[10];

        currentState[0] = moveToInteger(game.getPacmanLastMoveMade());

        int[] pills = game.getActivePillsIndices();
        if(pills.length != 0) {
            int nearestPill = game.getClosestNodeIndexFromNodeIndex(location, pills, Constants.DM.PATH);
            MOVE pillMove = game.getNextMoveTowardsTarget(location, nearestPill, Constants.DM.PATH);
            currentState[1] = moveToInteger(pillMove);
        } else {
            currentState[1] = 4;
        }

        for(int i = 2; i < 6; i++){
            if(game.getNeighbour(location, moves[i-2]) == -1){
                currentState[i] = 1;
            } else {
                currentState[i] = 0;
            }
        }

        for(Constants.GHOST ghost: Constants.GHOST.values()){
            int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
            if(ghostLocation != -1) {
                MOVE direction = game.getNextMoveTowardsTarget(location, ghostLocation, Constants.DM.PATH);
                if (direction == MOVE.UP) {
                    currentState[6] = 1;
                } else if (direction == MOVE.LEFT) {
                    currentState[7] = 1;
                } else if (direction == MOVE.DOWN) {
                    currentState[8] = 1;
                } else if (direction == MOVE.RIGHT){
                    currentState[9] = 1;
                }
            }
        }

        for (QEntry qEntry : qTable) {
            if (Arrays.equals(currentState, qEntry.state)) {
                return qEntry;
            }
        }

        return null;
    }

    /**
     * Choose the next action for Ms. Pacman to test using the epsilon-greedy policy.
     * @param firstEntry: entry containing the state we are looking at.
     * @return action to take in the given state for next step.
     */
    private MOVE chooseAction(QEntry firstEntry){
        ArrayList<QEntry> possibleMoves = getSimilarEntries(firstEntry);
        if(randomGenerator.nextFloat() < EPSILON){
            QEntry bestEntry = getBestEntry(possibleMoves);
            if(bestEntry.qValue == 0 && bestEntry.action == MOVE.UP) {
                //System.out.println("No entries have been evaluated at this state. Choosing random.");
                return possibleMoves.get(randomGenerator.nextInt(possibleMoves.size())).action;
            }
            return bestEntry.action;
        } else {
            return possibleMoves.get(randomGenerator.nextInt(possibleMoves.size())).action;
        }
    }

    /**
     * Returns the reward to be attributed to a state-action execution according to the
     * resulting event.
     * @param game: game state being observed.
     * @return float score value.
     */
    private float rewardFunction(Game game, QEntry state, MOVE lastMove){
        float score = 0;

        if(game.wasPacManEaten()) {
            //System.out.println("Ms. Pacman has been eaten!");
            return -100;
        }
        if(moveToInteger(state.action) == state.state[2]){
            //System.out.println("Ms. Pacman is moving toward the nearest pill!");
            score += 5;
        }
        if(game.wasPillEaten()) {
            //System.out.println("Ms. Pacman has eaten a pill!");
            score += 10;
        } else if(game.wasPowerPillEaten()) {
            //System.out.println("Ms. Pacman has eaten a power pill!");
            score += 50;
        }
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            if (game.wasGhostEaten(ghost)) {
                //System.out.println("Ms. Pacman has eaten a ghost!");
                score += 10;
            }
        }

        if(state.state[moveToInteger(lastMove)+2] == 1) {
            //System.out.println("Ms. Pacman has attempted to move into a barrier!");
            score = -50;
        }
        return score;
    }

    /**
     * Get entries in the qTable with the same state as the given entry.
     * @param entry: entry with the state we'd like to compare.
     * @return all entries with the same state.
     */
    private ArrayList<QEntry> getSimilarEntries(QEntry entry){
        ArrayList<QEntry> possibleMoves = new ArrayList<>();
        for(int i = entry.qIndex;; i++){
            if(Arrays.equals(entry.state, qTable.get(i).state)) {
                possibleMoves.add(qTable.get(i));
            } else {
                break;
            }
        }
        return possibleMoves;
    }

    /**
     * Get the entry with the best Q-Value from the list of entries given.
     * @param entries: list of entries to be analysed.
     * @return entry with the best Q-Value.
     */
    private QEntry getBestEntry(ArrayList<QEntry> entries){
        float max = Integer.MIN_VALUE;
        QEntry maxEntry = null;
        for (QEntry entry: entries){
            if(entry.qValue > max) {
                maxEntry = entry;
                max = entry.qValue;
            }
        }
        assert maxEntry != null;
        return maxEntry;
    }

    /**
     * Converts a MOVE variable to integer based on the state representation used.
     * In this case {N/W/S/E} = {0/1/2/3}
     * @param move: move that should be converted to an integer.
     * @return integer value corresponding to that move.
     */
    private int moveToInteger(MOVE move){
        if(move == MOVE.UP)
            return 0;
        if(move == MOVE.LEFT)
            return 1;
        if(move == MOVE.DOWN)
            return 2;
        if(move == MOVE.RIGHT)
            return 3;
        return 0;
    }

    private String getStateMeaning(int[] state){

        String output = "Direction: ";
        switch(state[0]){
            case 0: output += "UP\t"; break;
            case 1: output += "LEFT\t"; break;
            case 2: output += "DOWN\t"; break;
            case 3: output += "RIGHT\t"; break;
        }

        output += "PILL: ";
        switch(state[1]){
            case 0: output += "UP\n"; break;
            case 1: output += "LEFT\n"; break;
            case 2: output += "DOWN\n"; break;
            case 3: output += "RIGHT\n"; break;
        }

        output += "Wall Above: ";
        if(state[2] == 1)
            output += "Yes.\t";
        else
            output += "No.\t";
        output += "Wall Left: ";
        if(state[3] == 1)
            output += "Yes.\t";
        else
            output += "No.\t";
        output += "Wall Below: ";
        if(state[4] == 1)
            output += "Yes.\t";
        else
            output += "No.\t";
        output += "Wall Right: ";
        if(state[5] == 1)
            output += "Yes.\n";
        else
            output += "No.\n";
        return output;
    }

    public static class QEntry {

        int [] state; MOVE action;
        int qIndex; float qValue;

        public QEntry(int[] state, MOVE action, int index){
            this.state = state;
            this.action = action;
            qValue = 0;
            qIndex = index;
        }

        /**
         * Updates the Q-Value of this state-action entry in the Q-Table using the formula:
         * Q(S,A) = Q(S, A) + Alpha * [R + Gamma * Q(S', A') - Q(S,A)]
         * @param reward: reward value observed for executing this entry.
         * @param futureAction: Q-Value of the next optimised action being taken.
         */
        public void updateValue(float reward, QEntry futureAction){
            qValue += LEARNING_RATE * (reward + DISCOUNT_FACTOR * futureAction.qValue - qValue);
        }

    }
}