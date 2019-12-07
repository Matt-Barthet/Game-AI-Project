package entrants.pacman.matt_barthet;

import examples.StarterGhost.*;
import pacman.controllers.*;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.info.GameInfo;
import pacman.game.internal.PacMan;
import prediction.PillModel;

import java.util.*;

public class MyPacMan extends PacmanController {

    /**
     * Initialising constants and variables required for the genetic algorithm.
     */
    private final static int CHROMOSOME_SIZE = 8;
    private final static int POPULATION_SIZE = 100;
    private final static int COMPUTATIONAL_BUDGET = 40;
    private final static int MUTATION_RATE = 50;
    private static ArrayList<Gene> mPopulation;
    private static ArrayList<Gene> elitePopulation;
    private static Gene chosenIndividual;
    private static PillModel pillModel;
    private final static MOVE[] POSSIBLE_MOVES = new MOVE[]{MOVE.LEFT, MOVE.RIGHT, MOVE.UP, MOVE.DOWN};

    /**
     * Initialises the population lists for the genetic algorithm.
     */
    public MyPacMan() {
        mPopulation = new ArrayList<>();
        elitePopulation = new ArrayList<>();
    }

    /**
     * Returns Ms. Pacman's chosen move for this game tick.
     * @param game: the current state of the game at this tick.
     * @param timeDue: how long Ms. Pacman has to decide about her move.
     * @return the chosen move based on the results of the algorithm.
     */
    public MOVE getMove(Game game, long timeDue) {

        if(game.isGamePo()){
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
        }

        mPopulation.clear();
        elitePopulation.clear();

        for(int i = 0; i < POPULATION_SIZE; i++){
            Gene entry = new Gene();
            entry.randomizeChromosome();
            mPopulation.add(entry);
        }

        geneticAlgorithm(game);
        return chosenIndividual.mChromosome[0];
    }

    /**
     * Function to perform the genetic algorithm as long as it is within computational
     * budget. Once complete the individual with the best fitness in the resulting
     * population is chosen as Pac-man's best action.
     */
    private void geneticAlgorithm(Game game){

        int generationCount = 0;
        long start = new Date().getTime();
        while(new Date().getTime() < start + COMPUTATIONAL_BUDGET){
            evaluateGeneration(game);
            produceNextGeneration();
            generationCount++;
        }
        printEvaluation(generationCount);
    }

    /**
     * For all members of the population, runs a heuristic that evaluates their fitness
     * based on their phenotype.
     */
    //TODO - Fitness function using the appropriate heuristic at any given time.
    private void evaluateGeneration(Game game){
        Game simulationStart, simulation;
        Gene mostFit = null;
        int bestFit = Integer.MIN_VALUE;
        int worstFit = Integer.MAX_VALUE;
        int worstFitLocation = 0;
        int fitness;

        /*
          If the game is partially observable, construct a copy of the game based on
          Ms. Pacmnan's current knowledge of the maze.  Otherwise take a straight up
          copy of the game data structure for the forward model.
         */
        if(game.isGamePo()){
            simulationStart = getGameSimulation(game.copy());
        } else {
            simulationStart = game.copy();
        }

        for(int i = 0; i < mPopulation.size(); i++){

            simulation = simulationStart.copy();
            fitness = 0;

            for(int j = 0; j < CHROMOSOME_SIZE; j++){

                MOVE nextMove = mPopulation.get(i).getChromosomeElement(j);

                while(true){

                    simulation.advanceGame(nextMove, getBasicGhostMoves(simulation));

                    if(simulation.wasPowerPillEaten())
                        fitness += 50 * 2 / (j+1);
                    if(simulation.wasPillEaten())
                        fitness += 10 * 2 / (j+1);
                    for(Constants.GHOST ghost: Constants.GHOST.values())
                        if(simulation.wasGhostEaten(ghost))
                            fitness += simulation.getGhostCurrentEdibleScore() * 2 / (j+1);

                    if(simulation.getNeighbour(simulation.getPacmanCurrentNodeIndex(), nextMove) == -1)
                        break;
                    if(simulation.getNeighbouringNodes(simulation.getPacmanCurrentNodeIndex()).length > 2)
                        break;
                    if(simulation.wasPacManEaten()){
                        fitness = -1000 / (j+1);
                        break;
                    }

                }

                if(fitness < 0)
                    break;
            }


            mPopulation.get(i).setFitness(fitness);

            if(fitness > bestFit){
                mostFit = mPopulation.get(i);
                bestFit = fitness;
            } else if (fitness <= worstFit){
                worstFit = fitness;
                worstFitLocation = i;
            }
        }

        assert mostFit != null;
        mPopulation.get(worstFitLocation).mChromosome = mostFit.mChromosome.clone();
        elitePopulation.add(mostFit);
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
     * Take the current game being played and extract all of Ms. Pacman's knowledge of her
     * surroundings.
     * @param game: the current state of the game.
     * @return copy of the game capable of being simulated.
     */
    //TODO - Fix bugs in the game state copy
    private Game getGameSimulation(Game game){
        GameInfo virtualGame = game.getPopulatedGameInfo();
        virtualGame.setPacman(new PacMan(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade(), 0, false));

        for (int i = 0; i < pillModel.getPills().length(); i++) {
            virtualGame.setPillAtIndex(i, pillModel.getPills().get(i));
        }

        return game.getGameFromInfo(virtualGame);
    }

    /**
     * Function to get basic predictions of the ghost team, sending them directly at Ms. Pacman.
     * @param game: simulated copy of the current game.
     * @return moves for each individual ghost team member.
     */
    //TODO - Identify how you can tailor this generic function to your project
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
                }catch(NullPointerException npe){
                    System.err.println("PacmanLocation: " + pacmanLocation + " Maze Index: " + game.getMazeIndex() + " Last Move: " + previousMove);
                }
            } else {
                moves.put(ghost, previousMove);
            }
        }
        return moves;
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
    public int size(){ return mPopulation.size(); }

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
        String output = "Generation: " + generationCount;
        output += "\t AvgFitness: " + avgFitness;
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