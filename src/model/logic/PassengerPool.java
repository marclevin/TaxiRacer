package model.logic;

import java.util.ArrayList;
import java.util.Random;

import javafx.scene.image.Image;
import model.utility.EPassenger;

public class PassengerPool {
    private static PassengerPool instance = null;
    private static Random rand = null;
    private static Image passengerImage = null;
    private static ArrayList<Passenger> passengers;

    private PassengerPool() {
        passengers = new ArrayList<Passenger>();
        rand = new Random();
    }

    public static PassengerPool getInstance() {
        if (instance == null) {
            instance = new PassengerPool();
        }
        return instance;
    }

    public static void setPassengerImage(Image image) {
        passengerImage = image;
    }

    public Passenger aquirePassenger() {
        Passenger p = null;
        EPassenger passenger_location = null;
        if (passengers.isEmpty()) {
            passenger_location = rand.nextBoolean() ? EPassenger.PASSENGER_BOTTOM : EPassenger.PASSENGER_TOP;
            p = new Passenger(rand.nextInt(-1080, 1081), 0, passengerImage);
            p.scale(passenger_location);
        } else {
            p = passengers.get(passengers.size() - 1);
            passengers.remove(passengers.size() - 1);
        }
        return p;
    }

    public void releasePassenger(Passenger p) {
        if (p != null) {
            passengers.add(p);
        }
    }
}
