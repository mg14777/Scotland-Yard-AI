package player;
import java.util.*;

import scotlandyard.*;




public class Model {
	//int mrxLocation;
	ScotlandYardView view;
	//Map<Ticket,Integer> mrxTickets;
	private List<RealPlayer> players = new ArrayList<RealPlayer>();
	private Graph map;
	 private List<Boolean> rounds;
	 public Boolean winnerMrX;  
	 public int roundCounter = 0;
	 private int x = 0;          //current player index
	 public int alpha = -100000;
	 public int beta = 100000;
	 private int numberOfDetectives;
	Model(ScotlandYardView view,List<RealPlayer> players,String graphFileName,List<Boolean> rounds,int roundCounter) {
		this.view = view;
		this.roundCounter = roundCounter;
		this.players.addAll(players);
		this.rounds = rounds;
		this.numberOfDetectives = players.size()-1;
		ScotlandYardGraphReader read = new ScotlandYardGraphReader();
		try {map = read.readGraph(graphFileName);}
        catch(Exception e) { System.out.println("Inexistent file");}
	}
	
	
	protected boolean check(Colour colour,int target) {
        for(RealPlayer player : players) {
            if(!player.colour.equals(colour))
                if((player.location ==  target) && (!player.colour.equals(Colour.Black))) {
                    return false;
                }
        }
        return true;
    }
	
	/**
     * 
     * @param colour Colour of Player
     * @param target1 Location for Move 1
     * @param target2 Location for Move 2
     * @param route1  Transport for Move 1
     * @param route2  Transport for Move 2
     * @param secretmoves No. of Secret Move Tickets
     * @return Creates double secret moves for MrX
     */
    protected List<Move> doublesecretMoves (Colour colour, int target1, int target2,
     Edge<Integer,Route> route1, Edge<Integer,Route> route2, int secretmoves) {
        MoveTicket move1 = MoveTicket.instance(colour,Ticket.fromRoute(route1.data()),target1);
        MoveTicket move2 = MoveTicket.instance(colour,Ticket.fromRoute(route2.data()),target2);
        MoveTicket move1secret = MoveTicket.instance(colour,Ticket.Secret,target1);
        MoveTicket move2secret = MoveTicket.instance(colour,Ticket.Secret,target2);
        List<Move> doublesecretmoves = new ArrayList<Move>();
        if(secretmoves!=0) {
            doublesecretmoves.add(MoveDouble.instance(colour,move1secret,move2));
            doublesecretmoves.add(MoveDouble.instance(colour,move1,move2secret));
        }
        if(secretmoves>1)
            doublesecretmoves.add(MoveDouble.instance(colour,move1secret,move2secret));
        return doublesecretmoves;
    }
    /**
     * 
     * @param colour Colour of Player
     * @param target Target location of Move
     * @param route1 Transport of Move
     * @param secretmoves No. of Secret Move Tickets
     * @return All the possible double moves for MrX
     */
    protected List<Move> doublevalidMoves(Colour colour, int target,
     Edge<Integer,Route> route1, int secretmoves) {
        List<Edge<Integer, Route>> routes2 = new ArrayList<Edge<Integer,Route>>(map.getEdgesFrom(target));
        List<Move> doublemoves = new ArrayList<Move>();
        for(Edge<Integer,Route> route2 : routes2 ) {
            MoveTicket move1 = MoveTicket.instance(colour,Ticket.fromRoute(route1.data()),target);
            if(route1.data().equals(route2.data())) {
                if(getPlayerTickets(colour,Ticket.fromRoute(route2.data())) > 1) {
                    if((route2.target()).equals(target)) {
                        MoveTicket move2 = MoveTicket.instance(colour, Ticket.fromRoute(route2.data()), route2.source());
                        if(check(colour,route2.source())) {
                            doublemoves.add(MoveDouble.instance(colour, move1, move2));
                            doublemoves.addAll(doublesecretMoves(colour, target, route2.source(), route1, route2, secretmoves));
                        }
                    }
                    else {
                        MoveTicket move2 = MoveTicket.instance(colour,Ticket.fromRoute(route2.data()),route2.target());
                        if(check(colour,route2.target())) {
                            doublemoves.add(MoveDouble.instance(colour,move1,move2));
                            doublemoves.addAll(doublesecretMoves(colour, target, route2.target(), route1, route2, secretmoves));
                        }
                    }
                }
            }
            else {
                if(getPlayerTickets(colour, Ticket.fromRoute(route2.data())) != 0) {
                    if((route2.target()).equals(target)) {
                        MoveTicket move2 = MoveTicket.instance(colour,Ticket.fromRoute(route2.data()), route2.source());
                        if(check(colour, route2.source())) {
                            doublemoves.add(MoveDouble.instance(colour,move1,move2));
                            doublemoves.addAll(doublesecretMoves(colour,target,route2.source(), route1, route2, secretmoves));
                        }
                    }
                    else {
                        MoveTicket move2 = MoveTicket.instance(colour,Ticket.fromRoute(route2.data()),route2.target());
                        if(check(colour, route2.target())) {
                            doublemoves.add(MoveDouble.instance(colour, move1, move2));
                            doublemoves.addAll(doublesecretMoves(colour, target, route2.target(), route1, route2, secretmoves));
                        }
                    }
                }
            }
        }
        return doublemoves;
    }
    
   
    public List<Move> validMoves(Colour player) {
    	 
        int location = getPlayer(player).location;
        
        int flag = 0;
        if(player.equals(Colour.Black))
            flag = 1;
        List<Edge<Integer, Route>> routes = new ArrayList<Edge<Integer,Route>>(map.getEdgesFrom(location));
        List<Move> moves = new ArrayList<Move>();
        int doublemovetickets = getPlayerTickets(player,Ticket.Double);
        int secretmovetickets = getPlayerTickets(player,Ticket.Secret);
        
        for(Edge<Integer,Route> route : routes){
            if(getPlayerTickets(player,Ticket.fromRoute(route.data())) != 0){
                if((route.target()).equals(location)) {
                    if(check(player,route.source())) {
                        moves.add(MoveTicket.instance(player,Ticket.fromRoute(route.data()), route.source()));
                        if(flag==1 && secretmovetickets!=0)
                            moves.add(MoveTicket.instance(player,Ticket.Secret, route.source()));
                    } 
                    if(flag==1 && check(player,route.source()) && doublemovetickets!=0)
                        moves.addAll(doublevalidMoves(player, route.source(), route, secretmovetickets));
                }
                else {
                    if(check(player,route.target())) {
                        moves.add(MoveTicket.instance(player,Ticket.fromRoute(route.data()),route.target()));
                        if(flag==1 && secretmovetickets!=0)
                            moves.add(MoveTicket.instance(player,Ticket.Secret,route.target()));
                    }
                    if(flag==1 && check(player,route.target()) && doublemovetickets!=0)
                        moves.addAll(doublevalidMoves(player, route.target(), route, secretmovetickets));
                }
            }
        }
        if(moves.size() == 0&&!player.equals(Colour.Black))
            moves.add(MovePass.instance(player));
        return moves;
    }
    public int getPlayerLocation(Colour colour) {
            for(RealPlayer player: players)
                if(player.colour.equals(colour))
                    return player.location;
            return 0;
    }
    /**
     * 
     * @param colour Colour of the Player to be returned
     * @return Specified player on basis of Colour
     */
    public RealPlayer getPlayer(Colour colour) {
        for(RealPlayer player: players)
            if(player.colour.equals(colour))
                return player;
        return null;
    }
    public List<RealPlayer> getPlayers() {
    	return players;
    }
    public int getPlayerTickets(Colour colour, Ticket ticket) {
        for(RealPlayer player: players)
            if(player.colour.equals(colour))
                return player.tickets.get(ticket);
        return 0;
        
    }
    protected void nextPlayer() {
        x++;
    }
    /**
     * 
     * @return True if all detectives can't move
     */
    private boolean frozenDetectives() {
        int numberOfFrozenDetectives = 0;
        for(RealPlayer player: players)
            if(player.colour != Colour.Black && validMoves(player.colour).get(0) instanceof MovePass)
            	numberOfFrozenDetectives++;
        if(numberOfFrozenDetectives == numberOfDetectives)
            return true;
        else
            return false;
    }
    /**
     * 
     * @return True if Mr.X can't move
     */
    private boolean frozenMrX() {
            if(validMoves(Colour.Black).size()==0)
                return true;
            else
                return false;
    }

    public boolean isGameOver() {
        if(players.size() == 0)
            return false;
        if(frozenMrX()) {
            winnerMrX = false;
            return true;
        }
        for(RealPlayer player : players)
            if((getPlayer(Colour.Black).location == player.location) && !player.colour.equals(Colour.Black)) {
                winnerMrX = false;
                return true;
            }    
        if((rounds.size()==roundCounter)|| frozenDetectives()||(rounds.size()==(roundCounter+1)&&x==numberOfDetectives+1)) {
            winnerMrX = true;
            return true;
        }
        return false;
    }
	
	
	
	
}
