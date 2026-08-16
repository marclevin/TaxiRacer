package game.utility;

/**
 * This enum is an enum of difficulties.
 *
 * <p>
 * Each difficulty names the data file holding its pothole layout, so adding a difficulty is
 * a matter of dropping a file in {@code dat/} and adding a constant here.
 * </p>
 */
public enum EDifficulty {

    /** A quiet road: one pothole per lane. */
    EASY("Easy", "dat/easy.txt"),
    /** A busier road. */
    MEDIUM("Medium", "dat/medium.txt"),
    /** A minefield. */
    HARD("Hard", "dat/hard.txt");

    private final String label;
    private final String resource;

    /**
     * Constructor for the difficulty enum.
     *
     * @param label    the name shown to the player
     * @param resource the asset path of the pothole layout
     */
    EDifficulty(String label, String resource) {
        this.label = label;
        this.resource = resource;
    }

    /**
     * Returns the name shown to the player.
     *
     * @return the display label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the asset path of this difficulty's pothole layout.
     *
     * @return a slash-separated asset path.
     */
    public String getResource() {
        return resource;
    }
}
