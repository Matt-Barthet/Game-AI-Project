import entrants.pacman.matt_barthet.MyPacMan_GA;
import entrants.pacman.matt_barthet.MyPacMan_TDL;
import examples.StarterISMCTS.InformationSetMCTSPacMan;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.controllers.examples.StarterPacMan;
import pacman.game.Constants.*;
import pacman.entries.pacman.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.EnumMap;

/**
 * Created by pwillic on 06/05/2016.
 */
public class Main {


    public static void main(String[] args) throws IOException {

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setPacmanPO(true)
                .setGhostPO(true)
                .setScaleFactor(2)
                .setTickLimit(Integer.MAX_VALUE)
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);
        EnumMap<GHOST, IndividualGhostController> comms_controllers = new EnumMap<>(GHOST.class);

        controllers.put(GHOST.INKY, new examples.StarterGhost.Inky());
        controllers.put(GHOST.BLINKY, new examples.StarterGhost.Blinky());
        controllers.put(GHOST.PINKY, new examples.StarterGhost.Pinky());
        controllers.put(GHOST.SUE, new examples.StarterGhost.Sue());

        comms_controllers.put(GHOST.INKY, new examples.StarterGhostComm.Inky());
        comms_controllers.put(GHOST.BLINKY, new examples.StarterGhostComm.Blinky());
        comms_controllers.put(GHOST.PINKY, new examples.StarterGhostComm.Pinky());
        comms_controllers.put(GHOST.SUE, new examples.StarterGhostComm.Sue());

        executor.runGame(new MyPacMan_TDL(), new MASController(controllers), 0);
        executor.runGame(new MyPacMan_GA(), new MASController(controllers), 0);

        /*String result = executor.runExperiment(new MyPacMan_GA(), new MASController(controllers), 10, "Pacman PO: " + true + " ghosts PO: " + true)[0].toString();
        System.out.println(result);
        result = executor.runExperiment(new MyPacMan_TDL(), new MASController(controllers), 10, "Pacman PO: " + true + " ghosts PO: " + true)[0].toString();
        System.out.println(result);*/
    }
}
