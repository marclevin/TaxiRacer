package game.logic;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import game.display.models.Taxi;

public class TaxiSaver {
    private static File target = null;

    private TaxiSaver()
    {
    }

    public static void save(Taxi taxi)
    {
        if (taxi == null) return;
        try (FileOutputStream fos = new FileOutputStream(target);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(bos))
        {
        oos.writeDouble(taxi.getWallet());
        oos.writeInt(taxi.getCareerPassengers());
        oos.writeInt(taxi.getSlowDown());
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        
    }

    public static Taxi getTaxi()
    {
        System.out.println("Trying to read a taxi");
        Taxi taxi = null;
        try (FileInputStream fis = new FileInputStream(target);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream ois = new ObjectInputStream(bis))
        {
            double wallet = ois.readDouble();
            int career_passengers = ois.readInt();
            int slowDown = ois.readInt();
             taxi = new Taxi(0, 0);
            taxi.addCash(wallet);
            taxi.setCareerPassengers(career_passengers-1); //  Minus one because the addCash method adds a passenger
            taxi.setSlowDown(slowDown);
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        
        return taxi;
    }


    public static void setFile(File f)
    {
    target = f;
    }
    
}
