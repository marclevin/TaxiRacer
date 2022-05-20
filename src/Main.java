
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.display.GameCanvas;
import model.logic.InputHandler;
import model.utility.ESettings;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);

    }

    @Override
    public void start(Stage stg) throws Exception {
        Group root = new Group();
        GameCanvas cv = new GameCanvas(ESettings.SCENE_WIDTH.getVal(),ESettings.SCENE_HEIGHT.getVal());
        
        root.getChildren().add(cv);
        Scene scene = new Scene(root, ESettings.SCENE_WIDTH.getVal(), ESettings.SCENE_HEIGHT.getVal());
        scene.setOnKeyPressed(InputHandler::processKeyPress);
        
        stg.setScene(scene);
        cv.runAnimator();
        stg.show();

        


}
}
