/**
 * Entry point used when the game is started from a plain classpath, such as the packaged
 * jar or the container image.
 *
 * <p>
 * The JVM refuses to start a main class that extends {@code Application} unless the JavaFX
 * modules are on the module path. Launching through a class that does not extend it sidesteps
 * that check, which is what lets the game ship as one self-contained jar.
 * </p>
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Starts the game.
     *
     * @param args command line arguments, passed straight through
     */
    public static void main(String[] args) {
        Main.main(args);
    }
}
