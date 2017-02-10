package player;

import scotlandyard.*;
import solution.ScotlandYardModel;
import scotlandyard.MoveTicket;

import java.util.HashSet;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
/**
 * The MyAIPlayer class is an AI that
 * makes an intelligent move from the given set of moves by performing Adversarial search. Since the
 * MyAIPlayer implements Player, the only required method is
 * notify(), which takes the location of the player and the
 * list of valid moves. The return value is the desired move,
 * which must be one from the list.
 */
public class MyAIPlayer implements Player {
    ScotlandYardView view;
    String graphFilename;
    private Graph map;
    final int totalNodes;
    final int INF = 10000;
    public int depth = 4; 
    public int indi = 0;
    public int i = 0;
    public int signal  = 0;
    public int i3 = 0;
    public int secretCounter = 0;
   
    public Move selectMove = MoveTicket.instance(Colour.Black,Ticket.Taxi, 185);
    int [][] distanceMatrix; 
    List<RealPlayer> simulatedPlayers = new ArrayList<RealPlayer>();
    List<Integer> destinations = new ArrayList<Integer>();
    List<Integer> destinationBlue = new ArrayList<Integer>();
    List<Integer> destinationGreen = new ArrayList<Integer>();
    List<Integer> destinationRed = new ArrayList<Integer>();
    List<Integer> destinationWhite = new ArrayList<Integer>();
    List<Integer> destinationYellow = new ArrayList<Integer>();
    List<Move> orderedMoves = new ArrayList<Move>();
    Boolean timerIndex = true;
    /**
     * Runs a simultaneous Timer to keep track of time left to explore the tree
     * If it runs out of time it selects the best move at that point of Time
     * @author lenovo
     *
     */
    public class TimerMove extends TimerTask{
        public void run() {
            timerIndex = false;
        }
    }
    /**
     * 
     * @param move The list of valid moves from that node
     * @param model The Model (or node) which needs to be scored
     * @param distance Total distance of detectives from Mr. X
     * @return the score for the current node
     */
    public int score (List<Move> move,Model model,int distance) {
    		
            int score = 0;
            int minDistance = 100;
            final int mapCorner = 100;
            final int mapMain = 200;
            final int movePoint = 1;
            score = score + (move.size()*movePoint);
            int neighbours = map.getEdges(model.getPlayerLocation(Colour.Black)).size();
            if(neighbours < 4)
            	score += mapCorner;
            else
            	score += mapMain;
            score += 20*distance;
            minDistance = minDistance(model);
           
            if(minDistance ==1) {
            	
            	score = score - (score/2);
            	
            }
            else
            	score += 50*minDistance;
            return score;
            
    }
    /**
     * MyAIPlayer class constructor
     * @param view ScotlandYardView object
     * @param graphFilename The file that stores the map or graph
     */
    public MyAIPlayer(ScotlandYardView view, String graphFilename) {
        //TODO: A better AI makes use of `view` and `graphFilename`.
        this.view = view;
        this.graphFilename = graphFilename;
        ScotlandYardGraphReader read = new ScotlandYardGraphReader();
        try {map = read.readGraph(graphFilename);}
        catch(Exception e) { System.out.println("Inexistent file");}
        totalNodes = map.getNodes().size();
        this.distanceMatrix = new int[totalNodes][totalNodes];
    		floydMarshall();
        
    }
    /**
     * This class runs a Concurrent Thread in the background while the detectives play so that it can order moves and do 
     * some pre-computation. This helps in faster pruning when it is Mr. X's turn 
     * @author lenovo
     *
     */
    public class ProcedureA implements Runnable{
    	Model testModel;
    	Move move;
    	Model test;
    	ProcedureA(Model model,Move move) {
    		this.move = move;
    		simulatedPlayers.clear();
    		initialiseModel(model);
    		testModel = new Model(view,simulatedPlayers,graphFilename,view.getRounds(),model.roundCounter);
    		modifyModel(testModel,move);
    	}
    	public void run() {
    		System.out.println("Name: "+Thread.currentThread().getName());
    		List<Integer> scores = new ArrayList<Integer>();
    		int childDistance;
    		List<Move> moves = new ArrayList<Move>(removeDuplicates(testModel.validMoves(Colour.Black)));
    		for(Move move1 : moves) {
    			simulatedPlayers.clear();
    			initialiseModel(testModel);
    			test = new Model(view,simulatedPlayers,graphFilename,view.getRounds(),testModel.roundCounter);
    			modifyModel(test,move1);
    			childDistance = totalDistance(test);
    			scores.add(score(removeDuplicates(test.validMoves(Colour.Black)),test,childDistance));
    			System.out.println(move1.toString());
    		}
    		orderedMoves.clear();
    		orderedMoves.addAll(sortMoves(moves,scores));
    	}

    }
    /**
     * Sorts a given list of moves from best to worst
     * @param list The Move List to be ordered
     * @param score The list of scores for the moves 
     * @return The list of sorted moves
     */
    List<Move> sortMoves(List<Move> list, List<Integer> score) {
        Move aux, auxM1, auxM2;
        Integer auxScore1, auxScore2;
        for(int i = 0; i< list.size(); i++)
            for(int j = 0; j< list.size(); j++)
                if(score.get(i) > score.get(j)) {
                    auxScore1 = score.get(i);
                    auxScore2 = score.get(j);
                    score.remove(auxScore1);
                    score.add(i, auxScore2);
                    score.remove(auxScore2);
                    score.add(j, auxScore1);
                    auxM1 = list.get(i);
                    auxM2 = list.get(j);
                    list.remove(auxM1);
                    list.add(i, auxM2);
                    list.remove(auxM2);
                    list.add(j, auxM1);
                }
        //System.out.println(score);
        return list;
    }
    /**
     * Initializes the root node with the actual current status of the board
     * @param mrxLocation Mr. X actual location
     */
    public void initialise(int mrxLocation) {
    	for(Colour colour :view.getPlayers()) {
    		Map<Ticket, Integer> tickets = new HashMap<Ticket, Integer>();
    		if(colour.equals(Colour.Black)) {
    			tickets.put(Ticket.Bus, view.getPlayerTickets(Colour.Black, Ticket.Bus));
    			tickets.put(Ticket.Taxi, view.getPlayerTickets(Colour.Black, Ticket.Taxi));
    			tickets.put(Ticket.Underground, view.getPlayerTickets(Colour.Black, Ticket.Underground));
    			tickets.put(Ticket.Double, view.getPlayerTickets(Colour.Black, Ticket.Double));
    			tickets.put(Ticket.Secret, view.getPlayerTickets(Colour.Black, Ticket.Secret));
    			System.out.println("X Location "+mrxLocation);
    			simulatedPlayers.add(new RealPlayer(Colour.Black,mrxLocation,tickets));
    		}
    		else {
    			
    			tickets.put(Ticket.Bus, view.getPlayerTickets(colour, Ticket.Bus));
    			tickets.put(Ticket.Taxi, view.getPlayerTickets(colour, Ticket.Taxi));
    			tickets.put(Ticket.Underground, view.getPlayerTickets(colour, Ticket.Underground));
    			tickets.put(Ticket.Double, view.getPlayerTickets(colour, Ticket.Double));
    			tickets.put(Ticket.Secret, view.getPlayerTickets(colour, Ticket.Secret));
    			simulatedPlayers.add(new RealPlayer(colour,view.getPlayerLocation(colour),tickets));
    		}
    		
    	}
    	
    }
    /**
     * Clones a new Model using an existing Model 
     * @param model Model to be used to clone a new Model
     */
    public void initialiseModel(Model model) {
    	for(Colour colour :view.getPlayers()) {
    		Map<Ticket, Integer> tickets = new HashMap<Ticket, Integer>();
    		if(colour.equals(Colour.Black)) {
    			
    			tickets.put(Ticket.Bus, model.getPlayerTickets(Colour.Black, Ticket.Bus));
    			tickets.put(Ticket.Taxi, model.getPlayerTickets(Colour.Black, Ticket.Taxi));
    			tickets.put(Ticket.Underground, model.getPlayerTickets(Colour.Black, Ticket.Underground));
    			tickets.put(Ticket.Double, model.getPlayerTickets(Colour.Black, Ticket.Double));
    			tickets.put(Ticket.Secret, model.getPlayerTickets(Colour.Black, Ticket.Secret));
    			simulatedPlayers.add(new RealPlayer(Colour.Black,model.getPlayerLocation(Colour.Black),tickets));
    		}
    		else {
    			tickets.put(Ticket.Bus, model.getPlayerTickets(colour, Ticket.Bus));
    			tickets.put(Ticket.Taxi, model.getPlayerTickets(colour, Ticket.Taxi));
    			tickets.put(Ticket.Underground, model.getPlayerTickets(colour, Ticket.Underground));
    			tickets.put(Ticket.Double, model.getPlayerTickets(colour, Ticket.Double));
    			tickets.put(Ticket.Secret, model.getPlayerTickets(colour, Ticket.Secret));
    			simulatedPlayers.add(new RealPlayer(colour,model.getPlayerLocation(colour),tickets));
    		}
    		
    	}
    } 
    /**
     * Plays a move which has to be simulated by the AI
     * @param colour Colour of Detective playing the move
     * @param move Move to be Played
     */
    public void modifyModel(Model model,Move move) {
    	if(move instanceof MoveDouble) {
			model.getPlayer(move.colour).location = ((MoveDouble) move).move2.target;
			int valdb = model.getPlayer(move.colour).tickets.get(Ticket.Double);
			model.getPlayer(move.colour).tickets.put(Ticket.Double, valdb-1);
			int val1 = model.getPlayer(move.colour).tickets.get(((MoveDouble) move).move1.ticket);
			model.getPlayer(move.colour).tickets.put(((MoveDouble) move).move1.ticket, val1-1);
			int val2 = model.getPlayer(move.colour).tickets.get(((MoveDouble) move).move2.ticket);
			model.getPlayer(move.colour).tickets.put(((MoveDouble) move).move2.ticket, val2-1);
			
		}
		else if(move instanceof MoveTicket) {
			model.getPlayer(move.colour).location = ((MoveTicket) move).target;
			int val = model.getPlayer(move.colour).tickets.get(((MoveTicket) move).ticket);
			model.getPlayer(move.colour).tickets.put(((MoveTicket) move).ticket, val-1);
			
		}
		else
			;
    	 if(move.colour!= Colour.Black && !(move instanceof MovePass))                                          //if detective -> add used ticket to MrX
             model.getPlayer(Colour.Black).tickets.put(((MoveTicket) move).ticket, model.getPlayerTickets(Colour.Black,((MoveTicket) move).ticket) + 1);
         else
             model.roundCounter++;                                                     //if MrX -> go to next round
    	
    }
    /**
     * Calculates all neighbours of a node
     * @param node The node whose neighbours are to be calculated
     * @return List of all the neighbour nodes
     */
    public List<Integer> neighbours(int node) {
    	List<Integer> neighbours = new ArrayList<Integer>();
    	List<Edge<Integer,Route>> edges = new ArrayList<Edge<Integer,Route>>(map.getEdges(node));
    	for(Edge<Integer,Route> edge: edges) {
    		if(edge.source()==node)
    			neighbours.add(edge.target());
    		else
    			neighbours.add(edge.source());
    	}
    	return neighbours;
    }
    /**
     * Floyd Marshall Algorithm calculates the Shortest Distance Matrix from every node to every other node
     */
    public void floydMarshall() {
    	//Initialize the matrix
    	for(int i=0;i<totalNodes;i++) {
    		List<Integer> neighbours = new ArrayList<Integer>(neighbours(i+1));
    		for(int j=0;j<totalNodes;j++) {
    			if(neighbours.contains(j+1))
    				distanceMatrix[i][j] = 1;
    			else if(i==j)
    				distanceMatrix[i][j] = 0;
    			else
    				distanceMatrix[i][j] = INF;
    		}
    	}
    	//Iterating through intermediate vertex(k)
    	for(int k=0;k<totalNodes;k++) {
    		//Iterating through source vertex(i)
    		for(int i=0;i<totalNodes;i++) {
    			//Iterating through destination vertex(j)
    			for(int j=0;j<totalNodes;j++) {
    				if(distanceMatrix[i][k]+distanceMatrix[k][j] < distanceMatrix[i][j])
    					distanceMatrix[i][j] = distanceMatrix[i][k]+distanceMatrix[k][j];
    			}
    				
    		}
    	}
    	File file = new File("out.txt");
    	try {
    	file.createNewFile();
    	PrintWriter writer = new PrintWriter(file);
    	for(int i=0;i<totalNodes;i++) {
    		writer.write(Integer.toString(i+1)+"       ");
    		for(int j=0;j<totalNodes;j++) {
    			writer.write(Integer.toString(distanceMatrix[i][j])+" ");
    		}
    		writer.println("");
    	}
    	writer.flush();
    	writer.close();
    	}
    	catch(Exception e) { System.out.println("Inexistent file");}
    }
    /**
     * 
     * @param model Model whose total distance is to be calculated
     * @return The total distance of detectives from Mr. X
     */
    public int totalDistance(Model model) {
    	int childDistance = 0;
    	for(Colour colour: view.getPlayers()) {
        	if(!colour.equals(Colour.Black)) {
        		
        		childDistance += distanceMatrix[model.getPlayer(Colour.Black).location-1][model.getPlayer(colour).location-1];
        	}
        }
    	return childDistance;
    }
    /**
     * 
     * @param model Model whose minimum distance is to be calculated
     * @return the distance of the detective who is closest to Mr. X
     */
    public int minDistance(Model model) {
    	int minDist = 100;
    	 for(Colour colour: view.getPlayers()) {
         	if(!colour.equals(Colour.Black)) {
         
         		if(distanceMatrix[model.getPlayer(Colour.Black).location-1][model.getPlayer(colour).location-1] < minDist)
         			minDist = distanceMatrix[model.getPlayer(Colour.Black).location-1][model.getPlayer(colour).location-1];
         	}
         }
    	 return minDist;
    }
    public int distance(Model model,Colour colour) {
    	return distanceMatrix[model.getPlayer(Colour.Black).location-1][model.getPlayer(colour).location-1];
    }
    public boolean check(List<Integer> list,Move move) {
    	if(move instanceof MoveTicket) {
    		if(list.contains(((MoveTicket) move).target)) {
    			
    			return true;
    		}
    		else {
    			list.add(((MoveTicket) move).target);
    			return false;
    		}
    	}
    	return false;
    }
    /**
     * Computes the nodes of all Min levels in Alpha-Beta Pruning and Scouting
     * @param model Parent node in form of Model
     * @param currentDepth The depth that is being searched
     * @param alpha Lower bound of Parent Node
     * @return Worst score for that level (since it is Min)
     */
    public int minMove(Model model,int currentDepth,int alpha) {
    	int bestScore = 90000;
    	int score = 0;
    	int parentDistance = 0;
    	int childDistance = 0;
    	
    	
    	parentDistance = totalDistance(model);
    	destinationBlue.clear();
    	destinationGreen.clear();
    	destinationRed.clear();
    	destinationWhite.clear();
    	destinationYellow.clear();
    	    	
    	/* Iterate through all combinations of moves for Blue
    	 * 
    	 */
    	for(Move move1: removeDuplicates(model.validMoves(Colour.Blue))) {
    		simulatedPlayers.clear();
    		initialiseModel(model);
    		Model testModel = new Model(view,simulatedPlayers,graphFilename,view.getRounds(),model.roundCounter);
    		modifyModel(testModel,move1);
    		if(distance(model,Colour.Blue)-distance(testModel,Colour.Blue) < 0 || check(destinationBlue,move1)) {
    			System.out.println("Break off Blue");
    			continue;
    		}
    		/* Iterate through all combinations of moves for Green
    		 * 
    		 */
    		for(Move move2: removeDuplicates(testModel.validMoves(Colour.Green))) {
    			modifyModel(testModel,move2);
    			if(distance(model,Colour.Green)-distance(testModel,Colour.Green) < 0 || check(destinationGreen,move2)) {
    				System.out.println("Break off Green");
        			continue;
        		}
    			/* Iterate through all combinations of moves for Red
    			 * 
    			 */
    			for(Move move3: removeDuplicates(testModel.validMoves(Colour.Red))) {
        			modifyModel(testModel,move3);
        			if(distance(model,Colour.Red)-distance(testModel,Colour.Red) < 0 || check(destinationRed,move3)) {
            			
            			continue;
            		}
        			/* Iterate through all combinations of moves for White
        			 * 
        			 */
        			for(Move move4: removeDuplicates(testModel.validMoves(Colour.White))) {
            			modifyModel(testModel,move4);
            			if(distance(model,Colour.White)-distance(testModel,Colour.White) < 0 || check(destinationWhite,move4)) {
                			
                			continue;
                		}
            			/* Iterate through all combinations of moves for Yellow
            			 * 
            			 */
            			for(Move move5: removeDuplicates(testModel.validMoves(Colour.Yellow))) {
            				simulatedPlayers.clear();
            	    		initialiseModel(model);
            	    		Model testModelReal = new Model(view,simulatedPlayers,graphFilename,view.getRounds(),model.roundCounter);
            	    		if(timerIndex == false)
            	    			return -1;
        					
            				modifyModel(testModelReal,move1);
            				modifyModel(testModelReal,move2);
            				modifyModel(testModelReal,move3);
            				modifyModel(testModelReal,move4);
            				modifyModel(testModelReal,move5);
            				if(distance(model,Colour.Yellow)-distance(testModelReal,Colour.Yellow) < 0 || check(destinationYellow,move5)) {
                    		
                    			continue;
                    		}
            				childDistance = 0;
            				childDistance = totalDistance(testModelReal);
            				/**
            				 * SCOUTING ALGORITHM IMPLEMENTATION
            				 * Predict value of a node beforehand to decide to proceed further or stop
            				 */
            				// Stop if Game is Over
            				if(testModelReal.isGameOver()) {
            	    			if(testModelReal.winnerMrX)
            	    				score = 100000;
            	    			else {
            	    				System.out.println("Game Over DET");
            	    				return 0;
            	    			}
            	    		}
            				else if(currentDepth == depth){
            					
            	        		score = score(testModelReal.validMoves(Colour.Black),testModelReal,childDistance);
            	        		indi++;
            	        		System.out.println(indi+".    Score: "+score+"   Depth: "+currentDepth);
            	        		
            	        	}
            	    		else if(parentDistance - childDistance >= 4) {
            	    			score = maxMove(testModelReal,currentDepth+1,model.beta);
            	    			System.out.println("Score in else if(2): "+score);
            	    			
            	    		}
            	    		else {
            	    			score = score(testModelReal.validMoves(Colour.Black),testModelReal,childDistance);
            	    			indi++;
            	    			System.out.println("Total models in 2nd level evaluated (else): "+indi);
            	    			
            	    		}
            			
            				if(score < model.beta && (score > alpha) ) {
        	        			model.beta = score;
        	        			//System.out.println("Beta Set to :"+model.beta+" Score:"+score);
            				}
        	        		else if(score<alpha) {
        	        			model.beta = score;
        	        			System.out.println("Skipped DET");
        	        			return score;
        	        		}
        	        		else
        	        			;
        	        		
            	    		
            	    		if(score < bestScore) {
            	    			bestScore = score;
            	    		}
            			}
        			}
    			}
    		}
    	}
    	return bestScore;
    }
    /**
     * Evaluates the nodes of all Max Levels in Alpha-Beta Pruning and Scouting
     * @param model Parent node in form of Model
     * @param currentDepth The current depth being explored
     * @param beta The Upper Bound for the Parent Node
     * @return The Best Score for that level (since it is Max)
     */
   
    public int maxMove(Model model,int currentDepth,int beta) {
    	
    	int bestScore = 0;
    	int score = 0;
    	int secret = 0;
    	int parentDistance = 0;
    	int childDistance = 0;
    	int flag = 0;
    	parentDistance = totalDistance(model);  
    	System.out.println("MIN DISTANCE: "+currentDepth+" : "+minDistance(model));
    	System.out.println("TOTAL DISTANCE: "+parentDistance);
    	for(Colour colour: view.getPlayers()) {
    		System.out.println(colour.toString()+":   "+model.getPlayerLocation(colour));
    	}
    	destinations.clear();
    	/* Use Double and Secret Moves only in danger situations
    	 * 
    	 */
    	if(!(parentDistance <= 12 || minDistance(model) == 1)) {
    		System.out.println("Double and Secret Move Skipped");
    		flag = 1;
    	}
    	/* Iterate through all the moves for Mr. X
    	 * 
    	 */
    	List<Move> moves;
    	/*if(currentDepth == 1)
    		moves = new ArrayList<Move>(orderedMoves);
    	else*/
    		moves = new ArrayList<Move>(removeDuplicates(model.validMoves(Colour.Black)));
    	for(Move move : removeDuplicates(model.validMoves(Colour.Black))) {
    		System.out.println("ENTERED");
    		if(timerIndex == false) {
    			System.out.println("Finished");
    			return 0;
    		}
    		simulatedPlayers.clear();
    		initialiseModel(model);
    		Model testModel = new Model(view,simulatedPlayers,graphFilename,view.getRounds(),model.roundCounter);
    		modifyModel(testModel,move);
    		childDistance = 0;
    		childDistance = totalDistance(testModel);
    		if(move instanceof MoveTicket) {
    			if(((MoveTicket) move).ticket.equals(Ticket.Secret)) {
    				if(flag == 1) {
    					secretCounter++;
    					continue;
    				}
    				else if(view.getRounds().get(view.getRound()+1))
    					continue;
    			}	
    		}
    		else {
    			if(flag == 1) {
    				secretCounter++;
    				continue;
    			}
    			else if(((MoveDouble) move).move1.ticket.equals(Ticket.Secret) || ((MoveDouble) move).move2.ticket.equals(Ticket.Secret)) {
    				if(view.getRounds().get(view.getRound()+1))
        				continue;
    			}
    		}
    		if(move instanceof MoveTicket) {
    			if(!((MoveTicket) move).ticket.equals(Ticket.Secret)) {
    				if(destinations.contains(((MoveTicket) move).target)) {
    					continue;
    				}
    				else
    					destinations.add(((MoveTicket) move).target);
    			}
    			else
    				secret = 1;
    		}
    		else {
    			if(!(((MoveDouble) move).move1.ticket.equals(Ticket.Secret) || ((MoveDouble) move).move2.ticket.equals(Ticket.Secret))) {
    				if(destinations.contains(((MoveDouble) move).move2.target)) {
    					continue;
    				}
    				else
    					destinations.add(((MoveDouble) move).move2.target);
    			}
    			else
    				secret = 1;
    		}
    		//System.out.println("Max Move: "+currentDepth+"    "+move.toString());
    		
    		/**
    		 * SCOUTING ALGORITHM IMPLEMENTATION
             * Predict value of a node beforehand to decide to proceed further or stop
    		 */
    		if(testModel.isGameOver()) {
    			if(testModel.winnerMrX)
    				return 100000;
    			else {
    				score = 0;
    				System.out.println("Game Over MAX");
    			}
    		}
    		else if(currentDepth == depth){
        		score = score(removeDuplicates(testModel.validMoves(Colour.Black)),testModel,childDistance);
        		i3++;
        	}
    		else if(childDistance - parentDistance >= 0) {
    			score = minMove(testModel,currentDepth+1,model.alpha);
    			if(score == -1)
    				return 0;
    		}
    		else {
    		
    			score = score(removeDuplicates(testModel.validMoves(Colour.Black)),testModel,childDistance);
        		i++;    		
    		}
    		if(secret == 1) {
    			score += 200;
    			secret = 0;
    		}
    		
    		if(score>model.alpha && (score < beta))
    			model.alpha = score;
			else if(score > beta) {
    			model.alpha = score;
    			System.out.println("Skipped");
    			return score;
    		}	
			else
				;
    		
    		if(score > bestScore) {
    			bestScore = score;
    			if(currentDepth == 1) {
    				System.out.println("Best score: "+score+"   Move:"+move.toString());
    				
    				signal = 1;
    				if(move instanceof MoveDouble)
    					selectMove = MoveDouble.instance(Colour.Black,((MoveDouble) move).move1,((MoveDouble) move).move2);
    				else if(move instanceof MoveTicket)
    					selectMove = MoveTicket.instance(Colour.Black,((MoveTicket) move).ticket,((MoveTicket) move).target);
    				else
    					;
    			}
    		}
    	}
    		
    	return bestScore;
    }
    @Override
    public Move notify(int location, Set<Move> moves) {
        //TODO: Some clever AI here ...
    	signal = 0;
    	secretCounter = 0;
    	
    	Timer timer = new Timer();
        timerIndex = true;
        timer.schedule(new TimerMove(), 12 * 1000);
    	
    	List<Move> move = new ArrayList<Move>(moves);
    	
    	System.out.println("Size : "+moves.size());
    	simulatedPlayers.clear();
    	initialise(location);
    	System.out.println("before this ");
    	Model root = new Model(view,simulatedPlayers,graphFilename,view.getRounds(),view.getRound());
    	
    	if(view.getRound() < 5)
    		depth = 1;
    	else if(view.getRound() < 10)
    		depth = 7;
    	else
    		depth = 9;
    	maxMove(root,1,root.beta);
    	System.out.println("Secret and Double Moves skipped: "+secretCounter);
    	timer.cancel();
    	if((timerIndex == false) && (signal == 1)){
    		/*
    		Runnable a = new ProcedureA(root,selectMove);
    		Thread ta = new Thread(a);
    		ta.start();
    		*/
    		System.out.println("Best move at this time");
    		return selectMove;
    	}
    	else if(timerIndex == false) {
    		int choice = new Random().nextInt(move.size());
    		/*
    		Runnable a = new ProcedureA(root,move.get(choice));
    		Thread ta = new Thread(a);
    		ta.start();
    		*/
    		return move.get(choice);
    	}
    	/*
    	Runnable a = new ProcedureA(root,selectMove);
		Thread ta = new Thread(a);
		ta.start();
		*/
    	System.out.println("AI Move " +selectMove.toString());
    	int choice = new Random().nextInt(move.size());
    	selectMove = move.get(choice);
        return selectMove;
    }
    public List<Move> removeDuplicates(List<Move> list) {
        // Store unique items in result.
        List<Move> result = new ArrayList<>();
        // Record encountered Strings in HashSet.
        HashSet<Move> set = new HashSet<>();
       // int taxi = 0, underground = 0, bus = 0, doubleIndex = 0;
        // Loop over argument list.
        for (Move item : list) {
            // If String is not in set, add it to the list and the set.
        	if (!set.contains(item)) {
        		result.add(item);
        		set.add(item);
        	    }
        }
       
        return result;
        }
 
}

