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
    private static Game currentGame;
    private static final int COMPUTATIONAL_BUDGET = 40;
    private static final ArrayList<int[]> stateSpace = initialiseStates();
    private static ArrayList<QEntry> qTable;

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
        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 2; j++){
                for(int k = 0; k < 2; k++){
                    for(int l = 0; l < 2; l++){
                        for(int m = 0; m < 4; m++){
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
        for (int[] state : stateSpace) {
            for (MOVE move: POSSIBLE_MOVES) {
                if(state[0] == 1 && move == MOVE.UP || state[2] == 1 && move == MOVE.DOWN)
                    continue;
                if(state[1] == 1 && move == MOVE.LEFT || state[3] == 1 && move == MOVE.RIGHT)
                    continue;
                qTable.add(new QEntry(state, move));
            }
        }
        return qTable;
    }

    /**
     * Ms. Pacman starts by updating her knowledge of the game state based on her current
     * location and entities (ghosts, pills) in her LOS. She then performs the Q-Learning
     * algorithm until she hits the computational budget.  The predictor is then updated,
     * and the highest rewarding move is chosen as Ms. Pacman's next move.
     * @param game: the current state of the game at this tick.
     * @param timeDue: how long Ms. Pacman has to decide about her move.
     * @return the chosen move based on the results of the algorithm.
     */
    public MOVE getMove(Game game, long timeDue) {
        updateObservations(game);
        observeGhosts(game);
        if(game != currentGame){ currentGame = game; }
        reinforcementLearning();
        predictions.update();
        return MOVE.NEUTRAL;
    }

    /**
     * Uses Q Learning to iteratively exploit and explore different actions in the state space
     * to identify the best action at this time. This action is then chosen by Ms. Pacman and
     * executed.
     */
    private void reinforcementLearning(){
        long startTime = new Date().getTime();
        while(new Date().getTime() < startTime + COMPUTATIONAL_BUDGET){
            chooseAction();
            applyAction();
            //updateValue(rewardFunction(currentGame));
        }
    }

    /**
     * TODO: Choose the next action to apply according to the greedy epsilon policy.
     */
    private void chooseAction(){

    }

    /**
     * TODO: Apply the action chosen in the previous step of the algorithm to the state being evaluated.
     */
    private void applyAction(){

    }

    /**
     * TODO: Calculate the reward to be assigned to the state-action pair according to any events that occur.
     */
    private int rewardFunction(Game game){
        if(game.wasPillEaten())
            return 1;
        if(game.wasPowerPillEaten())
            return 5;
        for (Constants.GHOST ghost : Constants.GHOST.values())
            if(game.wasGhostEaten(ghost))
                return 10;
        if(game.wasPacManEaten())
            return -10;
        return -1;
    }

    public class QEntry {

        int [] state; MOVE action;
        int qValue;

        public QEntry(int[] state, MOVE action){
            this.state = state;
            this.action = action;
            qValue = 0;
        }

        public void updateValue(int value){
            qValue = value;
        }
    }


}