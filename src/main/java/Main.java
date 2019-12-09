import examples.StarterGhost.Blinky;
import examples.StarterGhost.Inky;
import examples.StarterGhost.Pinky;
import examples.StarterGhost.Sue;
import examples.StarterGhostComm.*;
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

        boolean pacmanPO = false,
                ghostPO = false;

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setPacmanPO(pacmanPO)
                .setGhostPO(ghostPO)
                .setTickLimit(Integer.MAX_VALUE)
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);
        controllers.put(GHOST.INKY, new Inky());
        controllers.put(GHOST.BLINKY, new Blinky());
        controllers.put(GHOST.PINKY, new Pinky());
        controllers.put(GHOST.SUE, new Sue());

        executor.runGame(new MyPacMan(), new MASController(controllers), 0);
        //System.out.println(executor.runExperiment(new MyPacMan(), new MASController(controllers), 5, "Pacman PO: " + pacmanPO + " ghosts PO: " + ghostPO)[0].toString());
    }
}
