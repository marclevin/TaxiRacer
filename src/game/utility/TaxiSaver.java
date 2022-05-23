package game.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import game.display.models.Taxi;

/**
 * This class is responsible for saving and loading the game (effectively a taxi instance).
 */
public final class TaxiSaver {
    private static File target = null;

    /**
     * Private constructor.
     */
    private TaxiSaver()
    {
    }

    /**
     * This function saves the provided @param taxi to the target file.
     * @param taxi The taxi to be saved.
     */
    public static void save(Taxi taxi)
    {
        if (taxi == null) return;
        try (FileOutputStream fos = new FileOutputStream(target);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(bos))
        {
        oos.writeDouble(taxi.getWallet());
        oos.writeInt(taxi.getCareerPassengers());
        oos.writeInt(taxi.getPotholeResistance());
        oos.writeInt(taxi.getEngineUpgrade());
        oos.writeBoolean(taxi.hasNOS());
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        
    }

    /**
     * This function loads the taxi from the target file.
     * @return The loaded taxi.
     */
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
            int potholeResistance = ois.readInt();
            int engineUpgrade = ois.readInt();
            boolean hasNOS = ois.readBoolean();
             taxi = new Taxi(0, 0);
            taxi.addCash(wallet);
            taxi.setCareerPassengers(career_passengers-1); //  Minus one because the addCash method adds a passenger
            taxi.setPotHoleResistance(potholeResistance);
            taxi.setEngineUpgrade(engineUpgrade);
            taxi.setNOS(hasNOS);
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        
        return taxi;
    }


    /**
     * The target file to save the taxi to.
     * @param f The file.
     */
    public static void setFile(File f)
    {
    target = f;
    }
    
}
