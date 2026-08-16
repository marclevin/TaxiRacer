package game.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import game.display.models.Taxi;

/**
 * This class is responsible for saving and loading the game (effectively a taxi instance).
 *
 * <p>
 * Saves are written to a default profile so that progress is kept without the player having
 * to think about files; the file chooser in the menu bar is for moving a profile around by
 * hand. Files written by the original version of the game are still readable.
 * </p>
 */
public final class TaxiSaver {

    /** File signature, "TAXI" in ASCII, used to reject files that are not ours. */
    private static final int MAGIC = 0x54415849;
    /** Current save format version. */
    private static final int VERSION = 3;
    /** First tagged version; it had no body count and no capacity upgrade. */
    private static final int VERSION_WITHOUT_SLIMED = 1;
    /** Second version; it had the body count but no capacity upgrade. */
    private static final int VERSION_WITHOUT_CAPACITY = 2;

    /** Environment variable that relocates the save directory (used by the container). */
    private static final String SAVE_DIR_ENV = "TAXIRACER_SAVE_DIR";
    /** System property equivalent of {@link #SAVE_DIR_ENV}. */
    private static final String SAVE_DIR_PROPERTY = "taxiracer.saveDir";
    /** Directory used when nothing else is configured. */
    private static final String DEFAULT_SAVE_DIR = "saves";
    /** Name of the automatically managed profile. */
    private static final String DEFAULT_SAVE_NAME = "profile.sav";
    /** Where the original version of the game kept its save. */
    private static final String LEGACY_SAVE_NAME = "save.sav";

    private TaxiSaver() {
    }

    /**
     * Returns the file the game saves to automatically.
     *
     * @return the default profile file, whose parent directory may not exist yet.
     */
    public static File defaultSaveFile() {
        String configured = System.getProperty(SAVE_DIR_PROPERTY, System.getenv(SAVE_DIR_ENV));
        File dir = new File(configured == null || configured.isBlank() ? DEFAULT_SAVE_DIR : configured);
        return new File(dir, DEFAULT_SAVE_NAME);
    }

    /**
     * Saves the player's progress to the automatically managed profile.
     *
     * @param taxi The taxi to be saved; {@code null} is ignored.
     * @return {@code true} if the save was written.
     */
    public static boolean autoSave(Taxi taxi) {
        return save(taxi, defaultSaveFile());
    }

    /**
     * Loads the automatically managed profile, importing a legacy save the first time.
     *
     * @return the stored taxi, or {@code null} when there is no previous progress.
     */
    public static Taxi autoLoad() {
        File profile = defaultSaveFile();
        if (profile.isFile()) {
            return load(profile);
        }

        // First run after the upgrade: adopt the save the old version left behind.
        File legacy = new File(LEGACY_SAVE_NAME);
        if (legacy.isFile()) {
            Taxi imported = load(legacy);
            if (imported != null) {
                save(imported, profile);
                return imported;
            }
        }
        return null;
    }

    /**
     * This function saves the provided taxi to a file.
     *
     * @param taxi   The taxi to be saved.
     * @param target The file to write to.
     * @return {@code true} if the save was written.
     */
    public static boolean save(Taxi taxi, File target) {
        if (taxi == null || target == null) {
            return false;
        }

        File parent = target.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            System.err.println("Could not create save directory: " + parent);
            return false;
        }

        try (FileOutputStream fos = new FileOutputStream(target);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                DataOutputStream out = new DataOutputStream(bos)) {

            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeDouble(taxi.getWallet());
            out.writeInt(taxi.getCareerPassengers());
            out.writeInt(taxi.getPotholeResistance());
            out.writeInt(taxi.getEngineUpgrade());
            out.writeBoolean(taxi.hasNOS());
            out.writeInt(taxi.getCareerSlimed());
            out.writeInt(taxi.getCapacityUpgrade());
            return true;
        } catch (IOException e) {
            System.err.println("Could not save to " + target + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * This function loads a taxi from a file.
     *
     * @param source The file to read.
     * @return The loaded taxi, or {@code null} if the file is missing or not a save file.
     */
    public static Taxi load(File source) {
        if (source == null || !source.isFile()) {
            return null;
        }

        Taxi taxi = readCurrentFormat(source);
        if (taxi == null) {
            // Saves written by the original version have no header, so try that layout too.
            taxi = readLegacyFormat(source);
        }
        return taxi;
    }

    /**
     * Reads a save in the current, header-tagged format.
     *
     * @param source the file to read
     * @return the taxi, or {@code null} when the file is not in this format
     */
    private static Taxi readCurrentFormat(File source) {
        try (FileInputStream fis = new FileInputStream(source);
                BufferedInputStream bis = new BufferedInputStream(fis);
                DataInputStream in = new DataInputStream(bis)) {

            if (in.readInt() != MAGIC) {
                return null;
            }
            int version = in.readInt();
            if (version < VERSION_WITHOUT_SLIMED || version > VERSION) {
                return null;
            }

            double wallet = in.readDouble();
            int careerPassengers = in.readInt();
            int potholeResistance = in.readInt();
            int engineUpgrade = in.readInt();
            boolean hasNos = in.readBoolean();
            // Older saves predate these fields, so they simply start at zero.
            int slimed = version > VERSION_WITHOUT_SLIMED ? in.readInt() : 0;
            int capacity = version > VERSION_WITHOUT_CAPACITY ? in.readInt() : 0;

            return buildTaxi(wallet, careerPassengers, potholeResistance, engineUpgrade, hasNos,
                    slimed, capacity);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reads a save written by the original version of the game.
     *
     * @param source the file to read
     * @return the taxi, or {@code null} when the file cannot be read that way either
     */
    private static Taxi readLegacyFormat(File source) {
        try (FileInputStream fis = new FileInputStream(source);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectInputStream in = new ObjectInputStream(bis)) {

            return buildTaxi(in.readDouble(), in.readInt(), in.readInt(), in.readInt(),
                    in.readBoolean(), 0, 0);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Assembles a taxi from stored fields, rejecting values that cannot be genuine.
     *
     * @param wallet            stored wallet balance
     * @param careerPassengers  stored lifetime fare count
     * @param potholeResistance stored wheel upgrade level
     * @param engineUpgrade     stored engine upgrade level
     * @param hasNos            whether a NOS canister is in the boot
     * @param slimed            stored lifetime count of pedestrians run down
     * @param capacityUpgrade   stored seating capacity level
     * @return the restored taxi, or {@code null} if the values are not plausible
     */
    private static Taxi buildTaxi(double wallet, int careerPassengers, int potholeResistance,
            int engineUpgrade, boolean hasNos, int slimed, int capacityUpgrade) {

        if (!Double.isFinite(wallet) || wallet < 0 || careerPassengers < 0) {
            return null;
        }

        Taxi taxi = new Taxi(0, 0);
        // Setters clamp upgrade levels, so a hand-edited file cannot produce a 7/3 engine.
        taxi.setWallet(wallet);
        taxi.setCareerPassengers(careerPassengers);
        taxi.setPotHoleResistance(potholeResistance);
        taxi.setEngineUpgrade(engineUpgrade);
        taxi.setNOS(hasNos);
        taxi.setCareerSlimed(slimed);
        taxi.setCapacityUpgrade(capacityUpgrade);
        return taxi;
    }
}
