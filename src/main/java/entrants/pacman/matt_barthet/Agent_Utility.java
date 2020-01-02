package entrants.pacman.matt_barthet;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.info.GameInfo;
import pacman.game.internal.Ghost;
import pacman.game.internal.Maze;
import pacman.game.internal.PacMan;
import prediction.GhostLocation;
import prediction.PillModel;
import prediction.fast.GhostPredictionsFast;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

public class Agent_Utility {

    public static PillModel pillModel;
    public static Maze currentMaze;
    public static int[] ghostEdibleTime;
    public static GhostPredictionsFast predictions;
    public final static Constants.MOVE[] POSSIBLE_MOVES = new Constants.MOVE[]{Constants.MOVE.LEFT, Constants.MOVE.RIGHT, Constants.MOVE.UP, Constants.MOVE.DOWN};

    /**
     * Function to check the conditions of the game and update Ms. Pacman's knowledge
     * of the game accordingly.
     * @param game: game being played.
     */
    public static void updateObservations(Game game){
        if(currentMaze != game.getCurrentMaze()){
            System.out.println("Starting a new maze.");
            currentMaze = game.getCurrentMaze();
            predictions = new GhostPredictionsFast(game.getCurrentMaze());
            predictions.preallocate();

            Agent_Utility.pillModel = new PillModel(game.getNumberOfPills());
            int[] indices = game.getCurrentMaze().pillIndices;
            for (int index : indices) {
                Agent_Utility.pillModel.observe(index, true);
            }
            Arrays.fill(ghostEdibleTime, -1);
        }
        if (game.wasPacManEaten()) {
            System.out.println("Ms. Pacman was eaten. Lives remaining: " + game.getPacmanNumberOfLivesRemaining());
            predictions = new GhostPredictionsFast(game.getCurrentMaze());
            predictions.preallocate();
        }
        updatePills(game);
    }

    /**
     * Update the predictor with the observations made by Ms. Pacman in this game tick,
     * refreshing the ghosts' location if they are within LOS.
     * @param game: game being played.
     */
    public static void observeGhosts(Game game){
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            if (ghostEdibleTime[ghost.ordinal()] != -1) {
                ghostEdibleTime[ghost.ordinal()]--;
            }
            int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
            if (ghostIndex != -1) {
                predictions.observe(ghost, ghostIndex, game.getGhostLastMoveMade(ghost));
                ghostEdibleTime[ghost.ordinal()] = game.getGhostEdibleTime(ghost);
            } else {
                List<GhostLocation> locations = predictions.getGhostLocations(ghost);
                locations.stream().filter(location -> game.isNodeObservable(location.getIndex())).forEach(location -> {
                    predictions.observeNotPresent(ghost, location.getIndex());
                });
            }
        }
    }

    /**
     * Update the pill model with new observations made by Ms. Pacman in this game tick.
     * @param game: game being played.
     */
    public static void updatePills(Game game){
        int[] pills = game.getPillIndices();
        int[] powerPills = game.getPowerPillIndices();

        for (int pill : pills) {
            int pillIndex = game.getPillIndex(pill);
            if(pillIndex != -1){
                Boolean pillStillAvailable = game.isPillStillAvailable(pillIndex);
                if (pillStillAvailable != null && pillStillAvailable)
                    pillModel.observe(pillIndex, true);
            }
        }
        for (int pill : powerPills) {
            int pillIndex = game.getPillIndex(pill);
            if(pillIndex!=-1){
                Boolean pillStillAvailable = game.isPillStillAvailable(pillIndex);
                if (pillStillAvailable != null && pillStillAvailable)
                    pillModel.observe(pillIndex, true);
            }
        }

        // Update the pill model with what isn't available anymore
        int pillIndex = game.getPillIndex(game.getPacmanCurrentNodeIndex());
        if (pillIndex != -1) {
            Boolean pillState = game.isPillStillAvailable(pillIndex);
            if (pillState != null && !pillState) {
                pillModel.observe(pillIndex, false);
            }
        }
    }

    /**
     * Take the current game being played and extract all of Ms. Pacman's knowledge of her
     * surroundings.
     * @param game: the current state of the game.
     * @return copy of the game capable of being simulated.
     */
    public static Game getGameSimulation(Game game, GhostPredictionsFast predictions, int [] ghostEdibleTime) {
        GameInfo info = game.getPopulatedGameInfo();
        info.setPacman(new PacMan(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade(), 0, false));
        EnumMap<Constants.GHOST, GhostLocation> locations = predictions.sampleLocations();
        info.fixGhosts(ghost -> {
            GhostLocation location = locations.get(ghost);
            if (location != null) {
                int edibleTime = ghostEdibleTime[ghost.ordinal()];
                return new Ghost(ghost, location.getIndex(), edibleTime, 0, location.getLastMoveMade());
            } else {
                return new Ghost(ghost, game.getGhostInitialNodeIndex(), 0, 0, Constants.MOVE.NEUTRAL);
            }
        });
        for (int i = 0; i < pillModel.getPills().length(); i++) {
            info.setPillAtIndex(i, pillModel.getPills().get(i));
        }
        return game.getGameFromInfo(info);
    }

    /**
     * Function to get basic predictions of the ghost team, sending them directly at Ms. Pacman.
     *
     * @param game: simulated copy of the current game.
     * @return moves for each individual ghost team member.
     */
    public static EnumMap<Constants.GHOST, Constants.MOVE> getBasicGhostMoves(Game game) {
        EnumMap<Constants.GHOST, Constants.MOVE> moves = new EnumMap<>(Constants.GHOST.class);
        int pacmanLocation = game.getPacmanCurrentNodeIndex();
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            int index = game.getGhostCurrentNodeIndex(ghost);
            Constants.MOVE previousMove = game.getGhostLastMoveMade(ghost);
            if (game.isJunction(index)) {
                try {
                    Constants.MOVE move = (game.isGhostEdible(ghost))
                            ? game.getApproximateNextMoveAwayFromTarget(index, pacmanLocation, previousMove, Constants.DM.PATH)
                            : game.getNextMoveTowardsTarget(index, pacmanLocation, previousMove, Constants.DM.PATH);
                    moves.put(ghost, move);
                } catch (NullPointerException ignored) {
                }
            } else {
                moves.put(ghost, previousMove);
            }
        }
        return moves;
    }

    /**
     * Normalise a value given it's expected maximum and minimum range (minimum assumed to be -1).
     * @param value: value to be normalised.
     * @param maximum: maximum value in the range.
     * @return normalised value.
     */
    public static float normalize(float value, float maximum) {
        float range = maximum - (float) -1;
        float inZeroRange = (value - (float) -1);
        return inZeroRange / range;
    }
}
