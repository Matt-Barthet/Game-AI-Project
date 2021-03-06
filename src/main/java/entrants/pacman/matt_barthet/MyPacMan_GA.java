package entrants.pacman.matt_barthet;

import pacman.controllers.*;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import prediction.GhostLocation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static entrants.pacman.matt_barthet.Agent_Utility.*;

public class MyPacMan_GA extends PacmanController {

    /**
     * Initialising constants and variables required for the genetic algorithm.
     */
    private static int CHROMOSOME_SIZE = 10;
    private final static int POPULATION_SIZE = 20;
    private final static int COMPUTATIONAL_BUDGET = 40;
    private final static int MUTATION_RATE = 50;
    private static ArrayList<Gene> mPopulation;
    private static ArrayList<Gene> elitePopulation;
    private static Gene chosenIndividual;
    private final static float alphaWeight = 1f, betaWeight = 0f;
    private int moveCounter = 0, generationCount = 0;
    private boolean moveCalculated = false;
    private Constants.GHOST edibleGhost;
    private File file = new File("Genetic_Data_" + new Date().getTime() + ".txt");
    private FileWriter fr;

    /**
     * Initialises the population lists for the genetic algorithm.
     */
    public MyPacMan_GA(){
        mPopulation = new ArrayList<>();
        elitePopulation = new ArrayList<>();
        ghostEdibleTime = new int[Constants.GHOST.values().length];

        try {
            fr = new FileWriter(file, true);
            fr.write("Ms. Pacman Rolling Horizon Agent Test Run.\n");
            fr.write("Chromosome Size: " + CHROMOSOME_SIZE + "\n");
            fr.write("Population Size: " + POPULATION_SIZE + "\n");
            fr.write("Computational Budget: " + COMPUTATIONAL_BUDGET + "\n");
            fr.write("Mutation Rate: " + MUTATION_RATE + "\n");
            fr.write("\n");
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns Ms. Pacman's chosen move for this game tick.
     * @param game: the current state of the game at this tick.
     * @param timeDue: how long Ms. Pacman has to decide about her move.
     * @return the chosen move based on the results of the algorithm.
     */
    public MOVE getMove(Game game, long timeDue) {

        if (game.gameOver()) return null;

        updateObservations(game);

        if(chosenIndividual != null){
            //If the current micro action leads Ms.Pacman to a junction or barrier, skip to the next action
            if(game.isJunction(game.getPacmanCurrentNodeIndex())){
                //System.out.println("Ms. Pacman has reached a junction. Incrementing Move Counter.");
                moveCounter++;
            } else {
                while(true){
                    if(moveCounter < CHROMOSOME_SIZE && game.getNeighbour(game.getPacmanCurrentNodeIndex(), chosenIndividual.getChromosomeElement(moveCounter)) == -1) {
                        //System.out.println("Ms. Pacman has collided with an obstacle. Incrementing Move Counter.");
                        moveCounter++;
                    } else {
                        break;
                    }
                }
            }
            if(chosenIndividual.getFitness() == (alphaWeight * normalize(0, 500))){
                //moveCounter = CHROMOSOME_SIZE;
            }
        }

        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            if (ghostEdibleTime[ghost.ordinal()] != -1) {
                ghostEdibleTime[ghost.ordinal()]--;
            }
            int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
            if (ghostIndex != -1) {
                predictions.observe(ghost, ghostIndex, game.getGhostLastMoveMade(ghost));
                ghostEdibleTime[ghost.ordinal()] = game.getGhostEdibleTime(ghost);
                if(moveCounter < CHROMOSOME_SIZE){
                    Game simulation = getGameSimulation(game, predictions, ghostEdibleTime);
                    if(evaluateIndividual(simulation, chosenIndividual, moveCounter + 1) == 0) {
                        moveCounter = CHROMOSOME_SIZE;
                        //System.out.println("Ms. Pacman has observed a hostile ghost. Recalculating route!");
                    }
                    if(ghostEdibleTime[ghost.ordinal()] > 0 && edibleGhost == null) {
                        moveCounter = CHROMOSOME_SIZE;
                        edibleGhost = ghost;
                        //System.out.println("Ms. Pacman has observed a NEW edible ghost. Recalculating route!");
                    } else if (edibleGhost == ghost && ghostEdibleTime[ghost.ordinal()] == 0) {
                        edibleGhost = null;
                        //System.out.println("Ms. Pacman has observed that edible ghost is no longer edible. Resetting variable.");
                    }
                }
            } else {
                List<GhostLocation> locations = predictions.getGhostLocations(ghost);
                locations.stream().filter(location -> game.isNodeObservable(location.getIndex())).forEach(location -> {
                    predictions.observeNotPresent(ghost, location.getIndex());
                });
            }

            if(game.wasGhostEaten(ghost) && ghost == edibleGhost) {
                edibleGhost = null;
                moveCounter = CHROMOSOME_SIZE;
                //System.out.println("Edible Ghost Eaten");
            }
        }

        if(moveCounter >= CHROMOSOME_SIZE - 1) {
            moveCounter = 0;
            moveCalculated = false;
            generationCount = 0;
        }

        if(moveCounter == 0 && !moveCalculated) {

            //Clear out Ms. Pacman's previous population of genes
            mPopulation.clear();
            elitePopulation.clear();

            //Create and randomly initialise a new population of genes
            for (int i = 0; i < POPULATION_SIZE; i++) {
                Gene entry = new Gene();
                entry.randomizeChromosome();
                mPopulation.add(entry);
            }

            //Compute the genetic algorithm and return the first move of the best fitted individual
            try {
                fr = new FileWriter(file, true);
                geneticAlgorithm(game);
                moveCalculated = true;
                fr.append("\n");
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        predictions.update();
        return chosenIndividual.getChromosomeElement(moveCounter);
    }

    /**
     * Function to perform the genetic algorithm as long as it is within computational
     * budget. Once complete the individual with the best fitness in the resulting
     * population is chosen as Pac-man's best action.
     * @param game: current game being played.
     */
    private void geneticAlgorithm(Game game) throws IOException {
        long start = new Date().getTime();
        fr.write("Generation Evolution for Next Move: \n");
        fr.write("Generation\tAverage Fitness\t\tStandard Deviation\n");
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
        float bestFit = Integer.MIN_VALUE, worstFit = Integer.MAX_VALUE, fitness;
        int worstFitLocation = 0;
        Gene mostFit = null;

        for(int geneID = 0; geneID < mPopulation.size(); geneID++){
            Game simulation = getGameSimulation(game, predictions, ghostEdibleTime);
            fitness = evaluateIndividual(simulation, mPopulation.get(geneID), 1);
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
     * @param individual: the gene which the chromosome being evaluated belongs to.
     * @return the fitness of the chromosome
     */
    private float evaluateIndividual(Game simulation, Gene individual, int startingPoint){
        float scoreFitness = -simulation.getScore();

        for(int moveID = startingPoint; moveID <= CHROMOSOME_SIZE; moveID++){

            MOVE nextMove = individual.getChromosomeElement(moveID-1);

            while(true){

                //Advance another step in the simulation based on the next micro action and ghost actions
                //simulation.advanceGame(nextMove, this.ghosts.getMove(simulation.copy(), 0));
                simulation.advanceGame(nextMove, getBasicGhostMoves(simulation));

                //Stop applying action if Ms. Pacman was eaten, and assign a harsh fitness score
                if(simulation.wasPacManEaten())
                    return 0;
                //If the current micro action leads Ms.Pacman to a junction or barrier, skip to the next action
                if(simulation.isJunction(simulation.getPacmanCurrentNodeIndex()))
                    break;
                if(simulation.getNeighbour(simulation.getPacmanCurrentNodeIndex(), nextMove) == -1)
                    break;

            }
        }

        /*
        Ms. Pacman is trying to maximise her score, update the current fitness according
        to the resulting score after simulating the current move set.
         */
        scoreFitness += simulation.getScore();

        return alphaWeight * normalize(scoreFitness, 500);
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
    private void printEvaluation(int generationCount) throws IOException{
        float avgFitness=0.f, sd = 0;
        for(int i = 0; i < mPopulation.size(); i++){
            float currFitness = getGene(i).getFitness();
            avgFitness += currFitness;
        }
        if(mPopulation.size() > 0)
            avgFitness = avgFitness / mPopulation.size();
        for(int i = 0; i < size(); i++){
            float currFitness = getGene(i).getFitness();
            sd += (currFitness - avgFitness) * (currFitness - avgFitness) / size();
        }
        double standardDeviation = Math.sqrt(sd);
        fr.write(generationCount + "\t\t\t" + avgFitness + "\t\t\t" + standardDeviation + "\n");
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