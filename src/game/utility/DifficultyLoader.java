package game.utility;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public final class DifficultyLoader {
    private static int[] lanes;
    private DifficultyLoader() {
        
    }

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

    public static int[] getLanes() {
        return lanes;
    }
    
}
