package model.logic;

import java.util.ArrayList;

public class PassengerPool {
    private static PassengerPool instance = null;
    private ArrayList<Passenger> passengers;
    private PassengerPool()
    {
        passengers = new ArrayList<Passenger>();
    }
    public static PassengerPool getInstance()
    {
        if(instance == null)
        {
            instance = new PassengerPool();
        }
        return instance;
    }
    public Passenger aquirePassenger()
    {
        Passenger p = null;
        if(passengers.isEmpty())
        { 
        return p;
        } else
        {
            p = passengers.get(passengers.size() - 1);
            passengers.remove(passengers.size() - 1);
        }
        return p;
    }

    public void releasePassenger(Passenger p)
    {
        if (p != null)
        {
            passengers.add(p);
        }
    }
}
