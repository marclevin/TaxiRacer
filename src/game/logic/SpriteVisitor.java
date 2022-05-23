package game.logic;

import java.util.ArrayList;

import game.display.models.Passenger;
import game.display.models.Police;
import game.display.models.Pothole;
import game.display.models.Road;
import game.utility.EPassenger;

/**
 * This class is the visitor for the visitor design pattern
 */
public class SpriteVisitor implements Visitor
{
    private ArrayList<Passenger> bottom_render_list;
    private ArrayList<Passenger> clean_list;
    // This class will be used to help us detect collisions by visiting them

    /**
     * Constructor for the sprite visitor.
     */
    public SpriteVisitor()
    {
        bottom_render_list = new ArrayList<Passenger>();
        clean_list = new ArrayList<Passenger>();
    }

    /**
     * This function will return a list of passengers that are on the bottom of the screen.
     * @return The list of passengers that are on the bottom of the screen.
     */
    public ArrayList<Passenger> getBottomRenderList()
    {
        return bottom_render_list;
    }

    /**
     * This function will return a list of passengers that are to be cleaned from the screen
     * @return The list of passengers that are to be cleaned from the screen
     */
    public ArrayList<Passenger> getCleanList()
    {
        return clean_list;
    }

    /**
     * Visitor design pattern implementation of Pothole vistation.
     */
    @Override
    public void visit(Pothole pothole) {
        // When you touch a pothole, add time to the taxi's slowdown.
        // Pothole resistance will change the time to add.
        InputHandler.getTaxi().addPunishment(4 - InputHandler.getTaxi().getPotholeResistance());
    }

    /**
     * Visitor design pattern implementation of Passenger vistation.
     */
    @Override
    public void visit(Passenger passenger) {
        if (passenger.getEPassenger() == EPassenger.PASSENGER_BOTTOM && !bottom_render_list.contains(passenger)) {
            bottom_render_list.add(passenger);
    }
    if (InputHandler.pickupAttempted() && !clean_list.contains(passenger))
    {
    clean_list.add(passenger);
    InputHandler.getTaxi().addPunishment(20);
    InputHandler.pickupBlock();
    }
}

/**
 * Visitor design pattern implementation of Police vistation.
 */
    @Override
    public void visit(Road road) {
        // This doesn't need to do anything, but it's here for completeness
        return;
        
        
    }

    /**
     * Visitor design pattern implementation of Police vistation.
     */
    @Override
    public void visit(Police police) {
        // This doesn't need to do anything, but it's here for completeness
        return;
        
    }


}