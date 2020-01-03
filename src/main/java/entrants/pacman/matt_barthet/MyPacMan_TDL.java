package entrants.pacman.matt_barthet;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.Date;

import static entrants.pacman.matt_barthet.Agent_Utility.*;

public class MyPacMan_TDL extends PacmanController {

    /**
     * Initialises the variables for the Q-Learning algorithm.
     */
    private static Game currentGame;
    private static final int COMPUTATIONAL_BUDGET = 40;

    public MyPacMan_TDL() {
        ghostEdibleTime = new int[Constants.GHOST.values().length];
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
        initialiseStates();
        long startTime = new Date().getTime();
        while(new Date().getTime() < startTime + COMPUTATIONAL_BUDGET){
            chooseAction();
            applyAction();
            updateValue(rewardFunction(currentGame));
        }
    }

    /**
     * TODO: Initialise the states in the Q-Table.
     */
    private void initialiseStates(){

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

    /**
     * TODO: Update the Q-Value in the table according to the reward given and the Q-Learning equation.
     */
    private void updateValue(int value){

    }
}