import entrants.pacman.matt_barthet.MCTSController;
import examples.StarterISMCTS.InformationSetMCTSPacMan;
import entrants.ghosts.matt_barthet.*;
import entrants.pacman.matt_barthet.MyPacMan;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.game.Constants.*;
import java.util.EnumMap;

/**
 * Created by pwillic on 06/05/2016.
 */
public class Main {

    public static void main(String[] args) {

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setTickLimit(4000)
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);

        controllers.put(GHOST.INKY, new Inky());
        controllers.put(GHOST.BLINKY, new Blinky());
        controllers.put(GHOST.PINKY, new Pinky());
        controllers.put(GHOST.SUE, new Sue());

        executor.runGame(new InformationSetMCTSPacMan(), new MASController(controllers), 10);
    }
}
