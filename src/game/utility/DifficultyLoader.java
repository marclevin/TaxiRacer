package game.utility;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * This class loads difficulty aka pothole combinations from the difficulty files located in \\dat
 */
public final class DifficultyLoader {
    private static int[] lanes;
    /**
     * Private constructor, no instance needed.
     */
    private DifficultyLoader() {
        
    }

    /**
     * Loads difficulty from a given {@code File}
     * @param f The file to load from
     */
    public static void loadDifficulty(File f)
    {
        lanes = new int[4];
        // The file is a text file containing the number of potholes per lane, set local variables to the values from the file.
        try(Scanner reader = new Scanner(f)) {
            String line;
            for(int i = 0; i < 4; i++) {
                line = reader.nextLine();
                lanes[i] = Integer.parseInt(line);
            }
        }
        catch (IOException e)
        {
            System.out.println("Error loading difficulty file.");
            e.printStackTrace();
        }
    }

    /**
     * This returns the number of potholes in a given lane.
     * @return The number of potholes in a given lane.
     */
    public static int[] getLanes() {
        return lanes;
    }
    
}
