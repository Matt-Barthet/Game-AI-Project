package entrants.ghosts.matt_barthet;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class GAGhostController extends IndividualGhostController {

    private final static float CONSISTENCY = 0.9f;    //attack Ms Pac-Man with this probability
    private final static int PILL_PROXIMITY = 15;        //if Ms Pac-Man is this close to a power pill, back away
    private final static int CHROMOSOME_SIZE = 10;
    private final static int POPULATION_SIZE = 10;
    private final static int COMPUTATIONAL_BUDGET = 40;

    private ArrayList<Gene> mPopulation;

    /**
     * Creates the starting population of Gene classes, whose chromosome contents are random
     * @size: The size of the population is passed as an argument from the main class
     */
    public GAGhostController(Constants.GHOST ghost) {
        super(ghost);
        int size = 10;
        mPopulation = new ArrayList<Gene>();
        for(int i = 0; i < size; i++){
            Gene entry = new Gene();
            entry.randomizeChromosome();
            mPopulation.add(entry);
        }
    }

    //TODO
    public Constants.MOVE getMove(Game game, long timeDue) {
        return null;
    }

    // Genetic Algorithm maxA testing method
    public void geneticAlgorithm( String[] args ){
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
    //TODO
    public void evaluateGeneration(){
        for(int i = 0; i < mPopulation.size(); i++){

        }
    }

    /**
     * With each gene's fitness as a guide, chooses which genes should mate and produce offspring.
     * The offspring are added to the population, replacing the previous generation's Genes either
     * partially or completely. The population size, however, should always remain the same.
     * If you want to use mutation, this function is where any mutation chances are rolled and mutation takes place.
     */
    //TODO
    public void produceNextGeneration(){

    }

    /**
     * Returns the Gene at position <b>index</b> of the mPopulation arrayList
     * @param index: the position in the population of the Gene we want to retrieve
     * @return the Gene at position <b>index</b> of the mPopulation arrayList
     */
    public Gene getGene(int index){ return mPopulation.get(index); }

    /**
     * @return the size of the population
     */
    public int size(){ return mPopulation.size(); }

    /**
     * @param generationCount: the current generation count of the population
     * @return the evaluation of the population's fitness values.
     */
    private String printEvaluation(int generationCount){
        float avgFitness=0.f;
        float minFitness=Float.POSITIVE_INFINITY;
        float maxFitness=Float.NEGATIVE_INFINITY;
        String bestIndividual="";
        String worstIndividual="";
        for(int i = 0; i < mPopulation.size(); i++){
            float currFitness = getGene(i).getFitness();
            avgFitness += currFitness;
            if(currFitness < minFitness){
                minFitness = currFitness;
                worstIndividual = getGene(i).getPhenotype();
            }
            if(currFitness > maxFitness){
                maxFitness = currFitness;
                bestIndividual = getGene(i).getPhenotype();
            }
        }

        if(mPopulation.size()>0){ avgFitness = avgFitness/mPopulation.size(); }
        String output = "Generation: " + generationCount;
        output += "\t AvgFitness: " + avgFitness;
        output += "\t MinFitness: " + minFitness + " (" + worstIndividual +")";
        output += "\t MaxFitness: " + maxFitness + " (" + bestIndividual +")";
        return output;
    }

    public class Gene{

        protected float mFitness;
        protected int mChromosome[];

        /**
         * Allocates memory for the mChromosome array and initializes any other data, such as fitness
         * We chose to use a constant variable as the chromosome size, but it can also be
         * passed as a variable in the constructor
         */
        public Gene() {
            mChromosome = new int[CHROMOSOME_SIZE];
            mFitness = 0.f;
        }

        /**
         * Randomizes the numbers on the mChromosome array to values between 0 and 3
         */
        //TODO
        public void randomizeChromosome(){

        }

        /**
         * Creates a number of offspring by combining (using crossover) the current
         * Gene's chromosome with another Gene's chromosome.
         * Usually two parents will produce an equal amount of offpsring, although
         * in other reproduction strategies the number of offspring produced depends
         * on the fitness of the parents.
         * @param other: the other parent we want to create offpsring from
         * @return Array of Gene offspring (default length of array is 2).
         * These offspring will need to be added to the next generation.
         */
        //TODO
        public Gene[] reproduce(Gene other){
            Gene[] result = new Gene[2];
            return result;
        }

        /**
         * Mutates a gene using inversion, random mutation or other methods.
         * This function is called after the mutation chance is rolled.
         * Mutation can occur (depending on the designer's wishes) to a parent
         * before reproduction takes place, an offspring at the time it is created,
         * or (more often) on a gene which will not produce any offspring afterwards.
         */
        //TODO
        public void mutate(){
        }

        /**
         * Sets the fitness, after it is evaluated in the GeneticAlgorithm class.
         * @param value: the fitness value to be set
         */
        public void setFitness(float value) { mFitness = value; }

        /**
         * @return the gene's fitness value
         */
        public float getFitness() { return mFitness; }

        /**
         * Returns the element at position <b>index</b> of the mChromosome array
         * @param index: the position on the array of the element we want to access
         * @return the value of the element we want to access (0 or 1)
         */
        public int getChromosomeElement(int index){ return mChromosome[index]; }

        /**
         * Sets a <b>value</b> to the element at position <b>index</b> of the mChromosome array
         * @param index: the position on the array of the element we want to access
         * @param value: the value we want to set at position <b>index</b> of the mChromosome array (0 or 1)
         */
        public void setChromosomeElement(int index, int value){ mChromosome[index]=value; }

        /**
         * Returns the size of the chromosome (as provided in the Gene constructor)
         * @return the size of the mChromosome array
         */
        public int getChromosomeSize() { return mChromosome.length; }

        public String getPhenotype() {
            // create an empty string
            StringBuilder result= new StringBuilder();
            for(int i = 0; i < mChromosome.length; i++){
                // populate it with either A's or a's, depending on the the
                switch(mChromosome[i]){
                    case 0:
                        result.append("L");
                        break;
                    case 1:
                        result.append("R");
                        break;
                    case 2:
                        result.append("U");
                        break;
                    case 3:
                        result.append("D");
                        break;
                    default:
                        result.append("E");
                        break;
                }
            }
            return result.toString();
        }

    };
}



