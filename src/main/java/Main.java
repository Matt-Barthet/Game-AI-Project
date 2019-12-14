import entrants.pacman.matt_barthet.MyPacMan;
import examples.StarterGhost.Blinky;
import examples.StarterGhost.Inky;
import examples.StarterGhost.Pinky;
import examples.StarterGhost.Sue;
import examples.StarterGhostComm.*;
import examples.StarterISMCTS.InformationSetMCTSPacMan;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.controllers.examples.StarterPacMan;
import pacman.game.Constants.*;
import pacman.game.internal.POType;

import java.util.EnumMap;

/**
 * Created by pwillic on 06/05/2016.
 */
public class Main {


    public static void main(String[] args) {

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setPacmanPO(true)
                .setGhostPO(true)
                .setScaleFactor(2)
                .setTickLimit(4000)
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);
        controllers.put(GHOST.INKY, new Inky());
        controllers.put(GHOST.BLINKY, new Blinky());
        controllers.put(GHOST.PINKY, new Pinky());
        controllers.put(GHOST.SUE, new Sue());

        executor.runGameTimed(new MyPacMan(), new MASController(controllers));
        //executor.runGameTimed(new examples.StarterPacMan.MyPacMan(), new MASController(controllers));
        //System.out.println(executor.runExperiment(new MyPacMan(), new MASController(controllers), 5, "Pacman PO: " + pacmanPO + " ghosts PO: " + ghostPO)[0].toString());
    }
}
