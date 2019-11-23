package entrants.pacman.matt_barthet;

import pacman.controllers.Controller;
import pacman.controllers.PacmanController;
import pacman.controllers.examples.po.POGhosts;
import pacman.game.Constants;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.Random;

import static pacman.game.Constants.DELAY;
import static pacman.game.Constants.MOVE;

public class MCTSController extends PacmanController{

	//The time budget constant is used to cap the number of iterations the MCTS may complete per move
	private static final long TIMEBUDGET = 40;

	private Controller<MOVE> randomPacman = new MCTSRandomPlayout();
	private Controller<EnumMap<GHOST, MOVE>> ghostController = new POGhosts();

	//Choosing the next move using MCTS Algorithm
	public MOVE getMove(Game game, long timeDue){
		int current = game.getPacmanCurrentNodeIndex();
		MCTSNode rootNode = new MCTSNode(null, current, game.getNeighbouringNodes(current), game);
		//Keep looping until the MCTS algorithm hits the computational budget
		long start = new Date().getTime();
		while (new Date().getTime() < start + TIMEBUDGET) {
			MCTSNode new_node = treePolicy(rootNode, game);
			float [] new_reward = defaultPolicy(new_node, game);
			backPropogate(new_node, new_reward);
		}

		//Return the direction of the best child
		MCTSNode bestChild = bestChild(rootNode, 1/Math.sqrt(2));
		return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), bestChild.getIndex(), DM.PATH);
	}

	//Policy to select and expand the game tree using UCB
	private MCTSNode treePolicy(MCTSNode node, Game game) {
		node.computeTerminal(game);
		//Keep moving through the tree until a terminal node is encountered
		while (!node.checkTerminal()){
			//Return a new leaf node if the current node is not fully expanded
			if (!node.checkExpanded()){
				return node.expand();
			}
			//Use UCB to select the best child of the current subtree
			node = bestChild(node, 1/Math.sqrt(2));
		}
		return node;
	}

	//Use the UCB formula to select the best child in the given node's subtree
	private MCTSNode bestChild(MCTSNode node, double coefficient) {
		if (node.getChildren().isEmpty())
			return node;

		ArrayList <MCTSNode> children = node.getChildren();
		double [] ucbValues = new double[children.size()];
		int bestChildIndex = 0; double largest = -10000;

		//IF this is the third visit or less choose a random node as best child
		if(node.getVisit() < 3){
			bestChildIndex =  (int)(Math.random() * ((children.size() - 1) + 1));
			return children.get(bestChildIndex);
		}
		//OTHERWISE Loop through children and calculate UCB values using formula
		for (int i = 0; i < children.size(); i++) {
			ucbValues[i] = children.get(i).getReward() + coefficient * Math.sqrt( Math.log( node.getVisit() ) / children.get(i).getVisit() );
			//If pac man dies upon visiting child, set score to negative value to discourage visiting
			if(children.get(i).checkTerminal()){
				ucbValues[i] = -10000;
			}
			if (ucbValues[i] > largest) {
				bestChildIndex = i;
				largest = ucbValues[i];
			}
		}
		return children.get(bestChildIndex);
	}

	//Simulate the node and return a reward value
	private float [] defaultPolicy(MCTSNode node, Game game) {
		/*------------------Simulation Setup------------------------*/
		Game simulationRun = game.copy();
		float playThroughScore = -simulationRun.getScore();
		float ghostScore = 0;

		/*----------------------Tree Phase--------------------------*/
		while (simulationRun.getPacmanCurrentNodeIndex() != node.getIndex()) {
			simulationRun.advanceGame(simulationRun.getNextMoveTowardsTarget(simulationRun.getPacmanCurrentNodeIndex(), node.getIndex(), DM.PATH),
					ghostController.getMove(simulationRun.copy(), System.currentTimeMillis()));

			if(simulationRun.wasPowerPillEaten()) {
				return(new float[]{checkPowerPillBonus(simulationRun), ghostScore});
			}
			for(Constants.GHOST ghost : Constants.GHOST.values()) {
				if(simulationRun.wasGhostEaten(ghost)){
					playThroughScore += simulationRun.getGhostCurrentEdibleScore();
					ghostScore += simulationRun.getGhostCurrentEdibleScore();
				}
			}
			if(simulationRun.wasPacManEaten()){
				playThroughScore = -500;
				//node.setDeath();
				return new float[]{playThroughScore, ghostScore};
			}
		}

		/*---------------------Random Phase-------------------------*/
		for(int i = 0; i < 200 && !simulationRun.gameOver(); i++)
		{
			simulationRun.advanceGame(randomPacman.getMove(simulationRun.copy(),System.currentTimeMillis()),
					ghostController.getMove(simulationRun.copy(),System.currentTimeMillis()));
			if(simulationRun.wasPowerPillEaten()){
				return(new float[]{checkPowerPillBonus(simulationRun), ghostScore});
			}
			for(Constants.GHOST ghost : Constants.GHOST.values()) {
				if(simulationRun.wasGhostEaten(ghost)){
					playThroughScore += simulationRun.getGhostCurrentEdibleScore();
					ghostScore += simulationRun.getGhostCurrentEdibleScore();
				}
			}
			if(simulationRun.wasPacManEaten()){
				playThroughScore = -400;
				break;
			}
		}

		playThroughScore += simulationRun.getScore();
		return new float[]{playThroughScore, ghostScore};
	}

	private int checkPowerPillBonus(Game simulationRun){
		int closeGhosts = 0;
		for(Constants.GHOST ghost : Constants.GHOST.values()) {
			int distance = simulationRun.getShortestPathDistance(simulationRun.getPacmanCurrentNodeIndex(), simulationRun.getGhostCurrentNodeIndex(ghost));
			if (distance < 300)
				closeGhosts ++;
		}
		if(closeGhosts == 0){
			return -500;
		}
		return 500;
	}

	//Back track up the tree and update the node statistics
	private void backPropogate(MCTSNode node, float [] reward) {
		while (node != null) {
			node.setVisit();
			node.setReward(reward);
			node = node.getParent();
		}
	}

}

//Node in the Game Tree, representing junctions in game space.
class MCTSNode {

	//Linking with the node's parent and children
	private MCTSNode parent;
	private ArrayList <MCTSNode> children = new ArrayList <MCTSNode>();

	//Number of times visited to be used for the UCB calculation
	private int visited;

	//Storing two different heuristics to be used in line with the current strategy
	private float pill_score;
	private float ghost_score;

	//Storing the open junctions
	private int [] openDirections;
	private int expandedDirections;
	private int nodeIndex = 0;

	//Store a copy of the game at this node and track if pac man is alive at this location
	private Game game;
	private boolean isAlive;

	//Direction of the junction
	private MOVE direction;

	//Constants to be used for determining the move selection tactics
	private static final int SURVIVALTRESHOLD = 20;

	enum Tactic{
		GHOST,
		PILL,
		SURVIVE
	};

	//Constructor for creating new node in the game tree
	public MCTSNode(MCTSNode parent, int nodeIndex, int [] neighbours, Game game){
		this.parent = parent;
		this.nodeIndex = nodeIndex;
		this.openDirections = neighbours;
		this.expandedDirections = 0;
		this.game = game;
	}

	/*---------------------------------------------------------------------------------*/
	/*                                Node Functions                                   */
	/*---------------------------------------------------------------------------------*/

	public MCTSNode expand() {

		MOVE direction = game.getMoveToMakeToReachDirectNeighbour(nodeIndex, openDirections[expandedDirections]);
		int currentIndex = openDirections[expandedDirections];
		int nextIndex;
		//Keep going through neighbours until closest junction is found
		while(game.getNeighbouringNodes(currentIndex).length != 3){
			nextIndex = game.getNeighbour(currentIndex, direction);
			if(nextIndex == -1) {
				//nextIndex = game.getNeighbouringNodes(currentIndex, direction)[0];
				//direction = game.getMoveToMakeToReachDirectNeighbour(currentIndex, nextIndex);
				break;
			}
			currentIndex = nextIndex;
		}

		MCTSNode newChild = new MCTSNode(this, currentIndex, game.getNeighbouringNodes(currentIndex), game);
		expandedDirections++;
		children.add(newChild);
		newChild.computeTerminal(game);
		return newChild;
	}

	//Node is considered expanded when all directions have been explored or pac man survives the visit
	public boolean checkExpanded() {
		return expandedDirections == openDirections.length && isAlive;
	}

	public void computeTerminal(Game game) {
		Game simulationRun = game.copy();
		int previousPosition;
		Controller<EnumMap<GHOST, MOVE>> ghostController = new POGhosts();

		while (simulationRun.getPacmanCurrentNodeIndex() != getIndex()) {
			previousPosition = simulationRun.getPacmanCurrentNodeIndex();
			simulationRun.advanceGame(simulationRun.getNextMoveTowardsTarget(simulationRun.getPacmanCurrentNodeIndex(), getIndex(), Constants.DM.PATH),
					ghostController.getMove(simulationRun.copy(), System.currentTimeMillis() + DELAY));
			if(simulationRun.wasPacManEaten()){
				isAlive = false;
				return;
			}
			if (simulationRun.getPacmanCurrentNodeIndex() == previousPosition){
				return;
			}
		}
		isAlive = true;
	}

	//Determine which of the tactics is being used by the agent for the move
	private Tactic chooseTactic(Game game) {

		int minDistance=Integer.MAX_VALUE;
		Constants.GHOST minGhost=null;
		for(Constants.GHOST ghost : Constants.GHOST.values()) {
			if (game.getGhostEdibleTime(ghost) > 0) {
				int distance = game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(), game.getGhostCurrentNodeIndex(ghost));
				if (distance < minDistance) {
					minDistance = distance;
					minGhost = ghost;
				}
			}
		}
		if(minGhost!=null)	{
			return Tactic.GHOST;
		}
		return Tactic.PILL;
	}

	/*---------------------------------------------------------------------------------*/
	/*                                Getters and Setters                              */
	/*---------------------------------------------------------------------------------*/

	public MCTSNode getParent() {
		return this.parent;
	}

	public boolean checkTerminal(){
		return !this.isAlive;
	}

	public float getReward() {
		switch(chooseTactic(game)) {
			case GHOST:
				return normalize(ghost_score);
			default:
				return normalize(pill_score);
		}
	}

	public int getVisit() {
		return visited;
	}

	public int getIndex() {
		return nodeIndex;
	}

	public ArrayList <MCTSNode> getChildren(){
		return children;
	}

	public void setReward(float [] rewards) {
		pill_score += rewards[0];
		ghost_score += rewards[1];
	}

	private float normalize(float x) {
		float min = -500;
		float max = 2000;
		float range = max - min;
		float inZeroRange = (x - min);
		float norm = inZeroRange / range;
		return norm;
	}

	public void setVisit() {
		visited++;
	}

}

class MCTSRandomPlayout extends Controller<MOVE>{

	private Random rnd=new Random();
	private MOVE[] allMoves=MOVE.values();
	private MOVE lastMove;
	private int lastLoc;

	/* (non-Javadoc)
	 * @see pacman.controllers.Controller#getMove(pacman.game.Game, long)
	 */
	public MOVE getMove(Game game, long timeDue)
	{
		int [] junctions = game.getJunctionIndices();
		lastMove = game.getPacmanLastMoveMade();
		for(int i = 0; i < junctions.length; i++){
			if(junctions[i] == game.getPacmanCurrentNodeIndex()){
				MOVE newMove = lastMove;
				while(newMove == lastMove){
					newMove = allMoves[rnd.nextInt(allMoves.length)];
				}
				lastLoc = game.getPacmanCurrentNodeIndex();
				return newMove;
			} else if(lastLoc == game.getPacmanCurrentNodeIndex()){
				return allMoves[rnd.nextInt(allMoves.length)];
			}
		}
		lastLoc = game.getPacmanCurrentNodeIndex();
		return lastMove;
	}

}
