
import java.io.File;
import java.io.IOException;

import game.display.models.Taxi;
import game.display.view.GameCanvas;
import game.logic.InputHandler;
import game.utility.DifficultyLoader;
import game.utility.EDifficulty;
import game.utility.ESettings;
import game.utility.TaxiSaver;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Main Class
 */
public class Main extends Application {

    private File save_game = null;
    private Font small_font = new Font("Verdana", 20);
    private Font large_font = new Font("Verdana", 40);
    private GameCanvas gc;
    private EDifficulty difficulty = EDifficulty.EASY;

    /**
     * Main function, launches application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);

    }

    /**
     * This function gets the difficulty selected based on {@code difficulty}
     * @return File of the selected difficulty
     */
    private File getDifficultyFile() {
        File easy_file = null;
        File medium_file = null;
        File hard_file = null;

        // DIFFICULTY FILES
        easy_file = new File("dat\\easy.txt");
        medium_file = new File("dat\\medium.txt");
        hard_file = new File("dat\\hard.txt");
        if (difficulty == EDifficulty.EASY) {
            return easy_file;
        } else if (difficulty == EDifficulty.MEDIUM) {
            return medium_file;
        } else if (difficulty == EDifficulty.HARD) {
            return hard_file;
        }
        return null;
    }

    /**
     * This function returns the cost of the next upgrade for the Engine
     * @return cost of the next upgrade for the Engine
     */
    private double getEngineUpgradeCost() {
        double cost = 0;
        switch (InputHandler.getTaxi().getEngineUpgrade()) {
            case 0:
                cost = 100.0;
                break;
            case 1:
                cost = 200.0;
                break;
            case 2:
                cost = 500.0;
            default:
                break;
        }
        return cost;
    }

    /**
     * This function returns the cost of the next upgrade for Wheels
     * @return the cost of the next upgrade for Wheels
     */
    private double getWheelUpgradeCost() {
        double cost = 0;
        switch (InputHandler.getTaxi().getPotholeResistance()) {
            case 0:
                cost = 100.0;
                break;
            case 1:
                cost = 250.0;
                break;
            case 2:
                cost = 700.0;
            default:
                break;
        }
        return cost;
    }

    /**
     * This function starts the application display
     */
    @Override
    public void start(Stage stg) throws Exception {

        // GROUPS
        Group main_root = new Group();
        Group instruct_root = new Group();
        Group game_root = new Group();
        Group canvas_root = new Group();
        Group difficulty_root = new Group();

        // BUTTONS
        Button start_btn = new Button("New game...");
        Button continue_btn = new Button("Continue game...");
        Button manual_btn = new Button("How to play..");
        Button exit_btn = new Button("Exit..");
        Button difficulty_btn = new Button("Change difficulty..");
        Button back_manual_btn = new Button("Go Back..");
        Button back_game_btn = new Button("Go Back..");
        Button back_diff_btn = new Button("Go Back..");
        Button start_game_btn = new Button("START");
        Button upgrade_engine_btn = new Button("Upgrade Engine");
        Button purchase_NOS_btn = new Button("Purchase NOS");
        Button refresh_stats_btn = new Button("Refresh cash & passenger info");
        Button upgrade_wheels_btn = new Button("Upgrade Wheels");
        Button easy_btn = new Button("Easy");
        Button medium_btn = new Button("Medium");
        Button hard_btn = new Button("Hard");

        VBox main_box = new VBox();
        VBox manual_box = new VBox();
        VBox difficulty_box = new VBox();
        VBox game_box = new VBox();
        VBox canvas_box = new VBox();

        HBox control_box = new HBox();
        HBox engine_box = new HBox();
        HBox wheel_box = new HBox();
        HBox NOS_box = new HBox();

        // BUTTON PROPERTIES
        continue_btn.setDisable(true);

        // GAME CANVAS CREATION
        gc = new GameCanvas(ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());

        // TEXT
        Text instruct = new Text(
                """
                        Welcome to Taxi Racer V0.1
                        In this game, your objective is to evade the police and collect passengers.
                        The police will constantly pursue you.
                        You can also buy upgrades to increase your speed and avoid the police.
                        You recieve money from picking up passengers.
                        You can press the UP and DOWN arrow(s) to move up and down between lanes.
                        You can press the LEFT and RIGHT arrow(s) to move left and right to try and get more passengers.
                        When you are near a passenger, press SPACE to attempt to pick them up.
                        Don't worry if you lose initially, to complete the game you need to aquire every upgrade.


                        To load a game, please use the menu bar on the previous screen to select a save file.
                        If a valid save file has been selected, the continue game button will become activated.
                        Press the 'New Game' button to start a new game.

                        Have fun!

                            """);

        Text selectedFile_Text = new Text("File Selected: ");
        Text titleText = new Text("Taxi Racer V0.1");
        Text gameText = new Text(
                "You can purchase upgrades on this screen.\n Press START to attempt a run.\nPressing the back button will automatically create a save file.\nPress the refresh button to refresh the cash and passenger info.");
        Text engineUpgradeText = new Text(
                "Upgrade your engine to increase your speed.\nPress the button to purchase an upgrade.");
        Text NOSPurchaseText = new Text(
                "Purchase illegal nitrogen (NOS) to rapidly increase your speed. \n(Adds ability to boost speed for a moment. press E to use !!!ONE TIME USE ONLY!!!)\nPress the button to purchase an upgrade.");
        Text WheelUpgradeText = new Text(
                "Upgrade your wheels to increase your ability to not be destroyed by potholes\nPress the button to purchase an upgrade.");
        Text walletInfo = new Text();
        Text passengerInfo = new Text();
        Text difficultyText = new Text("Click the buttons below to change difficulty.\nCurrent Difficulty: Easy");

        // TEXT PROPERTIES
        selectedFile_Text.setFont(small_font);
        selectedFile_Text.setVisible(false);
        titleText.setFont(large_font);
        gameText.setFont(small_font);
        instruct.setX(ESettings.SCENE_WIDTH.getVal() / 2);
        instruct.setFont(small_font);

        // DIFFICULTY SETUP
        // By default we will load easy.
        this.difficulty = EDifficulty.EASY;
        DifficultyLoader.loadDifficulty(getDifficultyFile());

        // Menu (javafx) setup
        MenuBar mb = new MenuBar();
        Menu menu = new Menu("File..");
        mb.getMenus().add(menu);
        MenuItem menuItem = new MenuItem("Open");
        menu.getItems().add(menuItem);

        // File Chooser for opening a save file here
        menuItem.setOnAction(value -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Save File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Save Game Files", "*.sav"));
            try {
                fileChooser.setInitialDirectory(new File(".").getCanonicalFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
            File selectedFile = fileChooser.showOpenDialog(stg);
            if (selectedFile != null) {
                save_game = selectedFile;
                selectedFile_Text.setText("File Selected: " + selectedFile.getName());
                selectedFile_Text.setVisible(true);
                continue_btn.setDisable(false);
                TaxiSaver.setFile(selectedFile);
                Taxi temp = TaxiSaver.getTaxi();
                if (temp == null) {
                    Alert badTaxi = new Alert(Alert.AlertType.ERROR, "Invalid save file");
                    badTaxi.setTitle("Error");
                    badTaxi.showAndWait().ifPresent(rs -> {
                        if (rs == ButtonType.OK) {
                            selectedFile_Text.setVisible(false);
                            continue_btn.setDisable(true);
                        }
                    });

                } else {
                    InputHandler.setTaxi(temp);
                    continue_btn.setDisable(false);
                }
            }
        });

        // Main Menu Setup


        // Scene setup
        Scene instruct_scene = new Scene(instruct_root, ESettings.SCENE_WIDTH.getVal(),
                ESettings.SCENE_HEIGHT.getVal());
        Scene menu_scene = new Scene(main_root, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());

        Scene difficulty_scene = new Scene(difficulty_root, ESettings.SCENE_WIDTH.getVal(),
                ESettings.SCENE_HEIGHT.getVal());
        Scene game_scene = new Scene(game_root, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());

        // Root setup
        game_root.getChildren().addAll(game_box);
        instruct_root.getChildren().add(manual_box);
        main_root.getChildren().add(main_box);
        difficulty_root.getChildren().add(difficulty_box);

        // Game Menu Control Setup:

        // Manual Box Setup
        manual_box.setAlignment(Pos.CENTER);
        manual_box.prefWidthProperty().bind(stg.widthProperty());
        manual_box.setSpacing(25);
        manual_box.getChildren().addAll(instruct, back_manual_btn);

        // Main Menu Box Setup:
        main_box.getChildren().addAll(mb, titleText, selectedFile_Text, start_btn, continue_btn, manual_btn,
                difficulty_btn,
                exit_btn);
        main_box.setAlignment(Pos.CENTER);
        main_box.prefWidthProperty().bind(stg.widthProperty());
        main_box.setSpacing(25);

        // Difficulty Box Setup
        difficulty_box.setAlignment(Pos.CENTER);
        difficulty_box.getChildren().addAll(difficultyText, easy_btn, medium_btn, hard_btn, back_diff_btn);
        difficulty_box.prefWidthProperty().bind(stg.widthProperty());
        difficulty_box.setSpacing(25);

        // Wheel Box Setup
        wheel_box.setAlignment(Pos.CENTER);
        wheel_box.setSpacing(25);
        wheel_box.getChildren().addAll(WheelUpgradeText, upgrade_wheels_btn);

        // Control Box setup
        control_box.setAlignment(Pos.CENTER);
        control_box.setSpacing(25);
        control_box.getChildren().addAll(start_game_btn, refresh_stats_btn, back_game_btn);

        // Engine Box setup
        engine_box.setAlignment(Pos.CENTER);
        engine_box.getChildren().addAll(engineUpgradeText, upgrade_engine_btn);
        engine_box.setSpacing(50);

        // NOS Box setup
        NOS_box.setAlignment(Pos.CENTER);
        NOS_box.getChildren().addAll(NOSPurchaseText, purchase_NOS_btn);
        NOS_box.setSpacing(50);

        // Info to display to the user.
        walletInfo.setFill(Color.GREEN);
        walletInfo.setText("");
        passengerInfo.setFill(Color.DARKRED);
        passengerInfo.setText("");

        // Game Menu control flow.
        game_box.getChildren().addAll(gameText, walletInfo, passengerInfo, engine_box, NOS_box, wheel_box, control_box);
        game_box.prefWidthProperty().bind(stg.widthProperty());
        game_box.setSpacing(25);
        game_box.setAlignment(Pos.CENTER);

        // End Setup

        // Canvas Screen Setup:
        canvas_box.setAlignment(Pos.CENTER);
        canvas_box.getChildren().add(gc);
        canvas_box.prefWidthProperty().bind(stg.widthProperty());
        canvas_root.getChildren().add(canvas_box);
        Scene canvas_scene = new Scene(canvas_root, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
        canvas_scene.setOnKeyPressed(InputHandler::processKeyPress);
        // End Setup

        // Game Menu: Refresh Stats Button
        refresh_stats_btn.setOnAction(value -> {
            if (InputHandler.getTaxi() == null) {
                walletInfo.setText("");
                passengerInfo.setText("");
                return;
            }
            if (InputHandler.getTaxi().hasNOS()) {
                purchase_NOS_btn.setDisable(true);
            } else {
                purchase_NOS_btn.setDisable(false);
            }
            walletInfo.setText(String.format("Cash avaliable: %.2f", InputHandler.getTaxi().getWallet()));
            upgrade_engine_btn
                    .setText(String.format("Upgrade Engine (%d/3)", InputHandler.getTaxi().getEngineUpgrade()));
            upgrade_wheels_btn
                    .setText(String.format("Upgrade Wheels (%d/3)", InputHandler.getTaxi().getPotholeResistance()));
            engineUpgradeText.setText(String.format(
                    "Upgrade your engine to increase your speed.\nPress the button to purchase an upgrade.\nCost: %.2f",
                    getEngineUpgradeCost()));
            WheelUpgradeText.setText(String.format(
                    "Upgrade your wheels to increase your ability to not be destroyed by potholes\nPress the button to purchase an upgrade.\nCost: %.2f",
                    getWheelUpgradeCost()));
            passengerInfo.setText(
                    String.format("Total Passengers picked up: %d", InputHandler.getTaxi().getCareerPassengers()));
            if (InputHandler.getTaxi().getEngineUpgrade() != 3) {
                upgrade_engine_btn.setDisable(false);
            } else {
                upgrade_engine_btn.setDisable(true);
            }
            if (InputHandler.getTaxi().getPotholeResistance() != 3) {
                upgrade_wheels_btn.setDisable(false);
            } else {
                upgrade_wheels_btn.setDisable(true);
            }

        });

        // Game Menu: Upgrade Engine Button
        upgrade_engine_btn.setOnAction(value -> {
            if (InputHandler.getTaxi() == null) {
                return;
            }
            double cost = getEngineUpgradeCost();
            if (InputHandler.getTaxi().getWallet() >= cost) {
                InputHandler.getTaxi().setWallet(InputHandler.getTaxi().getWallet() - cost);
                InputHandler.getTaxi().setEngineUpgrade(InputHandler.getTaxi().getEngineUpgrade() + 1);
            } else {
                upgrade_engine_btn.setText("Insufficient Funds");
            }
            refresh_stats_btn.fire();
        });

        // Game Menu: Upgrade Wheels Button
        upgrade_wheels_btn.setOnAction(value -> {
            if (InputHandler.getTaxi() == null) {
                return;
            }
            double cost = getWheelUpgradeCost();
            if (InputHandler.getTaxi().getWallet() >= cost) {
                InputHandler.getTaxi().setWallet(InputHandler.getTaxi().getWallet() - cost);
                InputHandler.getTaxi().setPotHoleResistance(InputHandler.getTaxi().getPotholeResistance() + 1);
            } else {
                upgrade_wheels_btn.setText("Insufficient Funds");
            }
            refresh_stats_btn.fire();
        });

        // Game Menu: Purchase NOS Button
        purchase_NOS_btn.setOnAction(value -> {
            if (InputHandler.getTaxi() == null) {
                return;
            }
            double cost = 100;
            if (InputHandler.getTaxi().getWallet() >= cost) {
                InputHandler.getTaxi().setWallet(InputHandler.getTaxi().getWallet() - cost);
                InputHandler.getTaxi().setNOS(true);
            } else {
                purchase_NOS_btn.setText("Insufficient Funds");
            }
            refresh_stats_btn.fire();
        });

        // Game Menu: Start Game Button
        start_game_btn.setOnAction(value -> {
            stg.setScene(canvas_scene);
            gc.initCanvas();
            gc.runAnimator();
        });

        // Main Menu: Start Game Button
        start_btn.setOnAction(value -> {
            if (InputHandler.getTaxi() != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "You already have a game started, would you like to start a new game?\nWARNING: This will overwrite your current save.");
                alert.setTitle("New Game");
                ButtonType ok = new ButtonType("Yes");
                ButtonType cancel = new ButtonType("Cancel");
                alert.getButtonTypes().setAll(ok, cancel);
                alert.showAndWait().ifPresent(rs -> {
                    if (rs == ok) {
                        InputHandler.setTaxi(null);
                        DifficultyLoader.loadDifficulty(getDifficultyFile());
                        gc.initCanvas();
                        upgrade_engine_btn.setDisable(true);
                        upgrade_wheels_btn.setDisable(true);
                        purchase_NOS_btn.setDisable(true);
                        walletInfo.setText("");
                        passengerInfo.setText("");
                        stg.setScene(game_scene);
                    } else {
                        return;
                    }
                });
            } else {
                stg.setScene(game_scene);
                DifficultyLoader.loadDifficulty(getDifficultyFile());
                upgrade_engine_btn.setDisable(true);
                upgrade_wheels_btn.setDisable(true);
                purchase_NOS_btn.setDisable(true);
                walletInfo.setText("");
                passengerInfo.setText("");
            }
        });

        // Main Menu: Continue Game Button
        continue_btn.setOnAction(value -> {
            DifficultyLoader.loadDifficulty(getDifficultyFile());
            stg.setScene(game_scene);
        });

        // Difficulty Menu: Back Button
        back_diff_btn.setOnAction(value -> {
            stg.setScene(menu_scene);
        });

        // Difficulty Menu: Easy Button
        easy_btn.setOnAction(value -> {
            difficulty = EDifficulty.EASY;
            difficulty_btn.fire();
        });

        // Difficulty Menu: Medium Button
        medium_btn.setOnAction(value -> {
            difficulty = EDifficulty.MEDIUM;
            difficulty_btn.fire();
        });

        // Difficulty Menu: Hard Button
        hard_btn.setOnAction(value -> {
            difficulty = EDifficulty.HARD;
            difficulty_btn.fire();
        });

        // Game Menu: Back Button
        back_game_btn.setOnAction(value -> {
            // Save the game.
            if (save_game != null) {
                TaxiSaver.setFile(save_game);
                TaxiSaver.save(InputHandler.getTaxi());
                stg.setScene(menu_scene);
            } else {
                if (InputHandler.getTaxi() == null) {
                    stg.setScene(menu_scene);
                    return;
                }
                Alert noSave = new Alert(Alert.AlertType.INFORMATION,
                        "No save file selected.\nWould you like to create a new save file?\nIf no file is selected, the game will not be saved.");
                noSave.setTitle("Save File");
                ButtonType yes = new ButtonType("Yes");
                ButtonType no = new ButtonType("No");
                ButtonType cancel = new ButtonType("Cancel");
                noSave.getButtonTypes().setAll(yes, no, cancel);
                noSave.showAndWait().ifPresent(rs -> {
                    if (rs == yes) {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save Game Files");
                        fileChooser.getExtensionFilters().addAll(
                                new FileChooser.ExtensionFilter("Save Game Files", "*.sav"));
                        try {
                            fileChooser.setInitialDirectory(new File(".").getCanonicalFile());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        File selectedFile = fileChooser.showSaveDialog(stg);
                        if (selectedFile != null) {
                            TaxiSaver.setFile(selectedFile);
                            TaxiSaver.save(InputHandler.getTaxi());
                            save_game = selectedFile;
                            stg.setScene(menu_scene);
                            selectedFile_Text.setVisible(true);
                            selectedFile_Text.setText("File Selected: " + selectedFile.getName());
                            continue_btn.setDisable(false);
                        }

                    } else if (rs == no) {
                        InputHandler.setTaxi(null);
                        stg.setScene(menu_scene);
                        return;
                    } else if (rs == cancel) {
                        return;
                    }
                });
            }
        });

        // Main Menu: Difficulty Button
        difficulty_btn.setOnAction(value -> {
            if (difficulty == EDifficulty.EASY) {
                difficultyText.setText("Click the buttons below to change difficulty.\nCurrent Difficulty: Easy");
                easy_btn.setDisable(true);
                medium_btn.setDisable(false);
                hard_btn.setDisable(false);
            }
            if (difficulty == EDifficulty.MEDIUM) {
                difficultyText.setText("Click the buttons below to change difficulty.\nCurrent Difficulty: Medium");
                medium_btn.setDisable(true);
                easy_btn.setDisable(false);
                hard_btn.setDisable(false);

            }
            if (difficulty == EDifficulty.HARD) {
                difficultyText.setText("Click the buttons below to change difficulty.\nCurrent Difficulty: Hard");
                hard_btn.setDisable(true);
                easy_btn.setDisable(false);
                medium_btn.setDisable(false);
            }
            stg.setScene(difficulty_scene);
        });

        // Main Menu: Exit Button
        exit_btn.setOnAction(value -> {
            System.exit(0);
        });

        // Main Menu: Manual Button
        manual_btn.setOnAction(value -> {
            stg.setScene(instruct_scene);
        });

        // Manual Menu: Back Button
        back_manual_btn.setOnAction(value -> {
            stg.setScene(menu_scene);
        });

        // InputHandler setup.
        InputHandler.setCanvas(gc);
        InputHandler.setMainStage(stg);
        InputHandler.setUpgradeScene(game_scene);

        // Stage setup.
        stg.setScene(menu_scene);
        stg.setTitle("Taxi Racer V0.1");
        stg.show();

    }
}
