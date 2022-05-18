
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.display.GameCanvas;
import model.logic.Settings;
import model.utility.InputHandler;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);

    }

    @Override
    public void start(Stage stg) throws Exception {
        Group root = new Group();
        GameCanvas cv = new GameCanvas(Settings.SCENE_WIDTH,Settings.SCENE_HEIGHT);
        
        root.getChildren().add(cv);
        Scene scene = new Scene(root, Settings.SCENE_WIDTH, Settings.SCENE_HEIGHT);
        scene.setOnKeyPressed(InputHandler::processKeyPress);
        
        stg.setScene(scene);
        cv.runAnimator();
        stg.show();

        


}
}
