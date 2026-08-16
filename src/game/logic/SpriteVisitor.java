package game.logic;

import java.util.ArrayList;

import game.display.models.Jaywalker;
import game.display.models.Passenger;
import game.display.models.Police;
import game.display.models.Pothole;
import game.display.models.Road;
import game.display.models.Taxi;
import game.utility.EPassenger;

/**
 * This class is the visitor for the visitor design pattern
 */
public class SpriteVisitor implements Visitor
{
    /** Frames of slowdown one pothole costs on bare wheels. */
    private static final int POTHOLE_PUNISHMENT = 30;
    /** Frames of that cost each wheel upgrade removes; a NOS boost cancels it outright. */
    private static final int POTHOLE_RESISTANCE_STEP = 8;
    /** Frames of slowdown for pulling over to load a fare. */
    private static final int PICKUP_PUNISHMENT = 20;

    private final ArrayList<Passenger> bottom_render_list;
    private final ArrayList<Passenger> clean_list;
    private final ArrayList<Jaywalker> splat_list;
    private final ArrayList<Pothole> pothole_hits;
    /** Cleared while the taxi is pulled over, so SPACE only ever means "let them out". */
    private boolean pickups_enabled = true;
    // This class will be used to help us detect collisions by visiting them

    /**
     * Constructor for the sprite visitor.
     */
    public SpriteVisitor()
    {
        bottom_render_list = new ArrayList<Passenger>();
        clean_list = new ArrayList<Passenger>();
        splat_list = new ArrayList<Jaywalker>();
        pothole_hits = new ArrayList<Pothole>();
    }

    /**
     * Turns fare pickups on and off.
     *
     * <p>
     * Switched off during a drop-off stop. Otherwise a press meant to shove someone out of
     * the taxi could be claimed by a fare standing next to it, quietly extending the stop
     * the player is trying to end.
     * </p>
     *
     * @param enabled whether waiting fares may be picked up
     */
    public void setPickupsEnabled(boolean enabled)
    {
        this.pickups_enabled = enabled;
    }

    /**
     * Returns the potholes struck since this list was last drained.
     *
     * @return the list of fresh pothole hits.
     */
    public ArrayList<Pothole> getPotholeHits()
    {
        return pothole_hits;
    }

    /**
     * Returns the pedestrians run down since this list was last drained.
     *
     * @return the list of freshly hit pedestrians.
     */
    public ArrayList<Jaywalker> getSplatList()
    {
        return splat_list;
    }

    /**
     * Visitor design pattern implementation of Jaywalker visitation.
     */
    @Override
    public void visit(Jaywalker jaywalker) {
        // splat() returns false if they were already down, so one body is only counted once.
        if (jaywalker.splat()) {
            splat_list.add(jaywalker);
        }
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
        // One thump per pothole, however long the taxi spends on top of it.
        if (!pothole.tryHit()) {
            return;
        }
        // Better wheels shorten the slowdown, and a live NOS boost cancels it outright.
        int resistance = InputHandler.getTaxi().getEffectivePotholeResistance();
        int punishment = Math.max(0, POTHOLE_PUNISHMENT - resistance * POTHOLE_RESISTANCE_STEP);
        if (punishment > 0) {
            InputHandler.getTaxi().addPunishment(punishment);
            pothole_hits.add(pothole);
        }
    }

    /**
     * Visitor design pattern implementation of Passenger vistation.
     */
    @Override
    public void visit(Passenger passenger) {
        // A passenger on the near pavement is standing between the camera and the taxi,
        // so it has to be drawn after the taxi to look right.
        if (passenger.getEPassenger() == EPassenger.PASSENGER_BOTTOM && !bottom_render_list.contains(passenger)) {
            bottom_render_list.add(passenger);
        }

        if (!pickups_enabled || clean_list.contains(passenger)) {
            return;
        }

        // Seats already claimed this step count too, so a single step cannot overfill the
        // taxi when several fares are within reach at once.
        Taxi taxi = InputHandler.getTaxi();
        if (taxi.getOccupants() + clean_list.size() >= taxi.getCapacity()) {
            return;
        }

        // One key press loads one fare, even when several are within reach.
        if (!InputHandler.consumeSpacePress()) {
            return;
        }

        clean_list.add(passenger);
        taxi.addPunishment(PICKUP_PUNISHMENT);
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