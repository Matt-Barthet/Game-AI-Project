package entrants.pacman.matt_barthet;

import entrants.ghosts.matt_barthet.GAGhostController;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;
import pacman.game.info.GameInfo;
import pacman.game.internal.Ghost;
import pacman.game.internal.PacMan;
import prediction.GhostLocation;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.Random;

import static pacman.game.Constants.DELAY;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getMove() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., entrants.pacman.username).
 */
public class MyPacMan extends PacmanController {

    /**
     * Initialising constant properties of the ghost team which may influence their actions
     * during run time.
     */
    private final static MOVE[] POSSIBLE_MOVES = new MOVE[]{MOVE.LEFT, MOVE.RIGHT, MOVE.UP, MOVE.DOWN};

    /**
     * Initialising constants and variables required for the genetic algorithm.
     */
    private final static int CHROMOSOME_SIZE = 10;
    private final static int POPULATION_SIZE = 50;
    private final static int COMPUTATIONAL_BUDGET = 40;
    private final static int MUTATION_RATE = 35;
    private final static int ELITE_COUNT = 2;
    private ArrayList<Gene> mPopulation;
    private ArrayList<Gene> elitePopulation;
    private Gene chosenIndividual;
    private Game virtual_game;
    private MASController ghostTeam;

    /**
     * Creates the starting population of Gene classes, whose chromosome contents are random
     * @size: The size of the population is passed as an argument from the main class
     */
    public MyPacMan(MASController ghostTeam) {
        mPopulation = new ArrayList<>();
        elitePopulation = new ArrayList<>();
        this.ghostTeam = ghostTeam;
    }


    public MOVE getMove(Game game, long timeDue) {
        this.virtual_game = game;
        mPopulation.clear();
        elitePopulation.clear();

        for(int i = 0; i < POPULATION_SIZE; i++){
            Gene entry = new Gene();
            entry.randomizeChromosome();
            mPopulation.add(entry);
        }

        geneticAlgorithm();
        return chosenIndividual.mChromosome[0];
    }

    /**
     * Function to perform the genetic algorithm as long as it is within computational
     * budget. Once complete the individual with the best fitness in the resulting
     * population is chosen as Pac-man's best action.
     */
    private void geneticAlgorithm(){

        int generationCount = 0;
        long start = new Date().getTime();
        while(new Date().getTime() < start + COMPUTATIONAL_BUDGET){
            evaluateGeneration();
            printEvaluation(generationCount);
            produceNextGeneration();
            generationCount++;
        }
    }

    /**
     * For all members of the population, runs a heuristic that evaluates their fitness
     * based on their phenotype. The evaluation of this problem's phenotype is fairly simple,
     * and can be done in a straightforward manner. In other cases, such as agent
     * behavior, the phenotype may need to be used in a full simulation before getting
     * evaluated (e.g based on its performance)
     */
    private void evaluateGeneration(){
        Gene mostFit = null;
        Gene secondMostFit = null;
        int bestFit = Integer.MIN_VALUE;
        int secondBestFit = Integer.MIN_VALUE;
        int worstFit = Integer.MAX_VALUE;
        int worstFitLocation = 0;

        for(int i = 0; i < mPopulation.size(); i++){

            /*----------------------------------------------------------------------------------------*/
            /*                                    Fitness Function                                    */
            /*----------------------------------------------------------------------------------------*/
            Game simulation = virtual_game.copy(true);
            int fitness = 0;
            /*for(int j = 0; j < CHROMOSOME_SIZE; j++){

                MOVE nextMove = mPopulation.get(i).getChromosomeElement(j);

                for(int k = 0; k < 6; k++){
                    int location = simulation.getPacmanCurrentNodeIndex();

                    simulation.advanceGame(nextMove, ghostTeam.getMove());

                    if(location != simulation.getPacmanCurrentNodeIndex()){
                        System.out.println("PAC-MAN MOVED");
                    }

                }

                if(simulation.wasPacManEaten())
                    fitness = Integer.MIN_VALUE;
                if(simulation.wasPillEaten()) {
                    fitness += 1;
                    System.out.println("Pill Eaten");
                }
            }*/

            for(int j = 0; j < CHROMOSOME_SIZE; j++){
                if(mPopulation.get(i).getChromosomeElement(j) == MOVE.DOWN){
                    fitness++;
                }
            }

            mPopulation.get(i).setFitness(fitness);

            /*----------------------------------------------------------------------------------------*/
            /*                                  Checking Best Genes                                   */
            /*----------------------------------------------------------------------------------------*/
            if(fitness > bestFit){
                mostFit = mPopulation.get(i);
                bestFit = fitness;
            } else if (fitness > secondBestFit){
                secondMostFit = mPopulation.get(i);
                secondBestFit = fitness;
            } else if (fitness <= worstFit){
                worstFit = fitness;
                worstFitLocation = i;
            }
        }

        assert mostFit != null;
        mPopulation.get(worstFitLocation).mChromosome = mostFit.mChromosome.clone();
        elitePopulation.add(mostFit);
        elitePopulation.add(secondMostFit);
    }

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
     * partially or completely. The population size, however, should always remain the same.
     * If you want to use mutation, this function is where any mutation chances are rolled and mutation takes place.
     */
    private void produceNextGeneration(){

        //Next generation of the population stored here
        ArrayList<Gene> newPopulation = new ArrayList<>();

        //Add the elite population straight into the new population with no crossover or mutation
        for(int i = 0; i < ELITE_COUNT; i++){
            newPopulation.add(elitePopulation.get(0));
            mPopulation.remove(elitePopulation.get(0));
            chosenIndividual = elitePopulation.get(0);
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
        output += "\t Population Size: " + size();
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
         * Randomizes the numbers on the mChromosome array to values between 0 and 3
         * according to the available moves at the junction / corner.
         */
        void randomizeChromosomeJunction(){
            int location = virtual_game.getPacmanCurrentNodeIndex();
            int nextAction;
            for(int i = 0; i < CHROMOSOME_SIZE; i++){
                Constants.MOVE[] moves = virtual_game.getPossibleMoves(location);
                nextAction = new Random().nextInt(moves.length);
                setChromosomeElement(i, moves[nextAction]);
                location = virtual_game.getNeighbour(location, moves[nextAction]);
                while (virtual_game.getNeighbour(location, moves[nextAction]) != -1 && virtual_game.getNeighbouringNodes(location).length <= 2) {
                    location = virtual_game.getNeighbour(location, moves[nextAction]);
                }
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
            //System.out.println("Using n-point crossover, n = " + point);

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

            //Return the new genes created in an array of Genes
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