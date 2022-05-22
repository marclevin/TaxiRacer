
import java.io.File;
import java.io.IOException;

import game.display.view.GameCanvas;
import game.logic.InputHandler;
import game.utility.ESettings;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Main extends Application {

    private File save_game = null;
    private boolean game_loop = true;
    public static void main(String[] args) {
        launch(args);

    }

    @Override
    public void start(Stage stg) throws Exception {
        Group main_root = new Group();
        Group instruct_root = new Group();
        Group game_root = new Group();
        Button start_btn = new Button("New game...");
        Button continue_btn = new Button("Continue game...");
        continue_btn.setDisable(true);
        Button manual_btn = new Button("How to play..");
        Button exit_btn = new Button("Exit..");
        Button difficulty_btn = new Button("Change difficulty..");
        Button back_manual_btn = new Button("Go Back..");
        GameCanvas cv = new GameCanvas(ESettings.SCENE_WIDTH.getVal(),ESettings.SCENE_HEIGHT.getVal());

        // How to play text going here.

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

        Text fileSelected = new Text("File Selected: ");
        fileSelected.setFont(Font.font("Verdana", 20));
        fileSelected.setVisible(false);

        Text titleText = new Text("Taxi Racer V0.1");
        titleText.setFont(Font.font("Verdana", 40));

        


        MenuBar mb = new MenuBar();
        Menu menu = new Menu("File..");        
        mb.getMenus().add(menu);
        MenuItem menuItem = new MenuItem("Open");
        menu.getItems().add(menuItem);
        menuItem.setOnAction(value -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Save File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Save Game Files", "*.sav"));
            try {
            fileChooser.setInitialDirectory(new File(".").getCanonicalFile());
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            File selectedFile = fileChooser.showOpenDialog(stg);
            if (selectedFile != null) {
                save_game = selectedFile;
                fileSelected.setText("File Selected: " + selectedFile.getName());
                fileSelected.setVisible(true);
               continue_btn.setDisable(false);
            }
        });
        VBox main_box = new VBox();

       

        instruct.setX(ESettings.SCENE_WIDTH.getVal() / 2);
        instruct.setFont(new Font("Verdana",20));
        VBox manual_box = new VBox();
        manual_box.setAlignment(Pos.CENTER);
        manual_box.prefWidthProperty().bind(stg.widthProperty());
        manual_box.setSpacing(25);
        manual_box.getChildren().addAll(instruct,back_manual_btn);


        instruct_root.getChildren().add(manual_box);
        Scene instruct_scene = new Scene(instruct_root, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
        //Button handling
        


        main_box.getChildren().addAll(mb,titleText,fileSelected,start_btn,continue_btn,manual_btn,difficulty_btn,exit_btn);
        main_box.setAlignment(Pos.CENTER);
        main_box.prefWidthProperty().bind(stg.widthProperty());
        main_box.setSpacing(25);

        main_root.getChildren().add(main_box);
        Scene menu_scene = new Scene(main_root, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
        

        stg.setScene(menu_scene);
        stg.setTitle("Taxi Racer V0.1");

        exit_btn.setOnAction(value -> {
            System.exit(0);
        });
        manual_btn.setOnAction(value -> {
            stg.setScene(instruct_scene);
        });

        back_manual_btn.setOnAction(value -> {
            stg.setScene(menu_scene);
        });
        game_root.getChildren().add(cv);
        Scene game_scene = new Scene(game_root, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
        game_scene.setOnKeyPressed(InputHandler::processKeyPress);
        start_btn.setOnAction(value -> {
        InputHandler.setCanvas(cv);
            stg.setScene(game_scene);
            cv.runNewGame();
        });


        stg.show();
        


        


}
}
