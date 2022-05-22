package game.logic;

import java.util.ArrayList;
import java.util.Random;

import game.display.models.Passenger;
import game.utility.EPassenger;
import game.utility.ESettings;
import javafx.scene.image.Image;
import javafx.util.Pair;

public class PassengerPool {
    private static PassengerPool instance = null;
    private static Random rand = null;
    private static Image passengerImage = null;
    private static ArrayList<Passenger> passengers;
    private static ArrayList<Pair<Integer, EPassenger>> passenger_locations;

    private PassengerPool() {
        passengers = new ArrayList<Passenger>();
        rand = new Random();
    }

    public static PassengerPool getInstance() {
        if (instance == null) {
            instance = new PassengerPool();
            passenger_locations = new ArrayList<Pair<Integer, EPassenger>>();
        }
        return instance;
    }

    public static void setPassengerImage(Image image) {
        passengerImage = image;
    }

    private static int rand_x()
    {
       return rand.nextInt(-ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_WIDTH.getVal()+1);
    }

    public Passenger aquirePassenger() {
        Passenger p = null;
        EPassenger passenger_location = null;
        if (passengers.isEmpty()) {
            passenger_location = rand.nextBoolean() ? EPassenger.PASSENGER_BOTTOM : EPassenger.PASSENGER_TOP;
            Pair<Integer,EPassenger> potential_location = new Pair<Integer,EPassenger>(rand_x(), passenger_location);
            while (passenger_locations.contains(potential_location)) {
                potential_location = new Pair<Integer,EPassenger>(rand_x(), passenger_location);
            }
            passenger_locations.add(potential_location);
            p = new Passenger(potential_location.getKey(), 0, passengerImage);
            p.scale(passenger_location);
        } else {
            p = passengers.get(passengers.size() - 1);
            p.setX(rand_x());
            passengers.remove(passengers.size() - 1);
        }
        return p;
    }

    public void releasePassenger(Passenger p) {
        if (p != null) {
            passengers.add(p);
            // Change their cash value.
            p.setCash();
            passenger_locations.remove(new Pair<Integer,EPassenger>(p.getX(), p.getEPassenger()));
        }
    }
}
