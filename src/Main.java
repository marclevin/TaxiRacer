import java.io.File;
import java.io.IOException;
import java.util.Optional;

import game.display.models.Taxi;
import game.display.view.GameCanvas;
import game.logic.InputHandler;
import game.logic.UpgradeShop;
import game.utility.Assets;
import game.utility.DifficultyLoader;
import game.utility.DifficultyProfile;
import game.utility.EDifficulty;
import game.utility.ESettings;
import game.utility.Sounds;
import game.utility.TaxiSaver;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Main Class: builds the menus and wires them to the game canvas.
 */
public class Main extends Application {

    private static final String TITLE = "Taxi Racer";
    private static final double WIDTH = ESettings.SCENE_WIDTH.getVal();
    private static final double HEIGHT = ESettings.SCENE_HEIGHT.getVal();

    private Stage stage;
    private GameCanvas canvas;
    private EDifficulty difficulty = EDifficulty.EASY;

    /** Set when the player opens or saves a profile by hand from the menu bar. */
    private File chosenProfile = null;

    // Scenes
    private Scene menuScene;
    private Scene garageScene;
    private Scene canvasScene;
    private Scene manualScene;
    private Scene difficultyScene;

    // Nodes that are refreshed as the player's state changes
    private final Button continueButton = new Button("Continue game");
    private final Text profileText = new Text();
    private final Text walletText = new Text();
    private final Text faresText = new Text();
    private final Text victoryText = new Text();
    private final Button engineButton = new Button();
    private final Button wheelButton = new Button();
    private final Button nosButton = new Button();
    private final Button capacityButton = new Button();
    private final Text engineText = new Text();
    private final Text wheelText = new Text();
    private final Text nosText = new Text();
    private final Text capacityText = new Text();
    private final Text difficultyText = new Text();
    private final Button easyButton = new Button(EDifficulty.EASY.getLabel());
    private final Button mediumButton = new Button(EDifficulty.MEDIUM.getLabel());
    private final Button hardButton = new Button(EDifficulty.HARD.getLabel());

    /**
     * Main function, launches application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * This function starts the application display.
     *
     * @param stg the primary stage supplied by JavaFX
     */
    @Override
    public void start(Stage stg) {
        this.stage = stg;

        DifficultyLoader.loadDifficulty(difficulty);
        // Picks up whatever is in res/sound; the game stays silent if that is nothing.
        Sounds.load();

        canvas = new GameCanvas(WIDTH, HEIGHT);
        InputHandler.setCanvas(canvas);
        InputHandler.setMainStage(stg);
        // Progress is banked the instant a run ends, so closing the window never costs a fare.
        canvas.setOnRunEnded(this::bankProgress);

        menuScene = buildMenuScene();
        garageScene = buildGarageScene();
        manualScene = buildManualScene();
        difficultyScene = buildDifficultyScene();
        canvasScene = buildCanvasScene();

        InputHandler.setUpgradeScene(garageScene);

        // Pick up where the player left off, importing an original-format save if present.
        Taxi saved = TaxiSaver.autoLoad();
        if (saved != null) {
            InputHandler.setTaxi(saved);
        }
        refreshMenu();

        stg.setScene(menuScene);
        stg.setTitle(TITLE);
        stg.setResizable(false);
        stg.setOnCloseRequest(event -> bankProgress());
        stg.show();
    }

    // ------------------------------------------------------------------
    // Scenes
    // ------------------------------------------------------------------

    /**
     * Builds the main menu.
     *
     * @return the main menu scene.
     */
    private Scene buildMenuScene() {
        Text title = styled(new Text(TITLE), "title");
        Text tagline = styled(new Text("Collect fares. Dodge potholes. Outrun the law."), "muted-text");

        Button newGame = new Button("New game");
        newGame.getStyleClass().add("primary-button");
        newGame.setOnAction(e -> startNewGame());

        continueButton.setOnAction(e -> openGarage());

        Button manual = new Button("How to play");
        manual.setOnAction(e -> stage.setScene(manualScene));

        Button difficultyBtn = new Button("Change difficulty");
        difficultyBtn.setOnAction(e -> openDifficulty());

        Button exit = new Button("Exit");
        exit.setOnAction(e -> quit());

        styled(profileText, "muted-text");

        VBox column = column(24, title, tagline, profileText, newGame, continueButton, manual, difficultyBtn, exit);
        column.setPadding(new Insets(40));

        BorderPane layout = new BorderPane();
        layout.setTop(buildMenuBar());
        layout.setCenter(column);

        return scene(layout);
    }

    /**
     * Builds the menu bar used to import and export profiles by hand.
     *
     * @return the configured menu bar.
     */
    private MenuBar buildMenuBar() {
        MenuItem open = new MenuItem("Open profile...");
        open.setOnAction(e -> openProfile());

        MenuItem saveAs = new MenuItem("Save profile as...");
        saveAs.setOnAction(e -> saveProfileAs());

        Menu file = new Menu("File");
        file.getItems().addAll(open, saveAs);

        MenuBar bar = new MenuBar();
        bar.getMenus().add(file);
        return bar;
    }

    /**
     * Builds the garage, where fares are spent on upgrades.
     *
     * @return the garage scene.
     */
    private Scene buildGarageScene() {
        Text heading = styled(new Text("The Garage"), "heading");
        Text blurb = styled(new Text(
                "Spend your fares here, then press START to take the taxi out.\n"
                        + "Going back to the menu saves your progress automatically."),
                "body-text");
        blurb.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        styled(walletText, "money-text");
        styled(faresText, "fares-text");
        styled(victoryText, "victory-text");
        victoryText.setVisible(false);
        victoryText.setManaged(false);

        engineButton.getStyleClass().add("buy-button");
        engineButton.setOnAction(e -> buy(UpgradeShop::buyEngine));
        wheelButton.getStyleClass().add("buy-button");
        wheelButton.setOnAction(e -> buy(UpgradeShop::buyWheels));
        nosButton.getStyleClass().add("buy-button");
        nosButton.setOnAction(e -> buy(UpgradeShop::buyNos));
        capacityButton.getStyleClass().add("buy-button");
        capacityButton.setOnAction(e -> buy(UpgradeShop::buyCapacity));

        // Two by two: four upgrades stacked in a column would not fit the window.
        GridPane upgrades = new GridPane();
        upgrades.setHgap(16);
        upgrades.setVgap(14);
        upgrades.setAlignment(Pos.CENTER);
        upgrades.add(upgradeCard(capacityText, capacityButton), 0, 0);
        upgrades.add(upgradeCard(engineText, engineButton), 1, 0);
        upgrades.add(upgradeCard(wheelText, wheelButton), 0, 1);
        upgrades.add(upgradeCard(nosText, nosButton), 1, 1);

        Button start = new Button("START RUN");
        start.getStyleClass().add("primary-button");
        start.setOnAction(e -> startRun());

        Button back = new Button("Back to menu");
        back.setOnAction(e -> leaveGarage());

        HBox controls = new HBox(20, start, back);
        controls.setAlignment(Pos.CENTER);

        VBox column = column(16, heading, blurb, walletText, faresText, victoryText, upgrades, controls);
        column.setPadding(new Insets(28, 40, 28, 40));

        return scene(column);
    }

    /**
     * Builds one upgrade row: a description on the left, the purchase button on the right.
     *
     * @param description the upgrade blurb, refreshed with its live price
     * @param button      the purchase button
     * @return the assembled row.
     */
    private VBox upgradeCard(Text description, Button button) {
        styled(description, "body-text");

        TextFlow flow = new TextFlow(description);
        flow.setMaxWidth(400);
        flow.setPrefHeight(78);

        VBox card = new VBox(10, flow, button);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(440);
        card.getStyleClass().add("card");
        return card;
    }

    /**
     * Builds the instructions screen.
     *
     * @return the manual scene.
     */
    private Scene buildManualScene() {
        Text heading = styled(new Text("How to play"), "heading");
        Text body = styled(new Text("""
                You drive a taxi. The police are behind you and they are patient.

                The taxi holds the middle of the screen and the city comes to you. All you
                choose is which of the four lanes to be in.

                PICK UP.  Fares wait on the two pavements, so you have to be in an outside
                lane to reach them. Press SPACE alongside one and they climb in through the
                windows, where you can see how full you are.

                WHERE THEY ARE GOING.  Every fare tells you how far they want to go as they
                get in, and the lamp under their seat follows the trip: blue at the start,
                amber as they get close, green once you are at their stop, red once you have
                driven them well past it. The gauge in the corner says the same thing, one
                pip per seat.

                DROP OFF.  A fare does not pay until you drop them off, and what they pay
                depends on where. On the green they pay well over the meter; let them out
                too soon, or miles too late, and they pay a fraction of it. Once the taxi is
                full, press SPACE to pull over, then mash SPACE to let them out one by one.
                Whoever is nearest their stop always goes first, so the money is best in the
                first seconds of a stop. You are stationary the whole time and the police
                close in fast. Change lane to pull away early.

                Do not want to wait until you are full? Press D to pull over on a half load
                and bank what you are carrying - ideally the moment the lights go green.

                The money in the back is not yours until it is out of the taxi. Get caught
                carrying it and you lose the lot. A bigger taxi turns several stops into one.

                Watch for people crossing the road. Hit one and you wear most of them on
                your bonnet for a few seconds, the police close a large part of the gap
                instantly, and they drive furious for a while afterwards. The game keeps
                count.

                THE CHASE.  Once the police are close enough to see, they lean into whatever
                lane you take, surge forward and drop back rather than closing steadily, and
                the officer in the back window leans out with a pistol. A reticle shows the
                lane the shot is settling on, and it locks on for the last part of the aim -
                change lane late and the round goes wide. Take one in a tyre and you limp,
                which is the moment they are waiting for.

                Finish the game by buying every upgrade.

                CONTROLS
                  UP / DOWN        change lane
                  SPACE            pick up a fare; when full, drop them off
                  D                pull over early to bank a half load
                  E                fire NOS (one canister per purchase)
                  P                pause
                  R                restart the run
                  ESC / ENTER      return to the garage

                Your progress saves itself. Use File > Open profile to load a save file
                from somewhere else, or File > Save profile as to keep a copy.
                """), "body-text");

        Button back = new Button("Go back");
        back.setOnAction(e -> stage.setScene(menuScene));

        // The window is a fixed 720px and the instructions are longer than that, so they
        // scroll rather than running off the bottom edge where nobody would ever find them.
        ScrollPane scroller = new ScrollPane(body);
        scroller.setFitToWidth(true);
        scroller.setPrefViewportHeight(HEIGHT - 190);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.getStyleClass().add("manual-scroll");

        VBox column = column(18, heading, scroller, back);
        column.setPadding(new Insets(24, 60, 24, 60));
        return scene(column);
    }

    /**
     * Builds the difficulty picker.
     *
     * @return the difficulty scene.
     */
    private Scene buildDifficultyScene() {
        Text heading = styled(new Text("Difficulty"), "heading");
        styled(difficultyText, "body-text");

        easyButton.setOnAction(e -> setDifficulty(EDifficulty.EASY));
        mediumButton.setOnAction(e -> setDifficulty(EDifficulty.MEDIUM));
        hardButton.setOnAction(e -> setDifficulty(EDifficulty.HARD));

        HBox choices = new HBox(20, easyButton, mediumButton, hardButton);
        choices.setAlignment(Pos.CENTER);

        Button back = new Button("Go back");
        back.setOnAction(e -> stage.setScene(menuScene));

        VBox column = column(24, heading, difficultyText, choices, back);
        column.setPadding(new Insets(40));
        return scene(column);
    }

    /**
     * Builds the scene that hosts the game canvas.
     *
     * @return the gameplay scene.
     */
    private Scene buildCanvasScene() {
        StackPane holder = new StackPane(canvas);
        holder.setPrefSize(WIDTH, HEIGHT);

        Group root = new Group(holder);
        Scene gameScene = new Scene(root, WIDTH, HEIGHT);
        applyStylesheet(gameScene);
        gameScene.setOnKeyPressed(InputHandler::processKeyPress);
        return gameScene;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    /**
     * Starts a brand new career, confirming first if one is already under way.
     */
    private void startNewGame() {
        if (InputHandler.getTaxi() != null) {
            ButtonType yes = new ButtonType("Start over");
            ButtonType cancel = new ButtonType("Cancel");
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "You already have a career in progress.\n"
                            + "Starting a new game will overwrite your saved profile.",
                    yes, cancel);
            alert.setTitle("New game");
            alert.setHeaderText("Start a new game?");

            Optional<ButtonType> answer = alert.showAndWait();
            if (answer.isEmpty() || answer.get() != yes) {
                return;
            }
        }

        InputHandler.setTaxi(null);
        DifficultyLoader.loadDifficulty(difficulty);
        canvas.initCanvas();
        bankProgress();
        openGarage();
    }

    /**
     * Shows the garage, refreshing the prices and the wallet first.
     */
    private void openGarage() {
        DifficultyLoader.loadDifficulty(difficulty);
        if (InputHandler.getTaxi() == null) {
            canvas.initCanvas();
        }
        refreshGarage();
        stage.setScene(garageScene);
    }

    /**
     * Leaves the garage for the main menu, saving on the way out.
     */
    private void leaveGarage() {
        bankProgress();
        refreshMenu();
        stage.setScene(menuScene);
    }

    /**
     * Takes the taxi out for a run.
     */
    private void startRun() {
        bankProgress();
        stage.setScene(canvasScene);
        canvas.initCanvas();
        canvas.runAnimator();
        canvas.requestFocus();
    }

    /**
     * Applies a purchase and refreshes the garage.
     *
     * @param purchase the shop operation to attempt
     */
    private void buy(java.util.function.Predicate<Taxi> purchase) {
        Taxi taxi = InputHandler.getTaxi();
        if (taxi == null) {
            return;
        }
        purchase.test(taxi);
        bankProgress();
        refreshGarage();
    }

    /**
     * Changes the difficulty and reloads its pothole layout.
     *
     * @param selected the difficulty chosen by the player
     */
    private void setDifficulty(EDifficulty selected) {
        this.difficulty = selected;
        DifficultyLoader.loadDifficulty(selected);
        refreshDifficulty();
    }

    /**
     * Shows the difficulty picker.
     */
    private void openDifficulty() {
        refreshDifficulty();
        stage.setScene(difficultyScene);
    }

    /**
     * Writes the player's progress to the automatic profile, and to a hand-picked file if
     * one has been chosen.
     */
    private void bankProgress() {
        Taxi taxi = InputHandler.getTaxi();
        if (taxi == null) {
            return;
        }
        TaxiSaver.autoSave(taxi);
        if (chosenProfile != null) {
            TaxiSaver.save(taxi, chosenProfile);
        }
    }

    /**
     * Saves and exits.
     */
    private void quit() {
        bankProgress();
        Platform.exit();
    }

    /**
     * Loads a profile chosen by the player.
     */
    private void openProfile() {
        File selected = chooser("Open profile").showOpenDialog(stage);
        if (selected == null) {
            return;
        }

        Taxi loaded = TaxiSaver.load(selected);
        if (loaded == null) {
            Alert bad = new Alert(Alert.AlertType.ERROR,
                    selected.getName() + " is not a Taxi Racer save file.");
            bad.setTitle("Could not open profile");
            bad.setHeaderText("Invalid save file");
            bad.showAndWait();
            return;
        }

        chosenProfile = selected;
        InputHandler.setTaxi(loaded);
        bankProgress();
        refreshMenu();
    }

    /**
     * Exports the current profile to a file chosen by the player.
     */
    private void saveProfileAs() {
        Taxi taxi = InputHandler.getTaxi();
        if (taxi == null) {
            Alert none = new Alert(Alert.AlertType.INFORMATION,
                    "Start a game before saving a profile.");
            none.setTitle("Nothing to save");
            none.setHeaderText(null);
            none.showAndWait();
            return;
        }

        File selected = chooser("Save profile as").showSaveDialog(stage);
        if (selected == null) {
            return;
        }
        chosenProfile = selected;
        TaxiSaver.save(taxi, selected);
        refreshMenu();
    }

    /**
     * Builds a file chooser pointed at the save directory.
     *
     * @param title the dialog title
     * @return a configured file chooser.
     */
    private FileChooser chooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Save Game Files", "*.sav"));

        File start = TaxiSaver.defaultSaveFile().getAbsoluteFile().getParentFile();
        if (start != null && start.isDirectory()) {
            fileChooser.setInitialDirectory(start);
        } else {
            try {
                fileChooser.setInitialDirectory(new File(".").getCanonicalFile());
            } catch (IOException ignored) {
                // A missing start directory just means the chooser opens wherever it likes.
            }
        }
        return fileChooser;
    }

    // ------------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------------

    /**
     * Updates the main menu to match the player's saved state.
     */
    private void refreshMenu() {
        Taxi taxi = InputHandler.getTaxi();
        continueButton.setDisable(taxi == null);

        if (taxi == null) {
            profileText.setText("No career started yet.");
            return;
        }
        String location = chosenProfile != null
                ? chosenProfile.getName()
                : TaxiSaver.defaultSaveFile().getPath();
        profileText.setText(String.format("Career: R%.2f banked, %d fares driven  (%s)",
                taxi.getWallet(), taxi.getCareerPassengers(), location));
    }

    /**
     * Updates every price, label and button in the garage.
     *
     * <p>
     * This runs on entry and after each purchase; the original required the player to press
     * a refresh button to see their own wallet.
     * </p>
     */
    private void refreshGarage() {
        Taxi taxi = InputHandler.getTaxi();
        if (taxi == null) {
            return;
        }

        walletText.setText(String.format("Wallet: R%.2f", taxi.getWallet()));
        faresText.setText(String.format("Fares driven: %d          Pedestrians slimed: %d",
                taxi.getCareerPassengers(), taxi.getCareerSlimed()));

        refreshCapacity(taxi);

        refreshUpgrade(engineText, engineButton,
                "Engine: get back to speed faster after a stop.",
                taxi.getEngineUpgrade(), UpgradeShop.engineCost(taxi), taxi.getWallet(), "Engine");

        refreshUpgrade(wheelText, wheelButton,
                "Wheels: shrug off potholes instead of slowing.",
                taxi.getPotholeResistance(), UpgradeShop.wheelCost(taxi), taxi.getWallet(), "Wheels");

        refreshNos(taxi);

        boolean complete = taxi.isFullyUpgraded();
        victoryText.setText("Every upgrade bought - the streets are yours. "
                + "Now go and land 100 fares in a single run.");
        victoryText.setVisible(complete);
        victoryText.setManaged(complete);
    }

    /**
     * Updates one upgrade row.
     *
     * @param label       the description text node
     * @param button      the purchase button
     * @param blurb       what the upgrade does
     * @param level       how many levels are already owned
     * @param cost        the price of the next level, or {@link UpgradeShop#UNAVAILABLE}
     * @param wallet      the player's balance
     * @param buttonLabel the noun used on the button
     */
    private void refreshUpgrade(Text label, Button button, String blurb, int level, double cost,
            double wallet, String buttonLabel) {

        if (cost == UpgradeShop.UNAVAILABLE) {
            label.setText(blurb + "\nFully upgraded.");
            button.setText(buttonLabel + " " + level + "/" + Taxi.MAX_UPGRADE);
            button.setDisable(true);
            return;
        }

        label.setText(String.format("%s%nNext level costs R%.2f", blurb, cost));
        button.setText(String.format("Upgrade %s (%d/%d)", buttonLabel.toLowerCase(), level,
                Taxi.MAX_UPGRADE));
        button.setDisable(wallet < cost);
    }

    /**
     * Updates the capacity row, which shows the seat count rather than a bare level.
     *
     * @param taxi the player's taxi
     */
    private void refreshCapacity(Taxi taxi) {
        int level = taxi.getCapacityUpgrade();
        int seats = taxi.getCapacity();
        double cost = UpgradeShop.capacityCost(taxi);

        // Lines are kept short by hand: the card is 400px wide and re-wrapping mid-sentence
        // reads as a typo.
        String blurb = String.format(
                "Seats: %d fares per load.%nFares only pay once you drop them off.", seats);

        if (cost == UpgradeShop.UNAVAILABLE) {
            capacityText.setText(blurb + "\nFull size - no more seats to fit.");
            capacityButton.setText(String.format("Seats %d (%d/%d)", seats, level, Taxi.MAX_UPGRADE));
            capacityButton.setDisable(true);
            return;
        }

        capacityText.setText(String.format("%s%nNext row of seats takes you to %d, for R%.2f",
                blurb, Taxi.capacityAtLevel(level + 1), cost));
        capacityButton.setText(String.format("Add seats (%d/%d)", level, Taxi.MAX_UPGRADE));
        capacityButton.setDisable(taxi.getWallet() < cost);
    }

    /**
     * Updates the NOS row, which behaves differently because the canister is consumable.
     *
     * @param taxi the player's taxi
     */
    private void refreshNos(Taxi taxi) {
        String blurb = "NOS: one burst of speed that loses the police.\n"
                + "Press E during a run to use it.";

        if (taxi.hasNOS()) {
            nosText.setText(blurb + "\nA canister is loaded and ready.");
            nosButton.setText("NOS loaded");
            nosButton.setDisable(true);
            return;
        }

        nosText.setText(String.format("%s%nA canister costs R%.2f", blurb, UpgradeShop.NOS_COST));
        nosButton.setText("Buy NOS");
        nosButton.setDisable(taxi.getWallet() < UpgradeShop.NOS_COST);
    }

    /**
     * Updates the difficulty screen to match the current selection.
     */
    private void refreshDifficulty() {
        DifficultyProfile p = DifficultyLoader.getProfile();
        int potholes = 0;
        for (int lane : p.getPotholes()) {
            potholes += lane;
        }

        // The chase drift is the number that actually decides how a difficulty feels, so it
        // is spelled out rather than left for the player to discover the hard way.
        String chase = p.getCleanDrift() < 0
                ? "Clean driving pulls you away from the police."
                : "The police gain on you even when you drive perfectly.";

        difficultyText.setText(String.format(
                "%s%n%n"
                        + "Current difficulty: %s%n"
                        + "  Potholes on the road:      %d%n"
                        + "  Police start:              %.0fm behind%n"
                        + "  Ground lost per pothole:   %.1fm per step%n"
                        + "  People crossing the road:  %.0f%% of spawns%n"
                        + "  Cost of running one down:  %.0fm handed to the police",
                chase, difficulty.getLabel(), potholes, p.getPoliceStart(), p.getSlowGain(),
                p.getJaywalkerChance() * 100, p.getSplatPenalty()));

        easyButton.setDisable(difficulty == EDifficulty.EASY);
        mediumButton.setDisable(difficulty == EDifficulty.MEDIUM);
        hardButton.setDisable(difficulty == EDifficulty.HARD);
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    /**
     * Adds a style class to a text node and returns it, so nodes can be styled inline.
     *
     * @param text       the node to style
     * @param styleClass the CSS class to add
     * @return the same node.
     */
    private static Text styled(Text text, String styleClass) {
        text.getStyleClass().add(styleClass);
        return text;
    }

    /**
     * Builds a centred, evenly spaced vertical column.
     *
     * @param spacing gap between children, in pixels
     * @param nodes   the children, top to bottom
     * @return the assembled column.
     */
    private static VBox column(double spacing, javafx.scene.Node... nodes) {
        VBox box = new VBox(spacing, nodes);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(WIDTH, HEIGHT);
        return box;
    }

    /**
     * Wraps a layout root in a stylesheet-carrying scene.
     *
     * @param root the scene root
     * @return the assembled scene.
     */
    private static Scene scene(javafx.scene.Parent root) {
        Scene built = new Scene(root, WIDTH, HEIGHT);
        applyStylesheet(built);
        return built;
    }

    /**
     * Attaches the game stylesheet, if it can be found.
     *
     * @param target the scene to style
     */
    private static void applyStylesheet(Scene target) {
        String css = Assets.url("res/style.css");
        if (css != null) {
            target.getStylesheets().add(css);
        }
    }
}
