package player;

import java.util.Map;

import scotlandyard.Colour;
import scotlandyard.Player;
import scotlandyard.Ticket;

public class RealPlayer {
	public Colour colour;
	public Player player;
	public int location;
	public Map<Ticket, Integer> tickets;
	RealPlayer(Colour colour, int location, Map<Ticket, Integer> tickets) {
		this.colour = colour;
		this.location = location;
		this.tickets = tickets;
		this.player = player;
		 
	}
}
