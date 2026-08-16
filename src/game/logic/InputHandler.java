package game.logic;

import game.display.models.Police;
import game.display.models.Taxi;
import game.display.view.GameCanvas;
import game.utility.ETaxiPositions;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * This class is responsible for handling the input from the user.
 *
 * <p>
 * It also owns the small amount of state that describes "the run in progress" — which lane
 * the player is in, whether the run is paused, and whether it has ended. All of that is
 * cleared by {@link #resetForNewRun()}, which is what stops one attempt from bleeding into
 * the next.
 * </p>
 */
public final class InputHandler {

    /** The lane the taxi starts every run in. */
    public static final int DEFAULT_LANE = 2;

    private static Taxi taxi = null;
    private static Stage mainStage = null;
    private static Scene upgradeScene = null;
    private static Police police = null;
    private static GameCanvas gameCanvas = null;

    /**
     * Most unhandled SPACE presses kept in hand.
     *
     * <p>
     * Presses are counted rather than flagged so that mashing during a drop-off registers
     * every tap, even when several land inside one 1/60s step. The buffer is small so a
     * held key cannot bank up a huge queue and empty the taxi in one go.
     * </p>
     */
    private static final int MAX_BUFFERED_PRESSES = 3;

    private static boolean runActive = true;
    private static boolean paused = false;
    private static int spacePresses = 0;
    private static boolean steeredThisStep = false;
    private static boolean dropRequested = false;
    private static int lane = DEFAULT_LANE;

    private InputHandler() {
    }

    /**
     * Clears all per-run state, putting the taxi back in its starting lane.
     *
     * <p>
     * The lane index in particular has to be reset here. It used to survive a run, so after
     * being caught in the outside lane the taxi was redrawn in the middle lane while this
     * counter still said "outside" — and the first press of UP appeared to do nothing.
     * </p>
     */
    public static void resetForNewRun() {
        lane = DEFAULT_LANE;
        runActive = true;
        paused = false;
        spacePresses = 0;
        steeredThisStep = false;
        dropRequested = false;
    }

    /**
     * Returns the lane index the taxi currently occupies.
     *
     * @return an index into {@link ETaxiPositions}.
     */
    public static int getLane() {
        return lane;
    }

    /**
     * Discards every pending SPACE press.
     */
    public static void pickupBlock() {
        spacePresses = 0;
    }

    /**
     * This method returns true if a pickup has been attempted.
     *
     * @return true if there is an unhandled SPACE press waiting.
     */
    public static boolean pickupAttempted() {
        return spacePresses > 0;
    }

    /**
     * Takes one pending SPACE press, if there is one.
     *
     * @return {@code true} if a press was claimed.
     */
    public static boolean consumeSpacePress() {
        if (spacePresses <= 0) {
            return false;
        }
        spacePresses--;
        return true;
    }

    /**
     * Takes the "the player changed lane" flag, if it is set.
     *
     * <p>
     * Used to break off a drop-off: pulling out of the lane means the driver has decided
     * to leave with whoever is still aboard.
     * </p>
     *
     * @return {@code true} if the player changed lane since this was last called.
     */
    public static boolean consumeSteering() {
        boolean steered = steeredThisStep;
        steeredThisStep = false;
        return steered;
    }

    /**
     * Takes the "pull over early" request, if one is pending.
     *
     * <p>
     * A full taxi can be emptied with SPACE, but banking a half load is a deliberate
     * decision and gets its own key. Starting a stop on any stray SPACE press would mean
     * standing still by accident, which is the most expensive mistake in the game.
     * </p>
     *
     * @return {@code true} if the player asked to pull over since this was last called.
     */
    public static boolean consumeDropRequest() {
        boolean requested = dropRequested;
        dropRequested = false;
        return requested;
    }

    /**
     * This method sets the main stage for the handler.
     *
     * @param stage The stage to set.
     */
    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    /**
     * This method sets the upgrade screen for the handler.
     *
     * @param scene the scene to set.
     */
    public static void setUpgradeScene(Scene scene) {
        upgradeScene = scene;
    }

    /**
     * This method sets the taxi for the handler.
     *
     * @param s The taxi to be set.
     */
    public static void setTaxi(Taxi s) {
        taxi = s;
    }

    /**
     * This returns the handler's taxi instance.
     *
     * @return the instance of Taxi
     */
    public static Taxi getTaxi() {
        return taxi;
    }

    /**
     * This method sets the canvas for the handler.
     *
     * @param g the canvas to be set.
     */
    public static void setCanvas(GameCanvas g) {
        gameCanvas = g;
    }

    /**
     * This sets the handler's police instance.
     *
     * @param p the police instance
     */
    public static void setPolice(Police p) {
        police = p;
    }

    /**
     * This gets the handler's police instance.
     *
     * @return the police instance
     */
    public static Police getPolice() {
        return police;
    }

    /**
     * Marks the run as over, so driving input stops being accepted.
     */
    public static void endGame() {
        runActive = false;
        paused = false;
    }

    /**
     * Marks a run as under way, so driving input is accepted again.
     */
    public static void begunGame() {
        runActive = true;
        paused = false;
    }

    /**
     * Reports whether the player has paused the run.
     *
     * @return true while the run is paused.
     */
    public static boolean isPaused() {
        return paused;
    }

    /**
     * Reports whether a run is still in progress.
     *
     * @return true until the run is won or lost.
     */
    public static boolean isRunActive() {
        return runActive;
    }

    /**
     * This method processes {@code KeyEvent} events from its assigned parent.
     *
     * @param event The event to process.
     */
    public static void processKeyPress(KeyEvent event) {
        if (taxi == null || police == null) {
            return;
        }

        switch (event.getCode()) {
            case UP:
                changeLane(-1);
                break;

            case DOWN:
                changeLane(1);
                break;

            case SPACE:
                if (canDrive()) {
                    spacePresses = Math.min(MAX_BUFFERED_PRESSES, spacePresses + 1);
                }
                break;

            case E:
                if (canDrive() && taxi.activateNos()) {
                    game.utility.Sounds.play(game.utility.Sounds.Sfx.NOS);
                }
                break;

            case M:
                game.utility.Sounds.setMuted(!game.utility.Sounds.isMuted());
                break;

            case D:
                if (canDrive()) {
                    dropRequested = true;
                }
                break;

            case P:
                if (runActive) {
                    paused = !paused;
                }
                break;

            case R:
                // A quick restart saves a trip through the garage after a bad run.
                if (gameCanvas != null) {
                    gameCanvas.restartRun();
                }
                break;

            case ENTER:
            case ESCAPE:
                returnToGarage();
                break;

            default:
                break;
        }
    }

    /**
     * Moves the taxi one lane, keeping the pursuing police car in the same lane.
     *
     * @param delta {@code -1} to move towards the far pavement, {@code +1} towards the near one.
     */
    private static void changeLane(int delta) {
        if (!canDrive()) {
            return;
        }
        int target = lane + delta;
        if (target < 0 || target >= ETaxiPositions.values().length) {
            return;
        }
        lane = target;
        steeredThisStep = true;
        taxi.scale(ETaxiPositions.values()[lane]);
    }

    /**
     * Reports whether driving input should be acted on right now.
     *
     * @return true only while a run is live and unpaused.
     */
    private static boolean canDrive() {
        return runActive && !paused;
    }

    /**
     * Leaves the run and goes back to the garage, banking whatever was earned.
     */
    private static void returnToGarage() {
        if (gameCanvas == null || mainStage == null || upgradeScene == null) {
            return;
        }
        gameCanvas.stopAnimator();
        mainStage.setScene(upgradeScene);
    }
}
