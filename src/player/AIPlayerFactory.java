package player;

import gui.Gui;
import net.PlayerFactory;
import scotlandyard.Colour;
import scotlandyard.Player;
import scotlandyard.ScotlandYardView;
import scotlandyard.Spectator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The AIPlayerFactory is an example of a PlayerFactory that
 * creates a series of random players. By default it assigns
 * an AI to the colour Black, while rest of the colours 
 * are controlled by the GUI.
 */
public class AIPlayerFactory implements PlayerFactory {
    protected Map<Colour, PlayerType> typeMap;

    public enum PlayerType {AI, GUI}

    String imageFilename;
    String positionsFilename;

    protected List<Spectator> spectators;
    Gui gui;

    public AIPlayerFactory() {
        typeMap = new HashMap<Colour, PlayerType>();
        typeMap.put(Colour.Black, AIPlayerFactory.PlayerType.AI);
        typeMap.put(Colour.Blue, AIPlayerFactory.PlayerType.GUI);
        typeMap.put(Colour.Green, AIPlayerFactory.PlayerType.GUI);
        typeMap.put(Colour.Red, AIPlayerFactory.PlayerType.GUI);
        typeMap.put(Colour.White, AIPlayerFactory.PlayerType.GUI);
        typeMap.put(Colour.Yellow, AIPlayerFactory.PlayerType.GUI);

        positionsFilename = "resources/pos.txt";
        imageFilename     = "resources/map.jpg";

        spectators = new ArrayList<Spectator>();
    }

    public AIPlayerFactory(Map<Colour, PlayerType> typeMap, String imageFilename, String positionsFilename) {
        this.typeMap = typeMap;
        this.imageFilename = imageFilename;
        this.positionsFilename = positionsFilename;
        spectators = new ArrayList<Spectator>();
    }

    @Override
    public Player player(Colour colour, ScotlandYardView view, String mapFilename) {
        switch (typeMap.get(colour)) {
            case AI:
                return new MyAIPlayer(view, mapFilename);
            case GUI:
                return gui(view);
            default:
                return new MyAIPlayer(view, mapFilename);
        }
    }

    @Override
    public void ready() {
        if (gui != null) gui.run();
    }

    @Override
    public List<Spectator> getSpectators(ScotlandYardView view) {
        List<Spectator> specs = new ArrayList<Spectator>();
        specs.add(gui(view));
        return specs;
    }

    @Override
    public void finish() {
        if (gui != null) gui.update();
    }


    private Gui gui(ScotlandYardView view) {
        System.out.println("GUI");
        if (gui == null) try {
            gui = new Gui(view, imageFilename, positionsFilename);
            spectators.add(gui);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return gui;
    }
}

