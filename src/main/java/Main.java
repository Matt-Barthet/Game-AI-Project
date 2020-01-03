import entrants.pacman.matt_barthet.MyPacMan_GA;
import entrants.pacman.matt_barthet.MyPacMan_TDL;
import examples.StarterISMCTS.InformationSetMCTSPacMan;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.game.Constants.*;
import pacman.entries.pacman.*;

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
                .setTickLimit(Integer.MAX_VALUE)
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);
        controllers.put(GHOST.INKY, new examples.StarterGhost.Inky());
        controllers.put(GHOST.BLINKY, new examples.StarterGhost.Blinky());
        controllers.put(GHOST.PINKY, new examples.StarterGhost.Pinky());
        controllers.put(GHOST.SUE, new examples.StarterGhost.Sue());

        /*controllers.put(GHOST.INKY, new examples.StarterGhostComm.Inky());
        controllers.put(GHOST.BLINKY, new examples.StarterGhostComm.Blinky());
        controllers.put(GHOST.PINKY, new examples.StarterGhostComm.Pinky());
        controllers.put(GHOST.SUE, new examples.StarterGhostComm.Sue());*/

        //executor.runGameTimed(new MyPacMan_GA(), new MASController(controllers));
        executor.runGameTimed(new MyPacMan_TDL(), new MASController(controllers));
        //System.out.println(executor.runExperiment(new MyPacMan_TDL(), new MASController(controllers), 5, "Pacman PO: " + true + " ghosts PO: " + true)[0].toString());
    }
}
