package entrants.pacman.matt_barthet;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import static entrants.pacman.matt_barthet.Agent_Utility.*;

public class MyPacMan_TDL extends PacmanController {

    /**
     * Initialises the population lists for the genetic algorithm.
     */
    public MyPacMan_TDL() {
        ghostEdibleTime = new int[Constants.GHOST.values().length];
    }

    /**
     * Returns Ms. Pacman's chosen move for this game tick.
     *
     * @param game: the current state of the game at this tick.
     * @param timeDue: how long Ms. Pacman has to decide about her move.
     * @return the chosen move based on the results of the algorithm.
     */
    public MOVE getMove(Game game, long timeDue) {

        updateObservations(game);
        observeGhosts(game);

        //Perform Learning Algorithm
        predictions.update();
        return MOVE.NEUTRAL;
    }

}