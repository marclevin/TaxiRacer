package game.logic;

import java.util.ArrayList;
import java.util.Random;

import game.display.models.Passenger;
import game.utility.EPassenger;
import game.utility.ESettings;
import javafx.scene.image.Image;
import javafx.util.Pair;

/**
 * This class is the object pool for the passengers.
 */
public class PassengerPool {
    private static PassengerPool instance = null;
    private static Random rand = null;
    private static Image passengerImage = null;
    private static ArrayList<Passenger> passengers;
    private static ArrayList<Pair<Integer, EPassenger>> passenger_locations;

    /**
     * Private constructor to ensure only one instance exists
     */
    private PassengerPool() {
        passengers = new ArrayList<Passenger>();
        rand = new Random();
    }

    /**
     * This function returns the instance of the passenger pool.
     * @return The instance of the passenger pool
     */
    public static PassengerPool getInstance() {
        if (instance == null) {
            instance = new PassengerPool();
            passenger_locations = new ArrayList<Pair<Integer, EPassenger>>();
        }
        return instance;
    }

    /**
     * This function sets the image of passengers created in the pool.
     * @param image The image of the passengers.
     */
    public static void setPassengerImage(Image image) {
        passengerImage = image;
    }

      /**
     * This function gets a random number
     * @param min The minimum number (inclusive)
     * @param max The maximum number (inclusive)
     * @return a random number between min and max
     */
    private static int getRandomNumber(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

    /**
     * This function gets a random X position for a passenger.
     * @return A random X position for a passenger.
     */
    private static int rand_x()
    {
       return getRandomNumber(-ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_WIDTH.getVal());
    }

    /**
     * This function creates a passenger at a random location or releases one from the pool but randomizes it's location
     * @return The passenger requested.
     */
    public Passenger aquirePassenger() {
        Passenger p = null;
        EPassenger passenger_location = null;
        if (passengers.isEmpty()) {
            passenger_location = rand.nextBoolean() ? EPassenger.PASSENGER_BOTTOM : EPassenger.PASSENGER_TOP;
            Pair<Integer,EPassenger> potential_location = new Pair<Integer,EPassenger>(rand_x(), passenger_location);
            while (passenger_locations.contains(potential_location) ) {
                potential_location = new Pair<Integer,EPassenger>(rand_x(), passenger_location);
            }
            passenger_locations.add(potential_location);
            p = new Passenger(potential_location.getKey(), 0);
            p.setImage(passengerImage);
            p.scale(passenger_location);
        } else {
            p = passengers.get(passengers.size() - 1);
            p.setX(rand_x());
            passengers.remove(passengers.size() - 1);
        }
        return p;
    }

    /**
     * Releases a passenger back to the pool.
     * @param p The passenger to be released.
     */
    public void releasePassenger(Passenger p) {
        if (p != null) {
            passengers.add(p);
            // Change their cash value.
            p.setCash();
            passenger_locations.remove(new Pair<Integer,EPassenger>(p.getX(), p.getEPassenger()));
        }
    }
}
