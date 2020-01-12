package entrants.pacman.matt_barthet;

import pacman.controllers.MASController;
import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static entrants.pacman.matt_barthet.Agent_Utility.*;

public class MyPacMan_TDL extends PacmanController {

    /**
     * Initialises the variables for the Q-Learning algorithm.
     */
    private static final int COMPUTATIONAL_BUDGET = 40;
    public static float LEARNING_RATE = 0.2f;
    private static final float DISCOUNT_FACTOR = 0.9f;
    private static final float EPSILON = 0.9f;
    public static int MAXIMUM_STEPS = 10;
    private static final ArrayList<int[]> stateSpace = initialiseStates();
    private static ArrayList<QEntry> qTable;
    private static Game currentGame;
    private static final MOVE[] moves = new MOVE[]{MOVE.UP, MOVE.LEFT, MOVE.DOWN, MOVE.RIGHT};
    Random randomGenerator;
    private float cumalativeScore = 0;

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
            for(int j = 0; j < 2; j++){
                for(int k = 0; k < 2; k++){
                    for(int l = 0; l < 2; l++){
                        for(int m = 0; m < 2; m++){
                            for(int n = 0; n < 2; n++){
                                for(int o = 0; o < 2; o++){
                                    for(int p = 0; p < 2; p++){
                                        for(int q = 0; q < 2; q++){
                                            for(int r = 0; r < 4; r++){
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
                if(state[moveToInteger(move) + 1] ==1)
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
        if(game.gameOver()) return null;
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

        int counter = 0;

        while(new Date().getTime() < startTime + COMPUTATIONAL_BUDGET){
            Game simulation = getGameSimulation(currentGame, predictions, ghostEdibleTime);
            learningEpisode(simulation, startState);
            counter++;
        }
        assert startState != null;
        QEntry bestMove = getBestEntry(getSimilarEntries(startState));

        try {
            FileWriter fr = new FileWriter( new File("Cum_Reward.txt"), true);
            fr.write(cumalativeScore/counter + "\n");
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bestMove.action;
    }

    /**
     * An learning episode refers to a string of moves Ms. Pacman tests from her current
     * in game position until the terminal condition is reached.  This is either hitting
     * the junction-step limit or if she's eaten by a ghost.
     * @param simulation: copy of the game to be used to simulate moves.
     * @param startingState: the current state of Ms. Pacman in the "real" game.
     */
    private void learningEpisode(Game simulation, QEntry startingState){
        QEntry currentState = startingState;

        //cumalativeScore = 0;
        for(int stepCount = 0; stepCount < MAXIMUM_STEPS && !simulation.wasPacManEaten(); stepCount++){

            //Choose the next move to make from this state and apply it.
            currentState = chooseAction(currentState);
            float reward = 0;

            while(true){

                //Advance another step in the simulation based on the next micro action and ghost actions
                simulation.advanceGame(currentState.action, getBasicGhostMoves(simulation));
                reward += rewardFunction(simulation);

                //If the current micro action leads Ms.Pacman to a junction or barrier, skip to the next action
                if(simulation.isJunction(simulation.getPacmanCurrentNodeIndex()))
                    break;
                if(simulation.getNeighbour(simulation.getPacmanCurrentNodeIndex(), currentState.action) == -1)
                    break;
                if(simulation.wasPacManEaten())
                    break;

            }

            //Find the first entry in the table with the resulting state and ensure it isn't a null pointer.
            QEntry nextState = getState(simulation, simulation.getPacmanCurrentNodeIndex()); assert nextState != null;

            //Update the Q-Value with the reward and best value of the next state.
            currentState.updateValue(reward, getBestEntry(getSimilarEntries(nextState)));

            //Set the current state to the newly derived state.
            currentState = nextState;
            cumalativeScore += reward;
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

        for(int i = 1; i < 5; i++){
            if(game.getNeighbour(location, moves[i-1]) == -1){
                currentState[i] = 1;
            } else {
                currentState[i] = 0;
            }
        }

        //Loop through every ghost, if the ghost is visible identify the direction and adjust the state.
        for(Constants.GHOST ghost: Constants.GHOST.values()){
            int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
            if(ghostLocation != -1) {
                MOVE direction = game.getNextMoveTowardsTarget(location, ghostLocation, Constants.DM.PATH);
                if(game.getGhostEdibleTime(ghost) > 0) {
                    currentState[9] = moveToInteger(direction);
                } else {
                    switch (moveToInteger(direction)) {
                        case 0: currentState[5] = 1; break;
                        case 1: currentState[6] = 1; break;
                        case 2: currentState[7] = 1; break;
                        case 3: currentState[8] = 1; break;
                    }
                }
            }
        }

        //Go through the Q-Table and extract the first entry with the same state.
        for (QEntry qEntry : qTable) {
            if (Arrays.equals(currentState, qEntry.state)) {
                return qEntry;
            }
        }

        //If no entry has been found print error and return a null pointer.
        System.err.println("ERROR: No entry in the Q-Table corresponds to current state!");
        return null;
    }

    /**
     * Choose the next action for Ms. Pacman to test using the epsilon-greedy policy.
     * @param firstEntry: entry containing the state we are looking at.
     * @return action to take in the given state for next step.
     */
    private QEntry chooseAction(QEntry firstEntry){
        ArrayList<QEntry> possibleMoves = getSimilarEntries(firstEntry);
        if(randomGenerator.nextFloat() < EPSILON){
            return getBestEntry(possibleMoves);
        } else {
            return possibleMoves.get(randomGenerator.nextInt(possibleMoves.size()));
        }
    }

    /**
     * Returns the reward to be attributed to a state-action execution according to the
     * resulting event.
     * @param game: game state being observed.
     * @return float score value.
     */
    private float rewardFunction(Game game){
        float score = 0;
        if(game.wasPacManEaten()) {
            return -100;
        }
        if(game.wasPillEaten()) {
            score += 10;
        } else if(game.wasPowerPillEaten()) {
            score += 50;
        }
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            if (game.wasGhostEaten(ghost)) {
                score += game.getGhostCurrentEdibleScore();
            }
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

    /**
     * Converter an integer array state representation into a readable text based
     * representation for debugging purposes.
     * @param state: integer array containing state being converted.
     * @return string containing state description.
     */
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
            case 4: output += "UNKNOWN\n"; break;
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
        output += "Ghost Above: ";
        if(state[6] == 1)
            output += "Yes.\t";
        else
            output += "No.\t";
        output += "Ghost Left: ";
        if(state[7] == 1)
            output += "Yes.\t";
        else
            output += "No.\t";
        output += "Ghost Below: ";
        if(state[8] == 1)
            output += "Yes.\t";
        else
            output += "No.\t";
        output += "Ghost Right: ";
        if(state[9] == 1)
            output += "Yes.\n";
        else
            output += "No.\n";
        return output;
    }

    /**
     * Internal class to be used to represent entries in the Q-Table. This improves
     * the organisation of the algorithm.
     */
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