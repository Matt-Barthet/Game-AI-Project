package entrants.pacman.matt_barthet;

import pacman.controllers.*;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Drawable;
import pacman.game.Game;
import pacman.game.info.GameInfo;
import pacman.game.internal.Ghost;
import pacman.game.internal.Maze;
import pacman.game.internal.PacMan;
import prediction.GhostLocation;
import prediction.PillModel;
import prediction.fast.GhostPredictionsFast;

import java.awt.*;
import java.util.*;
import java.util.List;

import static pacman.game.Constants.MAG;

public class MyPacMan extends PacmanController {

    /**
     * Initialising constants and variables required for the genetic algorithm.
     */
    private final static int CHROMOSOME_SIZE = 10;
    private final static int POPULATION_SIZE = 20;
    private final static int COMPUTATIONAL_BUDGET = 40;
    private final static int MUTATION_RATE = 50;
    private static ArrayList<Gene> mPopulation;
    private static ArrayList<Gene> elitePopulation;
    private static Gene chosenIndividual;
    private final static float alphaWeight = 1, betaWeight = 0f;

    /**
     * Initialising game component variables.
     */
    private static PillModel pillModel;
    private final static MOVE[] POSSIBLE_MOVES = new MOVE[]{MOVE.LEFT, MOVE.RIGHT, MOVE.UP, MOVE.DOWN};
    private static Maze currentMaze;
    private int[] ghostEdibleTime;
    private GhostPredictionsFast predictions;
    private ArrayList<Integer> targets = new ArrayList<>();

    /**
     * Initialises the population lists for the genetic algorithm.
     */
    public MyPacMan() {
        mPopulation = new ArrayList<>();
        elitePopulation = new ArrayList<>();
        ghostEdibleTime = new int[Constants.GHOST.values().length];
    }

    Game mostRecentGame;
    /**
     * Returns Ms. Pacman's chosen move for this game tick.
     * @param game: the current state of the game at this tick.
     * @param timeDue: how long Ms. Pacman has to decide about her move.
     * @return the chosen move based on the results of the algorithm.
     */
    public MOVE getMove(Game game, long timeDue) {

        if(currentMaze != game.getCurrentMaze()){
            currentMaze = game.getCurrentMaze();
            predictions = null;
            pillModel = null;
            Arrays.fill(ghostEdibleTime, -1);
        }

        if (game.gameOver()) return null;
        mostRecentGame = game;
        if (game.wasPacManEaten()) {
            predictions = null;
        }

        if (predictions == null) {
            predictions = new GhostPredictionsFast(game.getCurrentMaze());
            predictions.preallocate();
        }

        if (pillModel == null) {
            pillModel = new PillModel(game.getNumberOfPills());

            int[] indices = game.getCurrentMaze().pillIndices;
            for (int index : indices) {
                pillModel.observe(index, true);
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
        // Get observations of ghosts and pass them in to the predictor
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

        //Clear out Ms. Pacman's previous population of genes
        mPopulation.clear();
        elitePopulation.clear();

        //Create and randomly initialise a new population of genes
        for(int i = 0; i < POPULATION_SIZE; i++){
            Gene entry = new Gene();
            entry.randomizeChromosome();
            mPopulation.add(entry);
        }

        //Compute the genetic algorithm and return the first move of the best fitted individual
        geneticAlgorithm(game);
        predictions.update();

        return chosenIndividual.getChromosomeElement(0);
    }

    /**
     * Function to perform the genetic algorithm as long as it is within computational
     * budget. Once complete the individual with the best fitness in the resulting
     * population is chosen as Pac-man's best action.
     * @param game: current game being played.
     */
    private void geneticAlgorithm(Game game){
        int generationCount = 0;
        long start = new Date().getTime();

        while(new Date().getTime() < start + COMPUTATIONAL_BUDGET){
            evaluateGeneration(game);
            printEvaluation(generationCount++);
            produceNextGeneration();
        }
    }

    /**
     * For all members of the population, runs a heuristic that evaluates their fitness
     * based on their phenotype.
     * @param game: the current game being played.
     */
    private void evaluateGeneration(Game game){
        Gene mostFit = null;
        float bestFit = Integer.MIN_VALUE;
        float worstFit = Integer.MAX_VALUE;
        int worstFitLocation = 0;
        float fitness;

        for(int geneID = 0; geneID < mPopulation.size(); geneID++){

            Game simulation = getGameSimulation(game.copy());
            //Game simulation = game.copy();
            fitness = scoreFitness(simulation, geneID);
            mPopulation.get(geneID).setFitness(fitness);

            if(fitness >= bestFit){
                mostFit = getGene(geneID);
                bestFit = fitness;
            } else if (fitness <= worstFit){
                worstFit = fitness;
                worstFitLocation = geneID;
            }
        }

        assert mostFit != null;
        getGene(worstFitLocation).mChromosome = mostFit.mChromosome.clone();
        elitePopulation.add(mostFit);
    }

    /**
     * Evaluate the chromosome of an individual using a simulated copy of the game.
     * @param simulation: a copy of the game used for simulation.
     * @param geneID: the ID of the gene which the chromosome belongs to.
     * @return the fitness of the chromosome
     */
    private float scoreFitness(Game simulation, int geneID){
        float scoreFitness = 0, distanceFitness = 0;

        for(int moveID = 1; moveID <= CHROMOSOME_SIZE; moveID++){

            MOVE nextMove = mPopulation.get(geneID).getChromosomeElement(moveID-1);
            while(true){

                //Advance another step in the simulation based on the next micro action and ghost actions
                simulation.advanceGame(nextMove, getBasicGhostMoves(simulation));

                //Stop applying macro action if Ms. Pacman was eaten, and assign a harsh fitness score
                if(simulation.wasPacManEaten()){
                    scoreFitness = -100;
                    //distanceFitness = -100;
                    break;
                }

                /*
                Ms. Pacman is trying to maximise her score, update the current fitness according
                to the pills she's eating, adding a bias for pills closer to her current location
                 */
                if(simulation.wasPowerPillEaten()){
                    scoreFitness += 50 * 2f / (moveID);
                } else if(simulation.wasPillEaten()) {
                    scoreFitness += 10 * 2f / (moveID);
                } else {
                    for (Constants.GHOST ghost : Constants.GHOST.values()){
                        if (simulation.wasGhostEaten(ghost))
                            scoreFitness += simulation.getGhostCurrentEdibleScore();
                    }
                }

                //If the current micro action leads Ms.Pacman to a junction or barrier, skip to the next action
                if(simulation.getNeighbour(simulation.getPacmanCurrentNodeIndex(), nextMove) == -1)
                    break;
                if(simulation.getNeighbouringNodes(simulation.getPacmanCurrentNodeIndex()).length > 2)
                    break;
            }

            //If the result of the last micro action was a death for Ms.Pacman, stop simulating the individual
            if(scoreFitness == -100)
                break;

            /*
            Ms. Pacman is trying to close in on a distant pill, update her fitness with the negated distance
            from her current location in the simulation.
             */
            //If Ms. Pacman reaches the pill, assign the number of moves it took as the fitness and break
            //if(distanceFitness == 0){
                //distanceFitness = CHROMOSOME_SIZE - moveID;
             //   break;
            //}
        }

        return alphaWeight * (scoreFitness) + betaWeight * (distanceFitness);
    }

    private float normalize(float value, float minimum, float maximum) {
        float range = maximum - minimum;
        float inZeroRange = (value - minimum);
        float norm = inZeroRange / range;
        return norm;
    }

    /**
     * Take the current game being played and extract all of Ms. Pacman's knowledge of her
     * surroundings.
     * @param game: the current state of the game.
     * @return copy of the game capable of being simulated.
     */
    private Game getGameSimulation(Game game){
        GameInfo info = game.getPopulatedGameInfo();
        info.setPacman(new PacMan(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade(), 0, false));
        EnumMap<Constants.GHOST, GhostLocation> locations = predictions.sampleLocations();
        info.fixGhosts(ghost -> {
            GhostLocation location = locations.get(ghost);
            if (location != null) {
                int edibleTime = ghostEdibleTime[ghost.ordinal()];
                return new Ghost(ghost, location.getIndex(), edibleTime, 0, location.getLastMoveMade());
            } else {
                return new Ghost(ghost, game.getGhostInitialNodeIndex(), 0, 0, MOVE.NEUTRAL);
            }
        });

        for (int i = 0; i < pillModel.getPills().length(); i++) {
            info.setPillAtIndex(i, pillModel.getPills().get(i));
        }
        return game.getGameFromInfo(info);
    }

    /**
     * Function to get basic predictions of the ghost team, sending them directly at Ms. Pacman.
     * @param game: simulated copy of the current game.
     * @return moves for each individual ghost team member.
     */
    private EnumMap<Constants.GHOST, MOVE> getBasicGhostMoves(Game game) {
        EnumMap<Constants.GHOST, MOVE> moves = new EnumMap<>(Constants.GHOST.class);
        int pacmanLocation = game.getPacmanCurrentNodeIndex();
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            int index = game.getGhostCurrentNodeIndex(ghost);
            MOVE previousMove = game.getGhostLastMoveMade(ghost);
            if (game.isJunction(index)) {
                try {
                    MOVE move = (game.isGhostEdible(ghost))
                            ? game.getApproximateNextMoveAwayFromTarget(index, pacmanLocation, previousMove, Constants.DM.PATH)
                            : game.getNextMoveTowardsTarget(index, pacmanLocation, previousMove, Constants.DM.PATH);
                    moves.put(ghost, move);
                }catch(NullPointerException ignored){}
            } else {
                moves.put(ghost, previousMove);
            }
        }
        return moves;
    }

    /**
     * Select three random genes from the population and insert them into the tournament.
     * @return the gene with the best fitness of the three.
     */
    private Gene tournamentSelection(){

        Gene[] competition = new Gene[3];
        competition[0] = mPopulation.get(new Random().nextInt(size()));
        competition[1] = mPopulation.get(new Random().nextInt(size()));
        competition[2] = mPopulation.get(new Random().nextInt(size()));

        if(competition[0].getFitness() >= competition[1].getFitness() && competition[0].getFitness() >= competition[2].getFitness()){
            return competition[0];
        } else if (competition[1].getFitness() >= competition[0].getFitness() && competition[1].getFitness() >= competition[2].getFitness()){
            return competition[1];
        } else {
            return competition[2];
        }
    }

    /**
     * With each gene's fitness as a guide, chooses which genes should mate and produce offspring.
     * The offspring are added to the population, replacing the previous generation's Genes either
     * partially or completely.
     */
    private void produceNextGeneration(){

        //Next generation of the population stored here
        ArrayList<Gene> newPopulation = new ArrayList<>();

        chosenIndividual = elitePopulation.get(0);

        //Add the elite population straight into the new population with no crossover or mutation
        for(int i = 0; i < elitePopulation.size(); i++) {
            newPopulation.add(elitePopulation.get(0));
            mPopulation.remove(elitePopulation.get(0));
            elitePopulation.remove(0);
        }

        //Take the rest of the population, pair them together and produce new genes - mutate new genes at specified rate
        while(newPopulation.size() < POPULATION_SIZE){

            //Select two genes from the population using tournament selection (size 3)
            Gene parent1 = tournamentSelection();
            Gene parent2 = tournamentSelection();

            //Store the produced genes in an array for later use
            Gene[] newPair = parent1.reproduce(parent2);

            //Roll a die and check whether to mutate new genes
            newPair[0].mutate();
            newPair[1].mutate();

            //Add the newly generated and modified genes to the new generation pool
            newPopulation.add(newPair[0]);
            newPopulation.add(newPair[1]);
        }

        //Set the population to the new generation of genes
        mPopulation = (ArrayList)newPopulation.clone();
    }

    /**
     * @param generationCount : the current generation count of the population
     */
    private void printEvaluation(int generationCount){
        float avgFitness=0.f;
        float minFitness=Float.POSITIVE_INFINITY;
        float maxFitness=Float.NEGATIVE_INFINITY;
        String bestIndividual= "";
        String worstIndividual= "";


        for(int i = 0; i < mPopulation.size(); i++){
            float currFitness = getGene(i).getFitness();
            avgFitness += currFitness;
            if(currFitness < minFitness){
                minFitness = currFitness;
                worstIndividual = getPhenotype(getGene(i).mChromosome);
            }
            if(currFitness > maxFitness){
                maxFitness = currFitness;
                bestIndividual = getPhenotype(getGene(i).mChromosome);
            }
        }
        if(mPopulation.size()>0){ avgFitness = avgFitness/mPopulation.size(); }

        double sd = 0;
        for(int i = 0; i < size(); i++){
            float currFitness = getGene(i).getFitness();
            sd += (currFitness - avgFitness) * (currFitness - avgFitness) / size();
        }
        double standardDeviation = Math.sqrt(sd);

        String output = "Generation: " + generationCount;
        output += "\t AvgFitness: " + avgFitness;
        output += "\t Standard Deviation: " + standardDeviation;
        output += "\t MinFitness: " + minFitness + " (" + worstIndividual +")";
        output += "\t MaxFitness: " + maxFitness + " (" + bestIndividual +")";
        System.out.println(output);
    }

    /**
     * Converts the gene's genotype (integer array) into its phenotype (series of actions)
     * @return string containing gene's phenotype
     */
    private String getPhenotype(MOVE[] mChromosome) {
        StringBuilder result= new StringBuilder();
        for (MOVE move : mChromosome) {
            if (move == MOVE.LEFT)
                result.append("L");
            if (move == MOVE.RIGHT)
                result.append("R");
            if (move == MOVE.UP)
                result.append("U");
            if (move == MOVE.DOWN)
                result.append("D");
        }
        return result.toString();
    }

    /**
     * Returns the Gene at position <b>index</b> of the mPopulation arrayList
     * @param index: the position in the population of the Gene we want to retrieve
     * @return the Gene at position <b>index</b> of the mPopulation arrayList
     */
    private Gene getGene(int index){ return mPopulation.get(index); }

    /**
     * @return the size of the population
     */
    private int size(){ return mPopulation.size(); }

    /**
     * @class Gene: represents a series of actions for Pac-man to take (phenotype)
     * through the use of an integer array of values between 0 and 3 (genotype).
     */
    public class Gene{

        float mFitness;
        MOVE[] mChromosome;

        /**
         * Allocates memory for the mChromosome array and initializes any other data, such as fitness
         * We chose to use a constant variable as the chromosome size, but it can also be
         * passed as a variable in the constructor
         */
        Gene() {
            mChromosome = new MOVE[CHROMOSOME_SIZE];
            mFitness = 0.f;
        }

        /**
         * Randomizes the numbers on the mChromosome array to values between 0 and 3
         */
        void randomizeChromosome(){
            for(int i = 0; i < CHROMOSOME_SIZE; i++){
                setChromosomeElement(i, POSSIBLE_MOVES[new Random().nextInt(POSSIBLE_MOVES.length)]);
            }
        }

        /**
         * Creates 2 offspring by combining (using n-point crossover) the current
         * Gene's chromosome with another Gene's chromosome.
         * @param other: the other parent we want to create offpsring from
         * @return Array of Gene offspring (default length of array is 2).
         * These offspring will need to be added to the next generation.
         */
        Gene[] reproduce(Gene other){

            //Creating objects for the offspring of the chosen parents
            Gene[] result = new Gene[2];
            result[0] = new Gene();
            result[1] = new Gene();

            //Choose a random point for the crossover to flip
            int point = new Random().nextInt(CHROMOSOME_SIZE);

            //Loop through the chromosomes, taking elements from one parent and flipping to the other at n
            for (int i = 0; i < CHROMOSOME_SIZE; i++){
                if(i < point){
                    result[0].setChromosomeElement(i, getChromosomeElement(i));
                    result[1].setChromosomeElement(i, other.getChromosomeElement(i));
                } else {
                    result[1].setChromosomeElement(i, getChromosomeElement(i));
                    result[0].setChromosomeElement(i, other.getChromosomeElement(i));
                }
            }

            return result;
        }

        /**
         * Mutates a gene using inversion, random mutation or other methods.
         * This function is called after the mutation chance is rolled.
         * Mutation can occur (depending on the designer's wishes) to a parent
         * before reproduction takes place, an offspring at the time it is created,
         * or (more often) on a gene which will not produce any offspring afterwards.
         */
        void mutate(){

            //Loop through the chromosome and mutate the bits of the gene by choosing any random action
            //other than the current one.
            for(int i = 0; i < CHROMOSOME_SIZE; i++){

                //Roll a die and check whether the bit should be mutated or not
                if(new Random().nextInt(100) + 1 < MUTATION_RATE){
                    MOVE newMove =  POSSIBLE_MOVES[new Random().nextInt(POSSIBLE_MOVES.length)];
                    //Keep assigning a new random move until the move is different to the original one
                    while(newMove != mChromosome[i]){
                        newMove = POSSIBLE_MOVES[new Random().nextInt(POSSIBLE_MOVES.length)];
                    }
                    setChromosomeElement(i, newMove);
                }
            }
        }

        /**
         * Sets the fitness, after it is evaluated in the GeneticAlgorithm class.
         * @param value: the fitness value to be set
         */
        void setFitness(float value) { mFitness = value; }

        /**
         * @return the gene's fitness value
         */
        float getFitness() { return mFitness; }

        /**
         * Returns the element at position <b>index</b> of the mChromosome array
         * @param index: the position on the array of the element we want to access
         * @return the value of the element we want to access (0 or 1)
         */
        MOVE getChromosomeElement(int index){ return mChromosome[index]; }

        /**
         * Sets a <b>value</b> to the element at position <b>index</b> of the mChromosome array
         * @param index: the position on the array of the element we want to access
         * @param value: the value we want to set at position <b>index</b> of the mChromosome array (0 or 1)
         */
        public void setChromosomeElement(int index, MOVE value){ mChromosome[index]=value; }

        /**
         * Returns the size of the chromosome (as provided in the Gene constructor)
         * @return the size of the mChromosome array
         */
        public int getChromosomeSize() { return mChromosome.length; }

    };
}